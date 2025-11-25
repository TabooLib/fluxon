package org.tabooproject.fluxon.parser.expression.literal;

import org.tabooproject.fluxon.parser.expression.ExpressionType;

/**
 * 整数字面量
 */
public class IntLiteral extends Literal {
    private final int value;
    private final Integer boxedValue;

    public IntLiteral(int value) {
        this.value = value;
        this.boxedValue = value;
    }

    public int getValue() {
        return value;
    }

    public Integer getBoxedValue() {
        return boxedValue;
    }

    @Override
    public Object getSourceValue() {
        return boxedValue;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.INT_LITERAL;
    }

    @Override
    public String toString() {
        return "IntLiteral(" + value + ")";
    }

    @Override
    public String toPseudoCode() {
        return String.valueOf(value);
    }
}
