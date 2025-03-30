package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.statement.Block;
import org.tabooproject.fluxon.parser.statement.StatementType;

public class BlockEvaluator extends StatementEvaluator<Block> {

    @Override
    public StatementType getType() {
        return StatementType.BLOCK;
    }

    @Override
    public Object evaluate(Interpreter interpreter, Block result) {
        interpreter.enterScope();
        try {
            Object last = null;
            for (ParseResult statement : result.getStatements()) {
                last = interpreter.evaluate(statement);
            }
            return last;
        } finally {
            interpreter.exitScope();
        }
    }
}
