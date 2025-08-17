package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.runtime.Function;

import java.util.List;

public class FunctionPosition implements Callable {

    private final Function function;
    private final int index;

    public FunctionPosition(Function function, int index) {
        this.function = function;
        this.index = index;
    }

    @Override
    public List<Integer> getParameterCounts() {
        return function.getParameterCounts();
    }

    @Override
    public int getMaxParameterCount() {
        return function.getMaxParameterCount();
    }

    @Override
    public boolean supportsParameterCount(int count) {
        return function.getParameterCounts().contains(count);
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
