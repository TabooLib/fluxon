package org.tabooproject.fluxon.runtime.reflection;

import java.util.Arrays;
import java.util.Objects;

/**
 * 方法缓存键
 */
class MethodKey {

    private final Class<?> clazz;
    private final String methodName;
    private final Class<?>[] paramTypes;
    private final int hash;

    MethodKey(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        this.clazz = clazz;
        this.methodName = methodName;
        this.paramTypes = paramTypes;
        this.hash = Objects.hash(clazz, methodName, Arrays.hashCode(paramTypes));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodKey)) return false;
        MethodKey methodKey = (MethodKey) o;
        return clazz.equals(methodKey.clazz) && methodName.equals(methodKey.methodName) && Arrays.equals(paramTypes, methodKey.paramTypes);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
