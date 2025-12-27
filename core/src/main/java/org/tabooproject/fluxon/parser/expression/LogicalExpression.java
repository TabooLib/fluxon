package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 逻辑表达式
 */
public class LogicalExpression extends Expression {
    private final ParseResult left;
    private final Token operator;
    private final ParseResult right;

    public LogicalExpression(ParseResult left, Token operator, ParseResult right) {
        super(ExpressionType.LOGICAL);
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
        return ExpressionType.LOGICAL;
    }

    @Override
    public String toString() {
        return "Logical(" + left + " " + operator.getLexeme() + " " + right + ")";
    }

    @Override
    public String toPseudoCode() {
        return left.toPseudoCode() + " " + operator.getLexeme() + " " + right.toPseudoCode();
    }
}
