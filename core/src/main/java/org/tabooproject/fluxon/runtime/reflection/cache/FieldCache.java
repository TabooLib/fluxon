package org.tabooproject.fluxon.runtime.reflection.cache;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段访问 MethodHandle 缓存
 * 使用 ClassValue 确保类卸载时自动清理缓存
 */
public final class FieldCache {

    /**
     * 使用 ClassValue 存储每个类的字段 getter 缓存
     * 当类被卸载时，对应的缓存会自动被 GC 回收
     */
    private static final ClassValue<ConcurrentHashMap<String, MethodHandle>> GETTER_CACHE = new ClassValue<ConcurrentHashMap<String, MethodHandle>>() {
        @Override
        protected ConcurrentHashMap<String, MethodHandle> computeValue(@NotNull Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    /**
     * setter 缓存
     */
    private static final ClassValue<ConcurrentHashMap<String, MethodHandle>> SETTER_CACHE = new ClassValue<ConcurrentHashMap<String, MethodHandle>>() {
        @Override
        protected ConcurrentHashMap<String, MethodHandle> computeValue(@NotNull Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    private FieldCache() {}

    /**
     * 获取缓存的字段 getter MethodHandle
     */
    public static MethodHandle get(Class<?> clazz, String fieldName) {
        return GETTER_CACHE.get(clazz).get(fieldName);
    }

    /**
     * 缓存字段 getter MethodHandle
     */
    public static void put(Class<?> clazz, String fieldName, MethodHandle handle) {
        GETTER_CACHE.get(clazz).put(fieldName, handle);
    }

    /**
     * 获取缓存的字段 setter MethodHandle
     */
    public static MethodHandle getSetter(Class<?> clazz, String fieldName) {
        return SETTER_CACHE.get(clazz).get(fieldName);
    }

    /**
     * 缓存字段 setter MethodHandle
     */
    public static void putSetter(Class<?> clazz, String fieldName, MethodHandle handle) {
        SETTER_CACHE.get(clazz).put(fieldName, handle);
    }

    /**
     * 获取类级别缓存（用于批量检查）
     */
    public static ConcurrentHashMap<String, MethodHandle> getClassCache(Class<?> clazz) {
        return GETTER_CACHE.get(clazz);
    }

    /**
     * 清除指定类的缓存
     */
    public static void remove(Class<?> clazz) {
        GETTER_CACHE.remove(clazz);
        SETTER_CACHE.remove(clazz);
    }
}
