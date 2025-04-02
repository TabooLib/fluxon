package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.literal.LongLiteral;

public class LongLiteralEvaluator extends ExpressionEvaluator<LongLiteral> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.LONG_LITERAL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, LongLiteral expr) {
        return expr.getValue();
    }

    @Override
    public void generateBytecode(LongLiteral result, MethodVisitor mv) {
        mv.visitLdcInsn(result.getValue());
    }
}