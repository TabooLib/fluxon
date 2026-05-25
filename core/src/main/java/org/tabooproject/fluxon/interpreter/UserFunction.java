package org.tabooproject.fluxon.interpreter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.FunctionSignature;
import org.tabooproject.fluxon.runtime.Symbolic;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户自定义的函数
 * 解释执行环境中的函数定义
 */
public class UserFunction implements Function, Symbolic {

    private final SymbolFunction symbolInfo;
    private FunctionSignature signature;
    private final boolean envFree;
    private final int paramCount;
    private final int localVarCount;

    @NotNull
    private final FunctionDefinition definition;
    @NotNull
    private final Interpreter interpreter;

    public UserFunction(@NotNull FunctionDefinition definition, @NotNull Interpreter interpreter) {
        this.symbolInfo = new SymbolFunction(null, definition.getName(), definition.getParameters().size());
        this.definition = definition;
        this.interpreter = interpreter;
        this.signature = buildSignature(definition);
        this.envFree = !(definition instanceof LambdaFunctionDefinition)
                && !definition.hasVariablesCapturedByChildren();
        this.paramCount = definition.getParameters().size();
        this.localVarCount = definition.getLocalVariables().size();
    }

    /**
     * 根据 FunctionDefinition 的参数类型注解构建签名
     * 无类型注解的参数视为 OBJECT
     */
    @Nullable
    private static FunctionSignature buildSignature(@NotNull FunctionDefinition definition) {
        Map<Integer, Class<?>> parameterTypes = definition.getParameterTypes();
        if (parameterTypes.isEmpty()) return null;
        LinkedHashMap<String, Integer> parameters = definition.getParameters();
        Type[] types = new Type[parameters.size()];
        int i = 0;
        for (Integer slot : parameters.values()) {
            Class<?> clazz = parameterTypes.get(i);
            types[i] = clazz != null ? Type.fromClass(clazz) : Type.OBJECT;
            i++;
        }
        return FunctionSignature.returns(Type.OBJECT).params(types);
    }

    @NotNull
    @Override
    public String getName() {
        return definition.getName();
    }

    @Nullable
    @Override
    public String getNamespace() {
        return null;
    }

    @Nullable
    @Override
    public FunctionSignature getSignature() {
        return signature;
    }

    public void setSignature(FunctionSignature signature) {
        this.signature = signature;
    }

    @Override
    public boolean isAsync() {
        return definition.isAsync();
    }

    @Override
    public boolean isPrimarySync() {
        return definition.isPrimarySync();
    }

    @Override
    public void call(@NotNull final FunctionContext<?> context) {
        // 优先使用调用链传递的 interpreter（async 场景下为 child），回退到定义时的 interpreter
        Interpreter exec = context.getInterpreter();
        if (exec == null) exec = this.interpreter;
        if (envFree) {
            callEnvFree(context, exec);
            return;
        }
        // Lambda 的父环境来自定义时捕获，普通函数仍使用当前解释器环境。
        Environment parentEnv = definition instanceof LambdaFunctionDefinition ? context.getEnvironment() : exec.getEnvironment();
        Environment functionEnv = Intrinsics.bindFunctionParameters(
                parentEnv,
                definition.getParameters(),
                context,
                localVarCount
        );
        if (definition instanceof LambdaFunctionDefinition) {
            functionEnv.setCaptureOffset(((LambdaFunctionDefinition) definition).getCaptureOffset());
        }
        if (definition.getBody().getType() != null && definition.getBody().getType() != ParseResult.ResultType.STATEMENT) {
            exec.consumeCostStep();
        }
        Type t = exec.executeWithEnvironment(definition.getBody(), functionEnv);
        if (exec.hasReturn) {
            context.setReturnRef(exec.returnValue);
            exec.hasReturn = false;
            exec.returnValue = null;
        } else if (t.isPrimitive()) {
            long bits = exec.resultPrimitive;
            if (t == Type.I) context.setReturnInt((int) bits);
            else if (t == Type.Z) context.setReturnBool(bits != 0);
            else if (t == Type.J) context.setReturnLong(bits);
            else if (t == Type.D) context.setReturnDouble(Double.longBitsToDouble(bits));
            else if (t == Type.F) context.setReturnFloat(Float.intBitsToFloat((int) bits));
        } else {
            context.setReturnRef(exec.resultRef);
        }
    }

    /**
     * Env-free 调用路径：参数已在 FunctionContext 中，局部变量也使用 FunctionContext 数组存储
     * 跳过 Environment 创建，减少 per-call 开销
     */
    private void callEnvFree(FunctionContext<?> context, Interpreter exec) {
        // 确保 FunctionContext 数组容量足够存储所有局部变量
        if (localVarCount > paramCount) {
            context.ensureLocalCapacity(localVarCount);
        }
        // 将原始类型参数统一装箱到 refs 数组，使 getLocal 可以跳过 argTypes 检查
        context.normalizeArgsToRef(paramCount);
        if (definition.getBody().getType() != null && definition.getBody().getType() != ParseResult.ResultType.STATEMENT) {
            exec.consumeCostStep();
        }
        Type t = exec.executeWithFunctionContext(definition.getBody(), context);
        if (exec.hasReturn) {
            context.setReturnRef(exec.returnValue);
            exec.hasReturn = false;
            exec.returnValue = null;
        } else if (t.isPrimitive()) {
            long bits = exec.resultPrimitive;
            if (t == Type.I) context.setReturnInt((int) bits);
            else if (t == Type.Z) context.setReturnBool(bits != 0);
            else if (t == Type.J) context.setReturnLong(bits);
            else if (t == Type.D) context.setReturnDouble(Double.longBitsToDouble(bits));
            else if (t == Type.F) context.setReturnFloat(Float.intBitsToFloat((int) bits));
        } else {
            context.setReturnRef(exec.resultRef);
        }
    }

    @Override
    public SymbolFunction getInfo() {
        return symbolInfo;
    }

    @NotNull
    public FunctionDefinition getDefinition() {
        return definition;
    }

    @NotNull
    public Interpreter getInterpreter() {
        return interpreter;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return definition.getAnnotations();
    }

    @Override
    public String toString() {
        return "UserFunction{" +
                "definition=" + definition +
                '}';
    }
}
