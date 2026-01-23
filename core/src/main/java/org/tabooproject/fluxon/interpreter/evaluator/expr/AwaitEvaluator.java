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
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public class AwaitEvaluator extends ExpressionEvaluator<AwaitExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.AWAIT;
    }

    @Override
    public Type evaluate(Interpreter interpreter, AwaitExpression result) {
        Type t = interpreter.evaluate(result.getExpression());
        interpreter.resultRef = Intrinsics.awaitValue(interpreter.getResultBoxed(t));
        return Type.OBJECT;
    }

    @Override
    public Type generateBytecode(AwaitExpression result, CodeContext ctx, MethodVisitor mv) {
        // 获取内部表达式的求值器
        Evaluator<ParseResult> eval = ctx.getEvaluator(result.getExpression());
        if (eval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for await expression");
        }
        // 生成内部表达式的字节码
        Type t = eval.generateBytecode(result.getExpression(), ctx, mv);
        if (t == Type.VOID) {
            throw new VoidError("Void type is not allowed for await expression");
        }
        boxing(t, mv);
        // 调用 Operations.awaitValue 方法
        mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "awaitValue", "(" + Type.OBJECT + ")" + Type.OBJECT, false);
        return Type.OBJECT;
    }
}
