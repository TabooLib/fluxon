package org.tabooproject.fluxon.parser.expressions;

/**
 * 浮点数字面量
 */
public class FloatLiteral extends Literal {
    private final float value;

    public FloatLiteral(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.FLOAT_LITERAL;
    }

    @Override
    public String toString() {
        return "FloatLiteral(" + value + ")";
    }

    @Override
    public String toPseudoCode() {
        return value + "f";
    }
}
