package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * When 表达式分支
 */
public class WhenBranch {
    private final ParseResult condition;
    private final ParseResult result;

    public WhenBranch(ParseResult condition, ParseResult result) {
        this.condition = condition;
        this.result = result;
    }

    public ParseResult getCondition() {
        return condition;
    }

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
