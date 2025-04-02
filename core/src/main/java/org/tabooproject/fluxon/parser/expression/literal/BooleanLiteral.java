package org.tabooproject.fluxon.parser.expression.literal;

import org.tabooproject.fluxon.parser.expression.ExpressionType;

/**
 * 布尔字面量
 */
public class BooleanLiteral extends Literal {
    private final boolean value;

    public BooleanLiteral(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.BOOLEAN_LITERAL;
    }

    @Override
    public String toString() {
        return "BooleanLiteral(" + value + ")";
    }

    @Override
    public String toPseudoCode() {
        return String.valueOf(value);
    }
}
