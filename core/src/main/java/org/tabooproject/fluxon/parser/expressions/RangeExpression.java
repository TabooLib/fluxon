package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 范围表达式
 */
public class RangeExpression implements Expression {
    private final ParseResult start;
    private final ParseResult end;
    private final boolean inclusive; // true表示包含上界，false表示不包含上界

    public RangeExpression(ParseResult start, ParseResult end, boolean inclusive) {
        this.start = start;
        this.end = end;
        this.inclusive = inclusive;
    }

    public ParseResult getStart() {
        return start;
    }

    public ParseResult getEnd() {
        return end;
    }

    public boolean isInclusive() {
        return inclusive;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.RANGE;
    }

    @Override
    public String toString() {
        return "Range(" + start + ", " + end + ", " + inclusive + ")";
    }

    @Override
    public String toPseudoCode() {
        return start.toPseudoCode() + (inclusive ? ".." : "..<") + end.toPseudoCode();
    }
}
