package org.tabooproject.fluxon.parser.expression.literal;

import org.tabooproject.fluxon.parser.expression.ExpressionType;

/**
 * 长整型字面量
 */
public class LongLiteral extends Literal {
    private final long value;
    private final Long boxedValue;

    public LongLiteral(long value) {
        super(ExpressionType.LONG_LITERAL);
        this.value = value;
        this.boxedValue = value;
    }

    public long getValue() {
        return value;
    }

    public Long getBoxedValue() {
        return boxedValue;
    }

    @Override
    public Object getSourceValue() {
        return boxedValue;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.LONG_LITERAL;
    }

    @Override
    public String toString() {
        return "LongLiteral(" + value + ")";
    }

    @Override
    public String toPseudoCode() {
        return value + "L";
    }
}
