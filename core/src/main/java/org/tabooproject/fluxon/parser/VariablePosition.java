package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.runtime.Type;

public class VariablePosition {

    public static final Type TYPE = new Type(VariablePosition.class);

    private final int level;
    private final int index;

    public VariablePosition(int level, int index) {
        this.level = level;
        this.index = index;
    }

    public int getLevel() {
        return level;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "VariablePosition{" +
                "level=" + level +
                ", index=" + index +
                '}';
    }
}
