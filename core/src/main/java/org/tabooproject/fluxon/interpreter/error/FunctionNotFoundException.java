package org.tabooproject.fluxon.interpreter.error;

import org.jetbrains.annotations.Nullable;

/**
 * 没有找到函数异常
 */
public class FunctionNotFoundException extends RuntimeException {

    private final String name;
    private final Class<?> extensionClass;
    private final int index;

    public FunctionNotFoundException(String name) {
        super(name);
        this.name = name;
        this.extensionClass = null;
        this.index = -1;
    }

    public FunctionNotFoundException(String name, Class<?> extensionClass, int index) {
        super(name + ", context: " + extensionClass + ", index: " + index);
        this.name = name;
        this.extensionClass = extensionClass;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public Class<?> getExtensionClass() {
        return extensionClass;
    }

    public int getIndex() {
        return index;
    }
}
