package org.tabooproject.fluxon.parser.expression.literal;

import org.tabooproject.fluxon.parser.expression.ExpressionType;

public class NullLiteral extends Literal {

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.NULL;
    }

    @Override
    public String toPseudoCode() {
        return "null";
    }

    @Override
    public Object getSourceValue() {
        return null;
    }
}
