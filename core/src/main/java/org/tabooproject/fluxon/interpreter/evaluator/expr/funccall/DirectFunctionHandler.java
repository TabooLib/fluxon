package org.tabooproject.fluxon.interpreter.evaluator.expr.funccall;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * 用户定义函数直接引用处理器
 * 编译期已知函数为同一编译单元的用户定义函数时，
 * 直接通过静态字段引用调用 prepareCallDirect，跳过运行时名称查找。
 *
 * @author sky
 */
public class DirectFunctionHandler implements FunctionCallHandler {

    public static final DirectFunctionHandler INSTANCE = new DirectFunctionHandler();

    private DirectFunctionHandler() {
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
        String funcName = expr.getFunctionName();
        String ownerClass = ctx.getUserFunctionOwner(funcName);
        String funcClass = ownerClass + funcName;
        // 直接引用静态字段，跳过 Environment 名称查找
        return FunctionCallHandlers.emitPrepareCallDirect(ctx, mv, argCount,
                () -> mv.visitFieldInsn(GETSTATIC, ownerClass, funcName, "L" + funcClass + ";"));
    }

    @Override
    public Type generateFinishCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, PrepareCallResult prepareResult, Type returnType) {
        mv.visitVarInsn(ALOAD, prepareResult.ctxSlot);
        // 用户定义函数默认视为同步调用
        FunctionCallHandlers.emitFinishCall(returnType, true, mv);
        return returnType;
    }
}
