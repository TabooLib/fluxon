package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;
import org.tabooproject.fluxon.parser.statement.StatementType;

public class ExprStmtEvaluator extends StatementEvaluator<ExpressionStatement> {

    @Override
    public StatementType getType() {
        return StatementType.EXPRESSION_STATEMENT;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ExpressionStatement result) {
        return interpreter.evaluate(result.getExpression());
    }
}
