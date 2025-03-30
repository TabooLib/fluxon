package org.tabooproject.fluxon.parser.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @Nullable
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

    /**
     * When 表达式分支
     */
    public static class WhenBranch {
        private final ParseResult condition;
        private final ParseResult result;

        public WhenBranch(ParseResult condition, ParseResult result) {
            this.condition = condition;
            this.result = result;
        }

        @Nullable
        public ParseResult getCondition() {
            return condition;
        }

        @NotNull
        public ParseResult getResult() {
            return result;
        }

        @Override
        public String toString() {
            return (condition != null ? condition : "else") + " -> " + result;
        }

        public String toPseudoCode() {
            String condStr = condition != null ? condition.toPseudoCode() : "else";
            return condStr + " -> " + result.toPseudoCode();
        }
    }
}
