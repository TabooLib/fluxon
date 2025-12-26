package org.tabooproject.fluxon.runtime.reflection.cache;

import org.tabooproject.fluxon.runtime.reflection.resolve.MethodResolver;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 方法 MethodHandle 缓存
 * 包含方法索引和按 arity 分组的 MethodHandle 缓存
 */
public final class MethodCache {

    // 方法缓存：Class -> methodName -> ArityCache
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, ArityCache>> CACHE = new ConcurrentHashMap<>();

    // 方法索引（按方法名快速查找所有重载）
    private static final ConcurrentHashMap<Class<?>, Map<String, List<Method>>> INDEX = new ConcurrentHashMap<>();

    private MethodCache() {}

    /**
     * 获取方法的 ArityCache
     */
    public static ArityCache getArityCache(Class<?> clazz, String methodName) {
        ConcurrentHashMap<String, ArityCache> classCache = CACHE.get(clazz);
        if (classCache == null) return null;
        return classCache.get(methodName);
    }

    /**
     * 获取或创建方法的 ArityCache
     */
    public static ArityCache getOrCreateArityCache(Class<?> clazz, String methodName) {
        return CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>()).computeIfAbsent(methodName, k -> new ArityCache());
    }

    /**
     * 获取方法索引
     */
    public static Map<String, List<Method>> getMethodIndex(Class<?> clazz) {
        return INDEX.computeIfAbsent(clazz, MethodResolver::buildMethodIndex);
    }

    /**
     * 获取指定方法名的所有重载
     */
    public static List<Method> getMethods(Class<?> clazz, String methodName) {
        return getMethodIndex(clazz).getOrDefault(methodName, Collections.emptyList());
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
    public static MethodHandle get(Class<?> clazz, String methodName, int argCount, Class<?>[] argTypes) {
        ArityCache arityCache = getArityCache(clazz, methodName);
        if (arityCache == null) return null;
        return arityCache.get(argCount, argTypes);
    }

    /**
     * 将 MethodHandle 放入缓存
     */
    public static void put(Class<?> clazz, String methodName, int argCount, Class<?>[] argTypes, MethodHandle handle) {
        getOrCreateArityCache(clazz, methodName).put(argCount, argTypes, handle);
    }
}
