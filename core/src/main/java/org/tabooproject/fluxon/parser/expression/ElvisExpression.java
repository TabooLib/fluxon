package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * Elvis操作符表达式
 */
public class ElvisExpression extends Expression {
    private final ParseResult condition;
    private final ParseResult alternative;

    public ElvisExpression(ParseResult condition, ParseResult alternative) {
        super(ExpressionType.ELVIS);
        this.condition = condition;
        this.alternative = alternative;
    }

    public ParseResult getCondition() {
        return condition;
    }

    public ParseResult getAlternative() {
        return alternative;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.ELVIS;
    }

    @Override
    public String toString() {
        return "Elvis(" + condition + ", " + alternative + ")";
    }

    @Override
    public String toPseudoCode() {
        return condition.toPseudoCode() + " ?: " + alternative.toPseudoCode();
    }
}
