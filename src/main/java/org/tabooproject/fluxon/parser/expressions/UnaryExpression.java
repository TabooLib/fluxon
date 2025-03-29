package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 一元表达式
 */
public class UnaryExpression implements Expression {
    private final Token operator;
    private final ParseResult right;

    public UnaryExpression(Token operator, ParseResult right) {
        this.operator = operator;
        this.right = right;
    }

    public Token getOperator() {
        return operator;
    }

    public ParseResult getRight() {
        return right;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.UNARY;
    }

    @Override
    public String toString() {
        return "Unary(" + operator.getLexeme() + ", " + right + ")";
    }

    @Override
    public String toPseudoCode() {
        return operator.getLexeme() + right.toPseudoCode();
    }
}
