package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.interpreter.evaluator.expr.funccall.*;
import org.tabooproject.fluxon.interpreter.evaluator.expr.funccall.FunctionCallHandler.PrepareCallResult;
import org.tabooproject.fluxon.parser.ExtensionFunctionPosition;
import org.tabooproject.fluxon.parser.FunctionPosition;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;

import java.util.Map;

import static org.objectweb.asm.Opcodes.ALOAD;

/**
 * 函数调用表达式求值器
 *
 * @author sky
 */
public class FunctionCallEvaluator extends ExpressionEvaluator<FunctionCallExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.FUNCTION_CALL;
    }

    @Override
    public Type evaluate(Interpreter interpreter, FunctionCallExpression expr) {
        ParseResult[] args = expr.getArguments();
        int argCount = args.length;
        FunctionCallHandler handler = selectHandler(interpreter, expr);
        FunctionContext<?> ctx = handler.prepareCall(interpreter, expr, argCount);
        Type[] expectedTypes = getExpectedTypes(ctx.getFunction(), handler == DeferredOverloadHandler.INSTANCE);
        for (int i = 0; i < argCount; i++) {
            Type t = interpreter.evaluate(args[i]);
            Type expected = (expectedTypes != null && i < expectedTypes.length) ? expectedTypes[i] : Type.OBJECT;
            FunctionCallHandlers.setArgument(ctx, i, t, expected, interpreter);
        }
        return handler.finishCall(interpreter, expr, ctx);
    }

    @Override
    public Type generateBytecode(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv) {
        ParseResult[] args = expr.getArguments();
        int argCount = args.length;
        int savedLocalVar = ctx.getLocalVarIndex();
        TypeAnalyzer analyzer = ctx.getTypeAnalyzer();
        Type[] argTypes = inferArgTypes(args, analyzer);
        FunctionCallHandler handler = selectBytecodeHandler(expr, argTypes, analyzer);
        boolean isDeferred = handler == DeferredOverloadHandler.INSTANCE || handler == DeferredExtensionHandler.INSTANCE;
        // 延迟解析时不使用 expectedTypes，让运行时处理类型转换
        Type[] expectedTypes = isDeferred ? null : expr.resolveExpectedParameterTypes(argTypes);
        PrepareCallResult prepareResult = handler.generatePrepareCall(expr, ctx, mv, argCount);
        for (int i = 0; i < argCount; i++) {
            mv.visitVarInsn(ALOAD, prepareResult.ctxSlot);
            mv.visitLdcInsn(i);
            Evaluator<ParseResult> argEval = ctx.getEvaluator(args[i]);
            if (argEval == null) throw new EvaluatorNotFoundError("No evaluator found for argument expression");
            Type t = argEval.generateBytecode(args[i], ctx, mv);
            if (t == Type.VOID) throw new VoidError("Void type is not allowed for function arguments");
            Type expected = (expectedTypes != null && i < expectedTypes.length) ? expectedTypes[i] : null;
            FunctionCallHandlers.emitSetArg(t, expected, mv);
        }
        Type returnType = inferReturnType(expr, analyzer, isDeferred);
        Type actualReturn = handler.generateFinishCall(expr, ctx, mv, prepareResult, returnType);
        ctx.restoreLocalVarIndex(savedLocalVar);
        return actualReturn;
    }

    private FunctionCallHandler selectHandler(Interpreter interpreter, FunctionCallExpression expr) {
        // 无状态判断：不写入共享 AST，避免多线程缓存竞争
        ExtensionFunctionPosition extPos = expr.getExtensionPosition();
        if (extPos != null) {
            Object target = interpreter.getEnvironment().getTarget();
            if (target != null) {
                ExtensionDispatchTable dispatchTable = FluxonRuntime.getInstance().getCachedDispatchTables()[extPos.getIndex()];
                OverloadSet overloadSet = dispatchTable.resolveOverloadSet(target.getClass());
                if (overloadSet != null && overloadSet.size() > 1) {
                    return DeferredExtensionHandler.INSTANCE;
                }
            }
        }
        FunctionPosition position = expr.getPosition();
        if (position != null && position.getOverloadSet().size() > 1) {
            return DeferredOverloadHandler.INSTANCE;
        }
        return DynamicResolutionHandler.INSTANCE;
    }

    private FunctionCallHandler selectBytecodeHandler(FunctionCallExpression expr, Type[] argTypes, TypeAnalyzer analyzer) {
        // 扩展函数已在编译时解析
        if (expr.getResolvedExtensionFunction() != null && expr.getResolvedTargetClass() != null) {
            return ResolvedExtensionHandler.INSTANCE;
        }
        int argCount = argTypes != null ? argTypes.length : expr.getArguments().length;
        // 检查扩展函数重载
        ExtensionFunctionPosition extPos = expr.getExtensionPosition();
        if (extPos != null && needsDeferredExtensionResolution(extPos, argCount, analyzer)) {
            return DeferredExtensionHandler.INSTANCE;
        }
        // 检查系统函数重载
        FunctionPosition position = expr.getPosition();
        if (position != null && position.getOverloadSet().size() > 1 && hasUnknownType(argTypes)) {
            return DeferredOverloadHandler.INSTANCE;
        }
        return DynamicResolutionHandler.INSTANCE;
    }

    /**
     * 判断扩展函数是否需要延迟重载解析
     */
    private boolean needsDeferredExtensionResolution(ExtensionFunctionPosition extPos, int argCount, TypeAnalyzer analyzer) {
        Type targetType = analyzer != null ? analyzer.getCurrentTargetType() : null;
        boolean isUnknownTarget = targetType == null || targetType == Type.OBJECT;
        // target 类型未知时，检查所有 OverloadSet
        if (isUnknownTarget) {
            for (OverloadSet overloadSet : extPos.getOverloadSets().values()) {
                if (countMatchingOverloads(overloadSet, argCount) > 1) {
                    return true;
                }
            }
            return false;
        }
        // target 类型已知时，查找匹配的 OverloadSet
        Class<?> targetClass = targetType.getSource();
        if (targetClass == null) {
            return false;
        }
        for (Map.Entry<Class<?>, OverloadSet> entry : extPos.getOverloadSets().entrySet()) {
            if (entry.getKey().isAssignableFrom(targetClass)) {
                return countMatchingOverloads(entry.getValue(), argCount) > 1;
            }
        }
        return false;
    }

    /**
     * 统计参数数量匹配的重载个数
     */
    private int countMatchingOverloads(OverloadSet overloadSet, int argCount) {
        int count = 0;
        for (Function f : overloadSet.getOverloads()) {
            FunctionSignature sig = f.getSignature();
            if (sig == null || sig.getParameterCount() == argCount) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void analyzeTypes(FunctionCallExpression expr, TypeAnalyzer analyzer) {
        ParseResult[] args = expr.getArguments();
        for (ParseResult arg : args) {
            analyzer.analyzeNode(arg);
        }
        Type[] argTypes = inferArgTypes(args, analyzer);
        FunctionPosition position = expr.getPosition();
        if (position != null) {
            expr.setResolvedPositionIndex(position.resolveIndex(argTypes));
        }
        Type targetType = analyzer.getCurrentTargetType();
        if (targetType != null && targetType.getSource() != null) {
            expr.resolveExtensionFunction(targetType.getSource(), argTypes);
        }
    }

    @Override
    public Type inferResultType(FunctionCallExpression expr, TypeAnalyzer analyzer) {
        Function resolvedExt = expr.getResolvedExtensionFunction();
        if (resolvedExt != null) return resolvedExt.getReturnType();
        Type[] argTypes = inferArgTypes(expr.getArguments(), analyzer);
        OverloadSet overloadSet = FluxonRuntime.getInstance().getSystemFunctions().get(expr.getFunctionName());
        if (overloadSet != null) {
            Function function = overloadSet.resolve(argTypes);
            if (function != null) return function.getReturnType();
        }
        ExtensionFunctionPosition extPos = expr.getExtensionPosition();
        if (extPos != null) {
            Type inferred = extPos.inferReturnType(argTypes);
            if (inferred != null) return inferred;
        }
        return Type.OBJECT;
    }

    private Type[] getExpectedTypes(Function function, boolean isDeferred) {
        if (isDeferred || function.getSignature() == null) return null;
        return function.getSignature().getParameterTypes();
    }

    private Type[] inferArgTypes(ParseResult[] args, TypeAnalyzer analyzer) {
        if (analyzer == null) return null;
        Type[] types = new Type[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = analyzer.inferType(args[i]);
        }
        return types;
    }

    private boolean hasUnknownType(Type[] types) {
        if (types == null) return false;
        for (Type t : types) {
            if (t == Type.OBJECT) return true;
        }
        return false;
    }

    private Type inferReturnType(FunctionCallExpression expr, TypeAnalyzer analyzer, boolean isDeferred) {
        if (isDeferred || analyzer == null) return Type.OBJECT;
        Type inferred = inferResultType(expr, analyzer);
        return (inferred == Type.VOID) ? Type.OBJECT : inferred;
    }
}
