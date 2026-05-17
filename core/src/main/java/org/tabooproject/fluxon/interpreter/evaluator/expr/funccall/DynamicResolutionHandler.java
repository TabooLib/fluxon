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
 * 动态解析处理器
 *
 * @author sky
 */
public class DynamicResolutionHandler implements FunctionCallHandler {

    public static final DynamicResolutionHandler INSTANCE = new DynamicResolutionHandler();

    private DynamicResolutionHandler() {}

    @Override
    public FunctionContext<?> prepareCall(Interpreter interpreter, FunctionCallExpression expr, int argCount) {
        if (expr.isDirectContextCall()) {
            return Intrinsics.prepareContextCall(
                    interpreter.getPool(),
                    interpreter.getEnvironment(),
                    expr.getFunctionName(),
                    argCount,
                    expr.getPositionIndex(),
                    expr.getExtensionPositionIndex()
            );
        }
        return Intrinsics.prepareCall(
                interpreter.getPool(),
                interpreter.getEnvironment(),
                expr.getFunctionName(),
                argCount,
                expr.getPositionIndex(),
                expr.getExtensionPositionIndex()
        );
    }

    @Override
    public Type finishCall(Interpreter interpreter, FunctionCallExpression expr, FunctionContext<?> ctx) {
        return FunctionCallHandlers.executeSync(interpreter, ctx);
    }

    @Override
    public PrepareCallResult generatePrepareCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, int argCount) {
        Instructions.loadPool(mv, ctx);
        Instructions.loadEnvironment(mv, ctx);
        mv.visitLdcInsn(expr.getFunctionName());
        mv.visitLdcInsn(argCount);
        mv.visitLdcInsn(expr.getPositionIndex());
        mv.visitLdcInsn(expr.getExtensionPositionIndex());
        String methodName = expr.isDirectContextCall() ? "prepareContextCall" : "prepareCall";
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                methodName,
                "(" + FunctionContextPool.TYPE + Environment.TYPE + Type.STRING + "III)" + FunctionContext.TYPE,
                false
        );
        int ctxSlot = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, ctxSlot);
        return new PrepareCallResult(ctxSlot);
    }

    @Override
    public Type generateFinishCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, PrepareCallResult prepareResult, Type returnType) {
        mv.visitVarInsn(ALOAD, prepareResult.ctxSlot);
        FunctionCallHandlers.emitFinishCall(returnType, isKnownSync(expr), mv);
        return returnType;
    }

    /**
     * 编译期判断函数是否确定为同步调用
     */
    private static boolean isKnownSync(FunctionCallExpression expr) {
        // 无扩展函数时，检查系统函数位置
        FunctionPosition position = expr.getPosition();
        if (position != null) {
            for (Function f : position.getOverloadSet().getOverloads()) {
                if (f.isAsync() || f.isPrimarySync()) return false;
            }
            return true;
        }
        // 无位置信息（包括用户定义函数），无法确定
        return false;
    }
}
