package org.tabooproject.fluxon.parser.statement;

/**
 * 继续语句
 * 表示继续循环
 */
public class ContinueStatement extends Statement {

    public ContinueStatement() {
        super(StatementType.CONTINUE);
    }

    @Override
    public StatementType getStatementType() {
        return StatementType.CONTINUE;
    }

    @Override
    public String toString() {
        return "Continue";
    }

    @Override
    public String toPseudoCode() {
        return "continue";
    }
}
