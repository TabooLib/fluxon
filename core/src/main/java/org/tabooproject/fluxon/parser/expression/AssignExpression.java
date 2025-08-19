package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 赋值表达式
 */
public class AssignExpression implements Expression {

    private final String name;
    private final Token operator;
    private final ParseResult value;
    private final int position;

    public AssignExpression(String name, Token operator, ParseResult value, int position) {
        this.name = name;
        this.operator = operator;
        this.value = value;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public Token getOperator() {
        return operator;
    }

    public ParseResult getValue() {
        return value;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.ASSIGNMENT;
    }

    @Override
    public String toString() {
        return "Assignment(" + name + " " + operator.getLexeme() + " " + value + ", position: " + position + ")";
    }

    @Override
    public String toPseudoCode() {
        return name + " " + operator.getLexeme() + " " + value.toPseudoCode();
    }
}
