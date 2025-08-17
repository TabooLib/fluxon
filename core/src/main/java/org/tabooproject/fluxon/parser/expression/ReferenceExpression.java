package org.tabooproject.fluxon.parser.expression;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.VariablePosition;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;

/**
 * 引用表达式（&变量）
 */
public class ReferenceExpression implements Expression {
    private final Identifier identifier;
    private final boolean isOptional;

    @Nullable
    private final VariablePosition position;

    public ReferenceExpression(Identifier identifier, boolean isOptional, @Nullable VariablePosition position) {
        this.identifier = identifier;
        this.isOptional = isOptional;
        this.position = position;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public boolean isOptional() {
        return isOptional;
    }

    @Nullable
    public VariablePosition getPosition() {
        return position;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.REFERENCE;
    }

    @Override
    public String toString() {
        return "Reference(" + identifier + ", isOptional: " + isOptional + ", position: " + position + ")";
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
