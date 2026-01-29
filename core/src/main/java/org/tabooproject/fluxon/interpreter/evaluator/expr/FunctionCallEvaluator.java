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
        Type[] expectedTypes = expr.resolveExpectedParameterTypes(argTypes);
        FunctionCallHandler handler = selectBytecodeHandler(expr, argTypes);
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
        Type returnType = inferReturnType(expr, analyzer, handler == DeferredOverloadHandler.INSTANCE);
        Type actualReturn = handler.generateFinishCall(expr, ctx, mv, prepareResult, returnType);
        ctx.restoreLocalVarIndex(savedLocalVar);
        return actualReturn;
    }

    private FunctionCallHandler selectHandler(Interpreter interpreter, FunctionCallExpression expr) {
        Function resolvedExt = expr.getResolvedExtensionFunction();
        if (resolvedExt == null && expr.getExtensionPosition() != null) {
            Object target = interpreter.getEnvironment().getTarget();
            if (target != null) {
                resolvedExt = expr.resolveExtensionFunction(target.getClass(), new Type[expr.getArguments().length]);
            }
        }
        if (resolvedExt != null) return ResolvedExtensionHandler.INSTANCE;
        Function cachedDeferred = expr.getDeferredResolvedFunction();
        if (cachedDeferred != null) return new CachedDeferredHandler(cachedDeferred);
        FunctionPosition position = expr.getPosition();
        if (position != null && position.getOverloadSet().size() > 1) return DeferredOverloadHandler.INSTANCE;
        return DynamicResolutionHandler.INSTANCE;
    }

    private FunctionCallHandler selectBytecodeHandler(FunctionCallExpression expr, Type[] argTypes) {
        if (expr.getResolvedExtensionFunction() != null && expr.getResolvedTargetClass() != null) {
            return ResolvedExtensionHandler.INSTANCE;
        }
        FunctionPosition position = expr.getPosition();
        if (position != null && position.getOverloadSet().size() > 1 && hasUnknownType(argTypes)) {
            return DeferredOverloadHandler.INSTANCE;
        }
        return DynamicResolutionHandler.INSTANCE;
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
