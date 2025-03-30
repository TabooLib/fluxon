package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.statement.Block;

/**
 * If 表达式
 */
public class IfExpression implements Expression {
    private final ParseResult condition;
    private final ParseResult thenBranch;
    private final ParseResult elseBranch;

    public IfExpression(ParseResult condition, ParseResult thenBranch, ParseResult elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    public ParseResult getCondition() {
        return condition;
    }

    public ParseResult getThenBranch() {
        return thenBranch;
    }

    public ParseResult getElseBranch() {
        return elseBranch;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.IF;
    }

    @Override
    public String toString() {
        return "If(" + condition + ", " + thenBranch + ", " + elseBranch + ")";
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("if ").append(condition.toPseudoCode()).append(" ");

        // 处理 then 分支
        if (thenBranch instanceof Block) {
            sb.append(thenBranch.toPseudoCode());
        } else {
            sb.append("{ ").append(thenBranch.toPseudoCode()).append(" }");
        }

        // 处理 else 分支
        if (elseBranch != null) {
            sb.append(" else ");
            if (elseBranch instanceof Block || elseBranch instanceof IfExpression) {
                sb.append(elseBranch.toPseudoCode());
            } else {
                sb.append("{ ").append(elseBranch.toPseudoCode()).append(" }");
            }
        }

        return sb.toString();
    }
}
