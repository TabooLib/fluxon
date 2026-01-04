package org.tabooproject.fluxon.parser.expression;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.ParseResult;

import java.util.List;

/**
 * When 表达式
 */
public class WhenExpression extends Expression {
    private final ParseResult subject;
    private final List<WhenBranch> branches;

    public WhenExpression(ParseResult subject, List<WhenBranch> branches) {
        super(ExpressionType.WHEN);
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
     * When 表达式分支匹配方式
     */
    public enum MatchType {

        // 等值
        EQUAL(""),

        // 包含
        CONTAINS("in "),

        // 不包含
        NOT_CONTAINS("!in "),

        // 类型检查
        IS("is "),
        ;

        final String pseudoCode;

        MatchType(String pseudoCode) {
            this.pseudoCode = pseudoCode;
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    /**
     * When 表达式分支
     */
    public static class WhenBranch {

        private final MatchType matchType;
        private final ParseResult condition;
        private final ParseResult result;
        private final Class<?> targetClass; // 用于 IS 类型匹配

        public WhenBranch(MatchType matchType, ParseResult condition, ParseResult result) {
            this(matchType, condition, result, null);
        }

        public WhenBranch(MatchType matchType, ParseResult condition, ParseResult result, Class<?> targetClass) {
            this.matchType = matchType;
            this.condition = condition;
            this.result = result;
            this.targetClass = targetClass;
        }

        @NotNull
        public MatchType getMatchType() {
            return matchType;
        }

        @Nullable
        public ParseResult getCondition() {
            return condition;
        }

        @NotNull
        public ParseResult getResult() {
            return result;
        }

        @Nullable
        public Class<?> getTargetClass() {
            return targetClass;
        }

        @Override
        public String toString() {
            return (condition != null ? condition : "else") + " -> " + result;
        }

        public String toPseudoCode() {
            String condStr = condition != null ? condition.toPseudoCode() : "else";
            return matchType + condStr + " -> " + result.toPseudoCode();
        }
    }
}
