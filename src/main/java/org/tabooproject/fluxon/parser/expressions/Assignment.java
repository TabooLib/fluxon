package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 赋值表达式
 */
public class Assignment implements Expression {
    private final String name;
    private final Token operator;
    private final ParseResult value;

    public Assignment(String name, Token operator, ParseResult value) {
        this.name = name;
        this.operator = operator;
        this.value = value;
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

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.ASSIGNMENT;
    }

    @Override
    public String toString() {
        return "Assignment(" + name + " " + operator.getLexeme() + " " + value + ")";
    }

    @Override
    public String toPseudoCode() {
        return name + " " + operator.getLexeme() + " " + value.toPseudoCode();
    }
}
