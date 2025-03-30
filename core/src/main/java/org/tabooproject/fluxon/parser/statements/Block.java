package org.tabooproject.fluxon.parser.statements;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.List;

/**
 * 代码块
 * 表示一组语句组成的代码块
 */
public class Block implements Statement {

    private final String label;
    private final List<ParseResult> statements;

    public Block(String label, List<ParseResult> statements) {
        this.label = label;
        this.statements = statements;
    }

    public String getLabel() {
        return label;
    }

    public List<ParseResult> getStatements() {
        return statements;
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
