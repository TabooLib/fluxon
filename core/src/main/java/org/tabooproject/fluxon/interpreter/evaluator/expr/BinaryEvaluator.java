package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.util.NumberOperations;

public class BinaryEvaluator extends ExpressionEvaluator<BinaryExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.BINARY;
    }

    @Override
    public Object evaluate(Interpreter interpreter, BinaryExpression result) {
        Object left = interpreter.evaluate(result.getLeft());
        Object right = interpreter.evaluate(result.getRight());

        switch (result.getOperator().getType()) {
            case MINUS:
                checkNumberOperands(left, right);
                return NumberOperations.subtractNumbers((Number) left, (Number) right);
            case DIVIDE:
                checkNumberOperands(left, right);
                return NumberOperations.divideNumbers((Number) left, (Number) right);
            case MULTIPLY:
                checkNumberOperands(left, right);
                return NumberOperations.multiplyNumbers((Number) left, (Number) right);
            case PLUS:
                if (left instanceof Number && right instanceof Number) {
                    return NumberOperations.addNumbers((Number) left, (Number) right);
                }
                if (left instanceof String || right instanceof String) {
                    return String.valueOf(left) + right;
                }
                throw new RuntimeException("Operands must be numbers or strings.");
            case MODULO:
                checkNumberOperands(left, right);
                return NumberOperations.moduloNumbers((Number) left, (Number) right);
            case GREATER:
                checkNumberOperands(left, right);
                return NumberOperations.compareNumbers((Number) left, (Number) right) > 0;
            case GREATER_EQUAL:
                checkNumberOperands(left, right);
                return NumberOperations.compareNumbers((Number) left, (Number) right) >= 0;
            case LESS:
                checkNumberOperands(left, right);
                return NumberOperations.compareNumbers((Number) left, (Number) right) < 0;
            case LESS_EQUAL:
                checkNumberOperands(left, right);
                return NumberOperations.compareNumbers((Number) left, (Number) right) <= 0;
            case EQUAL:
                return isEqual(left, right);
            case NOT_EQUAL:
                return !isEqual(left, right);
            default:
                throw new RuntimeException("Unknown binary operator: " + result.getOperator().getType());
        }
    }
}
