package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 二元表达式
 */
public class BinaryExpression extends Expression {
    private final ParseResult left;
    private final Token operator;
    private final ParseResult right;

    public BinaryExpression(ParseResult left, Token operator, ParseResult right) {
        super(ExpressionType.BINARY);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public ParseResult getLeft() {
        return left;
    }

    public Token getOperator() {
        return operator;
    }

    public ParseResult getRight() {
        return right;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.BINARY;
    }

    @Override
    public String toString() {
        return "Binary(" + left + " " + operator.getLexeme() + " " + right + ")";
    }

    @Override
    public String toPseudoCode() {
        return left.toPseudoCode() + " " + operator.getLexeme() + " " + right.toPseudoCode();
    }
}
