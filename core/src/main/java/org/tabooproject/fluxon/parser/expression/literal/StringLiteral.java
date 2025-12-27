package org.tabooproject.fluxon.parser.expression.literal;

import org.tabooproject.fluxon.parser.expression.ExpressionType;

/**
 * 字符串字面量
 */
public class StringLiteral extends Literal {
    private final String value;

    public StringLiteral(String value) {
        super(ExpressionType.STRING_LITERAL);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public Object getSourceValue() {
        return value;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.STRING_LITERAL;
    }

    @Override
    public String toString() {
        return "StringLiteral(" + value + ")";
    }

    @Override
    public String toPseudoCode() {
        return "\"" + value + "\"";
    }
}
