package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.runtime.Function;

import java.util.Map;

public class ExtensionFunctionPosition {

    private final Map<Class<?>, Function> functions;
    private final int index;

    public ExtensionFunctionPosition(Map<Class<?>, Function> functions, int index) {
        this.functions = functions;
        this.index = index;
    }

    public Map<Class<?>, Function> getFunctions() {
        return functions;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "ExtensionFunctionPosition{" +
                "functions=" + functions +
                ", index=" + index +
                '}';
    }
}
