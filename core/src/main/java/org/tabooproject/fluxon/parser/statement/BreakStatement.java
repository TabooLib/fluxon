package org.tabooproject.fluxon.parser.statement;

/**
 * 跳出语句
 * 表示跳出循环
 */
public class BreakStatement extends Statement {

    public BreakStatement() {
        super(StatementType.BREAK);
    }

    @Override
    public StatementType getStatementType() {
        return StatementType.BREAK;
    }

    @Override
    public String toString() {
        return "Break";
    }

    @Override
    public String toPseudoCode() {
        return "break";
    }
}
