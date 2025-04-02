package org.tabooproject.fluxon.parser.expression.literal;

import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;

/**
 * 标识符
 */
public class Identifier implements Expression {
    private final String name;

    public Identifier(String name) {
        this.name = name;
    }

    public String getValue() {
        return name;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.IDENTIFIER;
    }

    @Override
    public String toString() {
        return "Identifier(" + name + ")";
    }

    @Override
    public String toPseudoCode() {
        return name;
    }
}
