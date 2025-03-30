package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * Await 表达式
 */
public class AwaitExpression implements Expression {
    private final ParseResult expression;

    public AwaitExpression(ParseResult expression) {
        this.expression = expression;
    }

    public ParseResult getExpression() {
        return expression;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.AWAIT;
    }

    @Override
    public String toString() {
        return "Await(" + expression + ")";
    }

    @Override
    public String toPseudoCode() {
        return "await " + expression.toPseudoCode();
    }
}
