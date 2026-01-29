package org.tabooproject.fluxon.interpreter.evaluator.expr.funccall;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.ExtensionFunctionPosition;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

/**
 * 延迟扩展函数重载解析处理器
 * 当扩展函数有多个重载且参数类型在编译时未知时使用
 *
 * @author sky
 */
@SuppressWarnings("DataFlowIssue")
public class DeferredExtensionHandler implements FunctionCallHandler {

    public static final DeferredExtensionHandler INSTANCE = new DeferredExtensionHandler();

    private DeferredExtensionHandler() {
    }

    @Override
    public FunctionContext<?> prepareCall(Interpreter interpreter, FunctionCallExpression expr, int argCount) {
        ExtensionFunctionPosition extPos = expr.getExtensionPosition();
        Object target = interpreter.getEnvironment().getTarget();
        return Intrinsics.prepareCallDeferredExtension(
                interpreter.getPool(),
                interpreter.getEnvironment(),
                target,
                extPos.getIndex(),
                argCount
        );
    }

    @Override
    public Type finishCall(Interpreter interpreter, FunctionCallExpression expr, FunctionContext<?> ctx) {
        ExtensionFunctionPosition extPos = expr.getExtensionPosition();
        Object target = interpreter.getEnvironment().getTarget();
        Type[] argTypes = ctx.collectArgTypes();
        ExtensionDispatchTable dispatchTable = FluxonRuntime.getInstance().getCachedDispatchTables()[extPos.getIndex()];
        Function resolved = dispatchTable.resolve(target.getClass(), argTypes);
        if (resolved != null) {
            ctx.setFunctionAndConvertArgs(resolved, argTypes);
        }
        return FunctionCallHandlers.executeSync(interpreter, ctx, resolved != null ? resolved : ctx.getFunction());
    }

    @Override
    public PrepareCallResult generatePrepareCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, int argCount) {
        return DynamicResolutionHandler.INSTANCE.generatePrepareCall(expr, ctx, mv, argCount);
    }

    @Override
    public Type generateFinishCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, PrepareCallResult prepareResult, Type returnType) {
        mv.visitVarInsn(ALOAD, prepareResult.ctxSlot);
        mv.visitLdcInsn(expr.getExtensionPositionIndex());
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "finishCallDeferredExtension",
                "(" + FunctionContext.TYPE.getDescriptor() + "I)" + Type.OBJECT,
                false
        );
        return Type.OBJECT;
    }
}
