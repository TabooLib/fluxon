package org.tabooproject.fluxon.runtime.reflection;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按参数数量分组的方法缓存，避免在热路径创建 MethodKey 对象
 */
final class ArityCache {

    // 按参数数量分组：argCount -> (argTypes -> MethodHandle)
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<ArgTypesKey, MethodHandle>> byArity = new ConcurrentHashMap<>();

    MethodHandle get(int argCount, Class<?>[] argTypes) {
        ConcurrentHashMap<ArgTypesKey, MethodHandle> arityMap = byArity.get(argCount);
        if (arityMap == null) return null;
        return arityMap.get(new ArgTypesKey(argTypes));
    }

    void put(int argCount, Class<?>[] argTypes, MethodHandle handle) {
        byArity.computeIfAbsent(argCount, k -> new ConcurrentHashMap<>()).put(new ArgTypesKey(argTypes), handle);
    }
}
