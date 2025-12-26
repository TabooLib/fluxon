package org.tabooproject.fluxon.runtime.reflection.cache;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 方法 MethodHandle 缓存
 * 包含方法索引和按 arity 分组的 MethodHandle 缓存
 * 使用 ClassValue 确保类卸载时自动清理缓存
 */
public final class MethodCache {

    /**
     * 使用 ClassValue 存储每个类的方法缓存
     * 当类被卸载时，对应的缓存会自动被 GC 回收
     */
    private static final ClassValue<ClassMethodData> CLASS_DATA = new ClassValue<ClassMethodData>() {
        @Override
        protected ClassMethodData computeValue(@NotNull Class<?> type) {
            return new ClassMethodData(type);
        }
    };

    private MethodCache() {}

    /**
     * 获取方法的 ArityCache
     */
    public static ArityCache getArityCache(Class<?> clazz, String methodName) {
        return CLASS_DATA.get(clazz).cache.get(methodName);
    }

    /**
     * 获取或创建方法的 ArityCache
     */
    public static ArityCache getOrCreateArityCache(Class<?> clazz, String methodName) {
        return CLASS_DATA.get(clazz).cache.computeIfAbsent(methodName, k -> new ArityCache());
    }

    /**
     * 获取方法索引
     */
    public static Map<String, List<Method>> getMethodIndex(Class<?> clazz) {
        return CLASS_DATA.get(clazz).index;
    }

    /**
     * 获取指定方法名的所有重载
     */
    public static List<Method> getMethods(Class<?> clazz, String methodName) {
        return getMethodIndex(clazz).getOrDefault(methodName, Collections.emptyList());
    }

    /**
     * 清除指定类的缓存
     */
    public static void remove(Class<?> clazz) {
        CLASS_DATA.remove(clazz);
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
