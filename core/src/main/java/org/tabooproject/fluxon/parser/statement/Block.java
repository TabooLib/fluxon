package org.tabooproject.fluxon.parser.statement;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.List;

/**
 * 代码块
 * 表示一组语句组成的代码块
 */
public class Block implements Statement {

    private final String label;
    private final ParseResult[] statements;
    private final int localVariables;

    public Block(String label, ParseResult[] statements, int localVariables) {
        this.label = label;
        this.statements = statements;
        this.localVariables = localVariables;
    }

    public String getLabel() {
        return label;
    }

    public ParseResult[] getStatements() {
        return statements;
    }

    public int getLocalVariables() {
        return localVariables;
    }

    @Override
    public StatementType getStatementType() {
        return StatementType.BLOCK;
    }

    @Override
    public String toString() {
        return "Block(" + statements + ")";
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
