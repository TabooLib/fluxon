package org.tabooproject.fluxon.parser.expression;

/**
 * 引用表达式（&变量）
 */
public class ReferenceExpression implements Expression {
    private final Identifier identifier;

    public ReferenceExpression(Identifier identifier) {
        this.identifier = identifier;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.REFERENCE;
    }

    @Override
    public String toString() {
        return "Reference(" + identifier + ")";
    }

    @Override
    public String toPseudoCode() {
        return "&" + identifier.toPseudoCode();
    }
}
