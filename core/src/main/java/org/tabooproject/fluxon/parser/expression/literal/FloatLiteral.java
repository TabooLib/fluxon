package org.tabooproject.fluxon.parser.expression.literal;

import org.tabooproject.fluxon.parser.expression.ExpressionType;

/**
 * 浮点数字面量
 */
public class FloatLiteral extends Literal {
    private final float value;
    private final Float boxedValue;

    public FloatLiteral(float value) {
        super(ExpressionType.FLOAT_LITERAL);
        this.value = value;
        this.boxedValue = value;
    }

    public float getValue() {
        return value;
    }

    public Float getBoxedValue() {
        return boxedValue;
    }

    @Override
    public Object getSourceValue() {
        return boxedValue;
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
