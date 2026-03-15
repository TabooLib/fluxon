package org.tabooproject.fluxon.interpreter.evaluator.expr.funccall;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.FunctionPosition;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.*;

/**
 * 已解析系统函数处理器
 * 编译期已确定为单重载系统函数时，在 <clinit> 中一次性解析 Function 引用，
 * 运行时通过 GETSTATIC + AALOAD 直接获取，跳过 resolveFunction 调用链。
 *
 * @author sky
 */
public class ResolvedSystemFunctionHandler implements FunctionCallHandler {

    public static final ResolvedSystemFunctionHandler INSTANCE = new ResolvedSystemFunctionHandler();

    private ResolvedSystemFunctionHandler() {
    }

    @Override
    public FunctionContext<?> prepareCall(Interpreter interpreter, FunctionCallExpression expr, int argCount) {
        // 解释模式走动态路径
        return DynamicResolutionHandler.INSTANCE.prepareCall(interpreter, expr, argCount);
    }

    @Override
    public Type finishCall(Interpreter interpreter, FunctionCallExpression expr, FunctionContext<?> ctx) {
        return FunctionCallHandlers.executeSync(interpreter, ctx);
    }

    @Override
    public PrepareCallResult generatePrepareCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, int argCount) {
        int funcSlot = ctx.addResolvedSystemFunction(expr.getPositionIndex());
        String internalName = ctx.getInternalName();
        return FunctionCallHandlers.emitPrepareCallDirect(ctx, mv, argCount, () -> {
            mv.visitFieldInsn(GETSTATIC, internalName, "RESOLVED_SYS_FUNCTIONS", "[" + Function.TYPE.getDescriptor());
            mv.visitLdcInsn(funcSlot);
            mv.visitInsn(AALOAD);
        });
    }

    @Override
    public Type generateFinishCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, PrepareCallResult prepareResult, Type returnType) {
        mv.visitVarInsn(ALOAD, prepareResult.ctxSlot);
        FunctionPosition position = expr.getPosition();
        boolean knownSync = isKnownSync(position);
        FunctionCallHandlers.emitFinishCall(returnType, knownSync, mv);
        return returnType;
    }

    /**
     * 单重载系统函数的同步性判断
     */
    private static boolean isKnownSync(FunctionPosition position) {
        if (position == null) return false;
        for (Function f : position.getOverloadSet().getOverloads()) {
            if (f.isAsync() || f.isPrimarySync()) return false;
        }
        return true;
    }
}
