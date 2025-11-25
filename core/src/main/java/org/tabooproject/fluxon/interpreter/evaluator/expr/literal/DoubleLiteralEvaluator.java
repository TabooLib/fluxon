package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.literal.DoubleLiteral;
import org.tabooproject.fluxon.runtime.Type;

public class DoubleLiteralEvaluator extends ExpressionEvaluator<DoubleLiteral> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.DOUBLE_LITERAL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, DoubleLiteral expr) {
        // 返回预装箱的数值以避免频繁 Double.valueOf 分配
        return expr.getBoxedValue();
    }

    @Override
    public Type generateBytecode(DoubleLiteral result, CodeContext ctx, MethodVisitor mv) {
        mv.visitLdcInsn(result.getValue());
        return boxing(Type.D, mv);
    }
}