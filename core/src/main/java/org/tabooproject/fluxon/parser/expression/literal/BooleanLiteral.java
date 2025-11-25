package org.tabooproject.fluxon.parser.expression.literal;

import org.tabooproject.fluxon.parser.expression.ExpressionType;

/**
 * 布尔字面量
 */
public class BooleanLiteral extends Literal {
    private final boolean value;
    private final Boolean boxedValue;

    public BooleanLiteral(boolean value) {
        this.value = value;
        this.boxedValue = value;
    }

    public boolean getValue() {
        return value;
    }

    public Boolean getBoxedValue() {
        return boxedValue;
    }

    @Override
    public Object getSourceValue() {
        return boxedValue;
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
