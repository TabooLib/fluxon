package org.tabooproject.fluxon.parser.expressions;

/**
 * 整数字面量
 */
public class IntLiteral extends Literal {
    private final int value;

    public IntLiteral(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
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
