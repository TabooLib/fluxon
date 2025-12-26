package org.tabooproject.fluxon.runtime.reflection;

import java.util.Objects;

/**
 * 字段缓存键
 */
class FieldKey {

    private final Class<?> clazz;
    private final String fieldName;
    private final int hash;

    FieldKey(Class<?> clazz, String fieldName) {
        this.clazz = clazz;
        this.fieldName = fieldName;
        this.hash = Objects.hash(clazz, fieldName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FieldKey)) return false;
        FieldKey fieldKey = (FieldKey) o;
        return clazz.equals(fieldKey.clazz) && fieldName.equals(fieldKey.fieldName);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
