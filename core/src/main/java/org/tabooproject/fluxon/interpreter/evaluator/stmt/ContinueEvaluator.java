package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.tabooproject.fluxon.interpreter.ContinueException;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.statement.ContinueStatement;
import org.tabooproject.fluxon.parser.statement.StatementType;

public class ContinueEvaluator extends StatementEvaluator<ContinueStatement> {

    @Override
    public StatementType getType() {
        return StatementType.CONTINUE;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ContinueStatement result) {
        throw new ContinueException();
    }
}
