package org.tabooproject.fluxon.compiler;

import org.tabooproject.fluxon.runtime.Type;

/**
 * 参数信息
 */
public class ParameterInfo {

    private final String name;
    private final Type type;
    private final int index;

    public ParameterInfo(String name, Type type, int index) {
        this.name = name;
        this.type = type;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }
}
