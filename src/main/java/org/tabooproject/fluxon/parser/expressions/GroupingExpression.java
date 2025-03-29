package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 分组表达式
 */
public class GroupingExpression implements Expression {
    private final ParseResult expression;

    public GroupingExpression(ParseResult expression) {
        this.expression = expression;
    }

    public ParseResult getExpression() {
        return expression;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.GROUPING;
    }

    @Override
    public String toString() {
        return "Grouping(" + expression + ")";
    }

    @Override
    public String toPseudoCode() {
        return "(" + expression.toPseudoCode() + ")";
    }
}
