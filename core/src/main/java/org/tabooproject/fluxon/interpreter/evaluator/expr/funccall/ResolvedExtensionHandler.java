package org.tabooproject.fluxon.interpreter.evaluator.expr.funccall;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.objectweb.asm.Opcodes.*;

/**
 * 已解析扩展函数处理器
 *
 * @author sky
 */
public class ResolvedExtensionHandler implements FunctionCallHandler {

    public static final ResolvedExtensionHandler INSTANCE = new ResolvedExtensionHandler();

    private ResolvedExtensionHandler() {
    }

    @Override
    public FunctionContext<?> prepareCall(Interpreter interpreter, FunctionCallExpression expr, int argCount) {
        Function resolvedExt = expr.getResolvedExtensionFunction();
        return Intrinsics.prepareCallDirect(interpreter.getPool(), interpreter.getEnvironment(), resolvedExt, argCount);
    }

    @Override
    public Type finishCall(Interpreter interpreter, FunctionCallExpression expr, FunctionContext<?> ctx) {
        return FunctionCallHandlers.executeSync(interpreter, ctx);
    }

    @Override
    public PrepareCallResult generatePrepareCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, int argCount) {
        int funcSlot = ctx.addResolvedExtensionFunction(
                expr.getResolvedDispatchTableIndex(),
                expr.getResolvedTargetClass(),
                expr.getResolvedOverloadIndex()
        );
        Instructions.loadPool(mv, ctx);
        Instructions.loadEnvironment(mv, ctx);
        mv.visitFieldInsn(GETSTATIC, ctx.getInternalName(), "RESOLVED_EXT_FUNCTIONS", "[" + Function.TYPE.getDescriptor());
        mv.visitLdcInsn(funcSlot);
        mv.visitInsn(AALOAD);
        mv.visitLdcInsn(argCount);
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "prepareCallDirect",
                "(" + FunctionContextPool.TYPE + Environment.TYPE + Function.TYPE + "I)" + FunctionContext.TYPE,
                false
        );
        int ctxSlot = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, ctxSlot);
        return new PrepareCallResult(ctxSlot);
    }

    @Override
    public Type generateFinishCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, PrepareCallResult prepareResult, Type returnType) {
        mv.visitVarInsn(ALOAD, prepareResult.ctxSlot);
        Function resolved = expr.getResolvedExtensionFunction();
        boolean knownSync = resolved != null && !resolved.isAsync() && !resolved.isPrimarySync();
        FunctionCallHandlers.emitFinishCall(returnType, knownSync, mv);
        return returnType;
    }
}
