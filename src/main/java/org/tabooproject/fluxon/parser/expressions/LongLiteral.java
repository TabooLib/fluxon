package org.tabooproject.fluxon.parser.expressions;

/**
 * 长整型字面量
 */
public class LongLiteral extends Literal {
    private final long value;

    public LongLiteral(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
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
