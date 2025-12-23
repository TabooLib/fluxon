package org.tabooproject.fluxon.parser;

/**
 * 赋值目标
 */
public class AssignmentTarget {

    private final ParseResult expression;
    private final int position;

    public AssignmentTarget(ParseResult expression, int position) {
        this.expression = expression;
        this.position = position;
    }

    public ParseResult expression() {
        return expression;
    }

    public int position() {
        return position;
    }
}
