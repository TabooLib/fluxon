package org.tabooproject.fluxon.parser.expression;

public class NullLiteral extends Literal {

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.NULL;
    }

    @Override
    public String toPseudoCode() {
        return "null";
    }
}
