package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.UnaryExpression;

import static org.tabooproject.fluxon.runtime.stdlib.Operations.*;

public class UnaryEvaluator extends ExpressionEvaluator<UnaryExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.UNARY;
    }

    @Override
    public Object evaluate(Interpreter interpreter, UnaryExpression result) {
        Object right = interpreter.evaluate(result.getRight());
        switch (result.getOperator().getType()) {
            case NOT:
                return !isTrue(right);
            case MINUS:
                checkNumberOperand(right);
                return negateNumber((Number) right);
            default:
                throw new RuntimeException("Unknown unary operator: " + result.getOperator().getType());
        }
    }
}
