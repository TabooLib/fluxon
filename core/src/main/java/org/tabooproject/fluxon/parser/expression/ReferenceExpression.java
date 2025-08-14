package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.expression.literal.Identifier;

/**
 * 引用表达式（&变量）
 */
public class ReferenceExpression implements Expression {
    private final Identifier identifier;
    private final boolean isOptional;

    public ReferenceExpression(Identifier identifier, boolean isOptional) {
        this.identifier = identifier;
        this.isOptional = isOptional;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public boolean isOptional() {
        return isOptional;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.REFERENCE;
    }

    @Override
    public String toString() {
        return "Reference(" + identifier + ", isOptional: " + isOptional + ")";
    }

    @Override
    public String toPseudoCode() {
        if (isOptional) {
            return "&?" + identifier.toPseudoCode();
        } else {
            return "&" + identifier.toPseudoCode();
        }
    }
}
