package org.tabooproject.fluxon.interpreter.evaluator.expr.funccall;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.parser.FunctionPosition;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.objectweb.asm.Opcodes.*;

/**
 * 延迟重载解析处理器
 *
 * @author sky
 */
@SuppressWarnings("DataFlowIssue")
public class DeferredOverloadHandler implements FunctionCallHandler {

    public static final DeferredOverloadHandler INSTANCE = new DeferredOverloadHandler();

    private DeferredOverloadHandler() {
    }

    @Override
    public FunctionContext<?> prepareCall(Interpreter interpreter, FunctionCallExpression expr, int argCount) {
        FunctionPosition position = expr.getPosition();
        return Intrinsics.prepareCallDeferred(interpreter.getPool(), interpreter.getEnvironment(), position.getOverloadSet(), argCount);
    }

    @Override
    public Type finishCall(Interpreter interpreter, FunctionCallExpression expr, FunctionContext<?> ctx) {
        FunctionPosition position = expr.getPosition();
        Type[] argTypes = ctx.collectArgTypes();
        Function resolved = position.getOverloadSet().resolve(argTypes);
        if (resolved != null) {
            ctx.setFunctionAndConvertArgs(resolved, argTypes);
            expr.setDeferredResolvedFunction(resolved);
        } else {
            resolved = ctx.getFunction();
        }
        return FunctionCallHandlers.executeSync(interpreter, ctx, resolved);
    }

    @Override
    public PrepareCallResult generatePrepareCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, int argCount) {
        FunctionPosition position = expr.getPosition();
        int deferredSlot = ctx.addDeferredOverloadSet(position.getOverloadSet());
        Instructions.loadPool(mv, ctx);
        Instructions.loadEnvironment(mv, ctx);
        mv.visitFieldInsn(GETSTATIC, ctx.getInternalName(), "DEFERRED_OVERLOAD_SETS", "[" + OverloadSet.TYPE.getDescriptor());
        mv.visitLdcInsn(deferredSlot);
        mv.visitInsn(AALOAD);
        mv.visitLdcInsn(argCount);
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "prepareCallDeferred",
                "(" + FunctionContextPool.TYPE + Environment.TYPE + OverloadSet.TYPE + "I)" + FunctionContext.TYPE,
                false
        );
        int ctxSlot = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, ctxSlot);
        return new PrepareCallResult(ctxSlot, deferredSlot);
    }

    @Override
    public Type generateFinishCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, PrepareCallResult prepareResult, Type returnType) {
        mv.visitVarInsn(ALOAD, prepareResult.ctxSlot);
        mv.visitFieldInsn(GETSTATIC, ctx.getInternalName(), "DEFERRED_OVERLOAD_SETS", "[" + OverloadSet.TYPE.getDescriptor());
        mv.visitLdcInsn(prepareResult.deferredSlot);
        mv.visitInsn(AALOAD);
        mv.visitFieldInsn(GETSTATIC, ctx.getInternalName(), "DEFERRED_CACHE", "[" + Function.TYPE.getDescriptor());
        mv.visitLdcInsn(prepareResult.deferredSlot);
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "finishCallDeferred",
                "(" + FunctionContext.TYPE.getDescriptor() + OverloadSet.TYPE.getDescriptor() + "[" + Function.TYPE.getDescriptor() + "I)" + Type.OBJECT,
                false
        );
        return Type.OBJECT;
    }
}
