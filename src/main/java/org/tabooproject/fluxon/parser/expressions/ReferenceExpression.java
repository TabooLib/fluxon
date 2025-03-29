package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 引用表达式（&变量）
 */
public class ReferenceExpression implements Expression {
    private final ParseResult expression;

    public ReferenceExpression(ParseResult expression) {
        this.expression = expression;
    }

    public ParseResult getExpression() {
        return expression;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.REFERENCE;
    }

    @Override
    public String toString() {
        return "Reference(" + expression + ")";
    }

    @Override
    public String toPseudoCode() {
        return "&" + expression.toPseudoCode();
    }
}
