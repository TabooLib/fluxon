package org.tabooproject.fluxon.interpreter.evaluator.expr.funccall;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

/**
 * 缓存的延迟解析结果处理器（仅解释模式）
 *
 * @author sky
 */
public class CachedDeferredHandler implements FunctionCallHandler {

    private final Function cachedFunction;

    public CachedDeferredHandler(Function cachedFunction) {
        this.cachedFunction = cachedFunction;
    }

    @Override
    public FunctionContext<?> prepareCall(Interpreter interpreter, FunctionCallExpression expr, int argCount) {
        return Intrinsics.prepareCallDirect(interpreter.getPool(), interpreter.getEnvironment(), cachedFunction, argCount);
    }

    @Override
    public Type finishCall(Interpreter interpreter, FunctionCallExpression expr, FunctionContext<?> ctx) {
        return FunctionCallHandlers.executeSync(interpreter, ctx);
    }

    @Override
    public PrepareCallResult generatePrepareCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, int argCount) {
        throw new UnsupportedOperationException("CachedDeferredHandler is only for interpret mode");
    }

    @Override
    public Type generateFinishCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, PrepareCallResult prepareResult, Type returnType) {
        throw new UnsupportedOperationException("CachedDeferredHandler is only for interpret mode");
    }
}
