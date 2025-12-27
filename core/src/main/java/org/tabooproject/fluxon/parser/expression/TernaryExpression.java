package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 三元运算符表达式
 * condition ? true_expr : false_expr
 */
public class TernaryExpression extends Expression {

    private final ParseResult condition;
    private final ParseResult trueExpr;
    private final ParseResult falseExpr;

    public TernaryExpression(ParseResult condition, ParseResult trueExpr, ParseResult falseExpr) {
        super(ExpressionType.TERNARY);
        this.condition = condition;
        this.trueExpr = trueExpr;
        this.falseExpr = falseExpr;
    }

    public ParseResult getCondition() {
        return condition;
    }

    public ParseResult getTrueExpr() {
        return trueExpr;
    }

    public ParseResult getFalseExpr() {
        return falseExpr;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.TERNARY;
    }

    @Override
    public String toString() {
        return "Ternary(" + condition + " ? " + trueExpr + " : " + falseExpr + ")";
    }

    @Override
    public String toPseudoCode() {
        return condition.toPseudoCode() + " ? " + trueExpr.toPseudoCode() + " : " + falseExpr.toPseudoCode();
    }
}