package org.tabooproject.fluxon.parser.statement;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.Arrays;
import java.util.List;

/**
 * 代码块
 * 表示一组语句组成的代码块
 */
public class Block extends Statement {

    private final String label;
    private final ParseResult[] statements;

    public Block(String label, ParseResult[] statements) {
        super(StatementType.BLOCK);
        this.label = label;
        this.statements = statements;
    }

    public String getLabel() {
        return label;
    }

    public ParseResult[] getStatements() {
        return statements;
    }

    @Override
    public StatementType getStatementType() {
        return StatementType.BLOCK;
    }

    @Override
    public String toString() {
        return "Block(" + Arrays.toString(statements) + ")";
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        if (label != null) {
            sb.append("@").append(label).append(" ");
        }
        sb.append("{");
        for (ParseResult stmt : statements) {
            sb.append(stmt.toPseudoCode()).append(";");
        }
        sb.append("}");
        return sb.toString();
    }
}
