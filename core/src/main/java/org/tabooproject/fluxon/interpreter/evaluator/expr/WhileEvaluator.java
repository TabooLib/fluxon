package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.WhileExpression;

public class WhileEvaluator extends ExpressionEvaluator<WhileExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.WHILE;
    }

    @Override
    public Object evaluate(Interpreter interpreter, WhileExpression result) {
        Object last = null;
        while (isTrue(interpreter.evaluate(result.getCondition()))) {
            last = interpreter.evaluate(result.getBody());
        }
        return last;
    }
}
