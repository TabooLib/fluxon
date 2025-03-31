package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.tabooproject.fluxon.interpreter.BreakException;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.statement.BreakStatement;
import org.tabooproject.fluxon.parser.statement.StatementType;

public class BreakEvaluator extends StatementEvaluator<BreakStatement> {

    @Override
    public StatementType getType() {
        return StatementType.BREAK;
    }

    @Override
    public Object evaluate(Interpreter interpreter, BreakStatement result) {
        throw new BreakException();
    }
}
