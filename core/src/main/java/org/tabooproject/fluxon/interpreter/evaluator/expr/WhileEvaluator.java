package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.BreakException;
import org.tabooproject.fluxon.interpreter.ContinueException;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.WhileExpression;

import static org.tabooproject.fluxon.runtime.stdlib.Operations.*;

public class WhileEvaluator extends ExpressionEvaluator<WhileExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.WHILE;
    }

    @Override
    public Object evaluate(Interpreter interpreter, WhileExpression result) {
        Object last = null;
        while (isTrue(interpreter.evaluate(result.getCondition()))) {
            try {
                last = interpreter.evaluate(result.getBody());
            } catch (ContinueException ignored) {
            } catch (BreakException ignored) {
                break;
            }
        }
        return last;
    }
}
