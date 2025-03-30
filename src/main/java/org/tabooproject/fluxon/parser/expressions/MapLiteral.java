package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.parser.ParseResult;

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

    /**
     * 字典条目
     */
    public static class MapEntry {
        private final ParseResult key;
        private final ParseResult value;

        public MapEntry(ParseResult key, ParseResult value) {
            this.key = key;
            this.value = value;
        }

        public ParseResult getKey() {
            return key;
        }

        public ParseResult getValue() {
            return value;
        }

        @Override
        public String toString() {
            return key + ": " + value;
        }

        public String toPseudoCode() {
            return key.toPseudoCode() + ": " + value.toPseudoCode();
        }
    }
}
