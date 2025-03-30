package org.tabooproject.fluxon.parser.expressions;

public class NullLiteral extends Literal {

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.NULL_LITERAL;
    }

    @Override
    public String toPseudoCode() {
        return "null";
    }
}
