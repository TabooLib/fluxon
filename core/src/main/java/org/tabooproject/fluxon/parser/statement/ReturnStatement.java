package org.tabooproject.fluxon.parser.statement;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 返回语句
 * 表示函数的返回语句
 */
public class ReturnStatement extends Statement {
    private final ParseResult value;

    public ReturnStatement(ParseResult value) {
        super(StatementType.RETURN);
        this.value = value;
    }

    public ParseResult getValue() {
        return value;
    }

    @Override
    public StatementType getStatementType() {
        return StatementType.RETURN;
    }

    @Override
    public String toString() {
        return "Return(" + (value != null ? value : "") + ")";
    }

    @Override
    public String toPseudoCode() {
        return "return" + (value != null ? " " + value.toPseudoCode() : "");
    }
}
