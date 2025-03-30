package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.GroupingExpression;

public class GroupingEvaluator extends ExpressionEvaluator<GroupingExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.GROUPING;
    }

    @Override
    public Object evaluate(Interpreter interpreter, GroupingExpression result) {
        return interpreter.evaluate(result.getExpression());
    }
}
