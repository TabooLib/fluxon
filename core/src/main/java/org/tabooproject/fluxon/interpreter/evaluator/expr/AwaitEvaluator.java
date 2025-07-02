package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.AwaitExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Operations;

import static org.objectweb.asm.Opcodes.*;

public class AwaitEvaluator extends ExpressionEvaluator<AwaitExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.AWAIT;
    }

    @Override
    public Object evaluate(Interpreter interpreter, AwaitExpression result) {
        // 使用 Operations.awaitValue 处理异步值
        return Operations.awaitValue(interpreter.evaluate(result.getExpression()));
    }

    @Override
    public Type generateBytecode(AwaitExpression result, CodeContext ctx, MethodVisitor mv) {
        // 获取内部表达式的求值器
        Evaluator<ParseResult> eval = ctx.getEvaluator(result.getExpression());
        if (eval == null) {
            throw new RuntimeException("No evaluator found for await expression");
        }
        // 生成内部表达式的字节码
        if (eval.generateBytecode(result.getExpression(), ctx, mv) == Type.VOID) {
            throw new RuntimeException("Void type is not allowed for await expression");
        }
        // 调用 Operations.awaitValue 方法
        mv.visitMethodInsn(INVOKESTATIC, Operations.TYPE.getPath(), "awaitValue", "(" + Type.OBJECT + ")" + Type.OBJECT, false);
        return Type.OBJECT;
    }
}
