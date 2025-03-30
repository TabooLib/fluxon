package org.tabooproject.fluxon.parser.statement;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 表达式语句
 * 表示一个表达式作为语句使用
 */
public class ExpressionStatement implements Statement {
    private final ParseResult expression;

    public ExpressionStatement(ParseResult expression) {
        this.expression = expression;
    }

    public ParseResult getExpression() {
        return expression;
    }

    @Override
    public StatementType getStatementType() {
        return StatementType.EXPRESSION_STATEMENT;
    }

    @Override
    public String toString() {
        return "ExpressionStatement(" + expression + ")";
    }

    @Override
    public String toPseudoCode() {
        return expression.toPseudoCode();
    }
}
