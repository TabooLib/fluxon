package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.List;

/**
 * When 表达式
 */
public class WhenExpression implements Expression {
    private final ParseResult subject;
    private final List<WhenBranch> branches;

    public WhenExpression(ParseResult subject, List<WhenBranch> branches) {
        this.subject = subject;
        this.branches = branches;
    }

    public ParseResult getSubject() {
        return subject;
    }

    public List<WhenBranch> getBranches() {
        return branches;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.WHEN;
    }

    @Override
    public String toString() {
        return "When(" + (subject != null ? subject : "") + ", " + branches + ")";
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();

        sb.append("when ");

        if (subject != null) {
            sb.append(subject.toPseudoCode());
        }

        sb.append("{");
        for (WhenBranch branch : branches) {
            sb.append(branch.toPseudoCode()).append(";");
        }
        sb.append("}");
        return sb.toString();
    }
}
