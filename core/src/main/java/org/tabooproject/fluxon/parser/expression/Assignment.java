package org.tabooproject.fluxon.parser.expression;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.VariablePosition;

/**
 * 赋值表达式
 */
public class Assignment implements Expression {

    private final String name;
    private final Token operator;
    private final ParseResult value;

    @Nullable
    private final VariablePosition position;

    public Assignment(String name, Token operator, ParseResult value, @Nullable VariablePosition position) {
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

    @Nullable
    public VariablePosition getPosition() {
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
