package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.runtime.OverloadSet;

import java.util.Map;

public class ExtensionFunctionPosition {

    private final Map<Class<?>, OverloadSet> overloadSets;
    private final int index;

    public ExtensionFunctionPosition(Map<Class<?>, OverloadSet> overloadSets, int index) {
        this.overloadSets = overloadSets;
        this.index = index;
    }

    public Map<Class<?>, OverloadSet> getOverloadSets() {
        return overloadSets;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "ExtensionFunctionPosition{" +
                "overloadSets=" + overloadSets +
                ", index=" + index +
                '}';
    }
}
