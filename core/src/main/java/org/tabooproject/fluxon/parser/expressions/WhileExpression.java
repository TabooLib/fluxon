package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.statements.Block;

/**
 * While表达式
 */
public class WhileExpression implements Expression {
    private final ParseResult condition;
    private final ParseResult body;

    public WhileExpression(ParseResult condition, ParseResult body) {
        this.condition = condition;
        this.body = body;
    }

    public ParseResult getCondition() {
        return condition;
    }

    public ParseResult getBody() {
        return body;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.WHILE;
    }

    @Override
    public String toString() {
        return "While(" + condition + ", " + body + ")";
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("while ").append(condition.toPseudoCode()).append(" ");

        if (body instanceof Block) {
            sb.append(body.toPseudoCode());
        } else {
            sb.append("{ ").append(body.toPseudoCode()).append(" }");
        }

        return sb.toString();
    }
}
