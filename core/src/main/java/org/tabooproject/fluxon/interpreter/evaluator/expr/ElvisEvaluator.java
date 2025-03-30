package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ElvisExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;

public class ElvisEvaluator extends ExpressionEvaluator<ElvisExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.ELVIS;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ElvisExpression result) {
        Object object = interpreter.evaluate(result.getCondition());
        if (object == null) {
            return interpreter.evaluate(result.getAlternative());
        }
        return object;
    }
}
