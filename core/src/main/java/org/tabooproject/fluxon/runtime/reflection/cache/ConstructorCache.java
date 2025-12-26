package org.tabooproject.fluxon.runtime.reflection.cache;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 构造函数 MethodHandle 缓存
 */
public final class ConstructorCache {

    // 构造函数缓存：Class -> ArityCache
    private static final ConcurrentHashMap<Class<?>, ArityCache> CACHE = new ConcurrentHashMap<>();

    // 构造函数索引：Class -> List<Constructor<?>>
    private static final ConcurrentHashMap<Class<?>, List<Constructor<?>>> INDEX = new ConcurrentHashMap<>();

    private ConstructorCache() {}

    /**
     * 获取构造函数的 ArityCache
     */
    public static ArityCache getArityCache(Class<?> clazz) {
        return CACHE.get(clazz);
    }

    /**
     * 获取或创建构造函数的 ArityCache
     */
    public static ArityCache getOrCreateArityCache(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, k -> new ArityCache());
    }

    /**
     * 获取构造函数索引
     */
    public static List<Constructor<?>> getConstructors(Class<?> clazz) {
        return INDEX.computeIfAbsent(clazz, ConstructorCache::buildConstructorIndex);
    }

    /**
     * 构建类的构造函数索引
     */
    private static List<Constructor<?>> buildConstructorIndex(Class<?> clazz) {
        return new ArrayList<>(Arrays.asList(clazz.getConstructors()));
    }

    /**
     * 清除缓存
     */
    public static void clear() {
        CACHE.clear();
        INDEX.clear();
    }

    /**
     * 从缓存中获取 MethodHandle
     */
    public static java.lang.invoke.MethodHandle get(Class<?> clazz, int argCount, Class<?>[] argTypes) {
        ArityCache arityCache = getArityCache(clazz);
        if (arityCache == null) return null;
        return arityCache.get(argCount, argTypes);
    }

    /**
     * 将 MethodHandle 放入缓存
     */
    public static void put(Class<?> clazz, int argCount, Class<?>[] argTypes, java.lang.invoke.MethodHandle handle) {
        getOrCreateArityCache(clazz).put(argCount, argTypes, handle);
    }
}
