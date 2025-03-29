package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 字典条目
 */
public class MapEntry {
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
