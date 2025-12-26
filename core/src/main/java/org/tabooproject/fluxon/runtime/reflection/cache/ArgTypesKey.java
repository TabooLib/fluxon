package org.tabooproject.fluxon.runtime.reflection.cache;

import java.util.Arrays;

/**
 * 参数类型数组包装器（用于 HashMap 键）
 */
public final class ArgTypesKey {

    private final Class<?>[] types;
    private final int hash;

    public ArgTypesKey(Class<?>[] types) {
        this.types = types;
        this.hash = Arrays.hashCode(types);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArgTypesKey)) return false;
        return Arrays.equals(types, ((ArgTypesKey) o).types);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
