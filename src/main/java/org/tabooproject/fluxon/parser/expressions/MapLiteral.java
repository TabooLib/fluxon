package org.tabooproject.fluxon.parser.expressions;

import java.util.List;

/**
 * 字典字面量
 */
public class MapLiteral implements Expression {
    private final List<MapEntry> entries;

    public MapLiteral(List<MapEntry> entries) {
        this.entries = entries;
    }

    public List<MapEntry> getEntries() {
        return entries;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.MAP_LITERAL;
    }

    @Override
    public String toString() {
        return "MapLiteral(" + entries + ")";
    }

    @Override
    public String toPseudoCode() {
        if (entries.isEmpty()) {
            return "[:]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(entries.get(i).toPseudoCode());
        }

        sb.append("]");
        return sb.toString();
    }
}
