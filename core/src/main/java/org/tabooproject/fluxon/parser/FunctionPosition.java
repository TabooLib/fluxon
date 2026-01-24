package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.runtime.Function;

public class FunctionPosition implements Callable {

    private final Function function;
    private final int index;

    public FunctionPosition(Function function, int index) {
        this.function = function;
        this.index = index;
    }

    @Override
    public int getParameterCount() {
        return function.getParameterCount();
    }

    public Function getFunction() {
        return function;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "FunctionPosition{" +
                "function=" + function +
                ", index=" + index +
                '}';
    }
}
