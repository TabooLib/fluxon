package org.tabooproject.fluxon.runtime.reflection.cache;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.util.List;

/**
 * 构造函数 MethodHandle 缓存
 * 使用 ClassValue 确保类卸载时自动清理缓存
 */
public final class ConstructorCache {

    /**
     * 使用 ClassValue 存储每个类的构造函数缓存
     * 当类被卸载时，对应的缓存会自动被 GC 回收
     */
    private static final ClassValue<ClassConstructorData> CLASS_DATA = new ClassValue<ClassConstructorData>() {
        @Override
        protected ClassConstructorData computeValue(Class<?> type) {
            return new ClassConstructorData(type);
        }
    };

    private ConstructorCache() {}

    /**
     * 获取构造函数的 ArityCache
     */
    public static ArityCache getArityCache(Class<?> clazz) {
        return CLASS_DATA.get(clazz).cache;
    }

    /**
     * 获取或创建构造函数的 ArityCache
     */
    public static ArityCache getOrCreateArityCache(Class<?> clazz) {
        return CLASS_DATA.get(clazz).cache;
    }

    /**
     * 获取构造函数索引
     */
    public static List<Constructor<?>> getConstructors(Class<?> clazz) {
        return CLASS_DATA.get(clazz).constructors;
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
    public static MethodHandle get(Class<?> clazz, int argCount, Class<?>[] argTypes) {
        return CLASS_DATA.get(clazz).cache.get(argCount, argTypes);
    }

    /**
     * 将 MethodHandle 放入缓存
     */
    public static void put(Class<?> clazz, int argCount, Class<?>[] argTypes, MethodHandle handle) {
        CLASS_DATA.get(clazz).cache.put(argCount, argTypes, handle);
    }
}
