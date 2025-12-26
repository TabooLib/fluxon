package org.tabooproject.fluxon.runtime.reflection.cache;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段访问 MethodHandle 缓存
 * Class -> fieldName -> MethodHandle
 */
public final class FieldCache {

    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, MethodHandle>> CACHE = new ConcurrentHashMap<>();

    private FieldCache() {}

    /**
     * 获取缓存的字段 MethodHandle
     */
    public static MethodHandle get(Class<?> clazz, String fieldName) {
        ConcurrentHashMap<String, MethodHandle> classCache = CACHE.get(clazz);
        if (classCache == null) return null;
        return classCache.get(fieldName);
    }

    /**
     * 缓存字段 MethodHandle
     */
    public static void put(Class<?> clazz, String fieldName, MethodHandle handle) {
        CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>()).put(fieldName, handle);
    }

    /**
     * 获取类级别缓存（用于批量检查）
     */
    public static ConcurrentHashMap<String, MethodHandle> getClassCache(Class<?> clazz) {
        return CACHE.get(clazz);
    }

    /**
     * 清除缓存
     */
    public static void clear() {
        CACHE.clear();
    }
}
