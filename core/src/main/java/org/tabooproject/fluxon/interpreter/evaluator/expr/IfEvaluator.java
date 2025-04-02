package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.IfExpression;

import static org.tabooproject.fluxon.runtime.stdlib.Operations.isTrue;

public class IfEvaluator extends ExpressionEvaluator<IfExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.IF;
    }

    @Override
    public Object evaluate(Interpreter interpreter, IfExpression result) {
        if (isTrue(interpreter.evaluate(result.getCondition()))) {
            return interpreter.evaluate(result.getThenBranch());
        } else if (result.getElseBranch() != null) {
            return interpreter.evaluate(result.getElseBranch());
        } else {
            return null;
        }
    }
}
