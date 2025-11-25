package org.tabooproject.fluxon.parser.expression.literal;

import org.tabooproject.fluxon.parser.expression.ExpressionType;

/**
 * 双精度字面量
 */
public class DoubleLiteral extends Literal {
    private final double value;
    private final Double boxedValue;

    public DoubleLiteral(double value) {
        this.value = value;
        this.boxedValue = value;
    }

    public double getValue() {
        return value;
    }

    public Double getBoxedValue() {
        return boxedValue;
    }

    @Override
    public Object getSourceValue() {
        return boxedValue;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.DOUBLE_LITERAL;
    }

    @Override
    public String toString() {
        return "DoubleLiteral(" + value + ")";
    }

    @Override
    public String toPseudoCode() {
        return String.valueOf(value);
    }
}
