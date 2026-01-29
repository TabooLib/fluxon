package org.tabooproject.fluxon.interpreter.evaluator.expr.funccall;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.Type;

/**
 * 函数调用策略处理器
 *
 * @author sky
 */
public interface FunctionCallHandler {

    /**
     * 解释执行：准备函数调用上下文
     */
    FunctionContext<?> prepareCall(Interpreter interpreter, FunctionCallExpression expr, int argCount);

    /**
     * 解释执行：完成函数调用并返回结果
     */
    Type finishCall(Interpreter interpreter, FunctionCallExpression expr, FunctionContext<?> ctx);

    /**
     * 字节码生成：生成 prepareCall 调用
     */
    PrepareCallResult generatePrepareCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, int argCount);

    /**
     * 字节码生成：生成 finishCall 调用
     */
    Type generateFinishCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, PrepareCallResult prepareResult, Type returnType);

    /**
     * prepareCall 生成结果
     */
    class PrepareCallResult {
        public final int ctxSlot;
        public final int deferredSlot;

        public PrepareCallResult(int ctxSlot) {
            this(ctxSlot, -1);
        }

        public PrepareCallResult(int ctxSlot, int deferredSlot) {
            this.ctxSlot = ctxSlot;
            this.deferredSlot = deferredSlot;
        }
    }
}
