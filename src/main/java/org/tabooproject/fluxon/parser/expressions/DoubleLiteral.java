package org.tabooproject.fluxon.parser.expressions;

/**
 * 双精度字面量
 */
public class DoubleLiteral extends Literal {
    private final double value;

    public DoubleLiteral(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
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
