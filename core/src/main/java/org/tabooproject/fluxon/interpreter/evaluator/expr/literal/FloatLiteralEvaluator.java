package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.literal.FloatLiteral;

public class FloatLiteralEvaluator extends ExpressionEvaluator<FloatLiteral> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.FLOAT_LITERAL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, FloatLiteral expr) {
        return expr.getValue();
    }

    @Override
    public void generateBytecode(FloatLiteral result, MethodVisitor mv) {
        mv.visitLdcInsn(result.getValue());
    }
}