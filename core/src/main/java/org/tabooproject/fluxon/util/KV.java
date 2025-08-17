package org.tabooproject.fluxon.util;

public class KV<Key, Value> {

    private final Key key;
    private final Value value;

    public KV(Key key, Value value) {
        this.key = key;
        this.value = value;
    }

    public Key getKey() {
        return key;
    }

    public Value getValue() {
        return value;
    }
}
