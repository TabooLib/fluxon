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

import java.util.List;

/**
 * 用户自定义的函数
 * 解释执行环境中的函数定义
 */
public class UserFunction implements Function, Symbolic {

    private final SymbolFunction symbolInfo;
    private FunctionSignature signature;

    @NotNull
    private final FunctionDefinition definition;
    @NotNull
    private final Interpreter interpreter;

    public UserFunction(@NotNull FunctionDefinition definition, @NotNull Interpreter interpreter) {
        this.symbolInfo = new SymbolFunction(null, definition.getName(), definition.getParameters().size());
        this.definition = definition;
        this.interpreter = interpreter;
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
        Environment functionEnv = Intrinsics.bindFunctionParameters(
                exec.getEnvironment(),
                definition.getParameters(),
                context,
                definition.getLocalVariables().size()
        );
        if (definition instanceof LambdaFunctionDefinition) {
            functionEnv.setCaptureOffset(((LambdaFunctionDefinition) definition).getCaptureOffset());
        }
        if (definition.getBody().getType() != null && definition.getBody().getType() != ParseResult.ResultType.STATEMENT) {
            exec.consumeCostStep();
        }
        try {
            Type t = exec.executeWithEnvironment(definition.getBody(), functionEnv);
            context.setReturnRef(exec.getResultBoxed(t));
        } catch (ReturnValue returnValue) {
            context.setReturnRef(returnValue.getValue());
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
