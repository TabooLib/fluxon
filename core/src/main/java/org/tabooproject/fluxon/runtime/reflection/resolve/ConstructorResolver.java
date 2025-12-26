package org.tabooproject.fluxon.runtime.reflection.resolve;

import org.tabooproject.fluxon.runtime.error.MemberAccessError;
import org.tabooproject.fluxon.runtime.reflection.cache.ConstructorCache;
import org.tabooproject.fluxon.runtime.reflection.util.TypeCompatibility;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 构造函数解析器
 * 负责查找和匹配构造函数
 */
public final class ConstructorResolver {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    private ConstructorResolver() {}

    /**
     * 构建类的构造函数索引
     */
    public static List<Constructor<?>> buildConstructorIndex(Class<?> clazz) {
        return new ArrayList<>(Arrays.asList(clazz.getConstructors()));
    }

    /**
     * 匹配最佳构造函数（支持 varargs）
     */
    public static Constructor<?> findBestConstructorMatch(List<Constructor<?>> candidates, Class<?>[] argTypes) {
        Constructor<?> result = TypeCompatibility.findBestMatch(candidates, argTypes);
        if (result == null) {
            throw new MemberAccessError("No matching constructor found for argument types: " + Arrays.toString(argTypes));
        }
        return result;
    }

    /**
     * 查找最佳匹配的构造函数
     */
    public static Constructor<?> findBestConstructor(Class<?> clazz, Class<?>[] argTypes) {
        // 首先尝试精确匹配
        try {
            return clazz.getConstructor(argTypes);
        } catch (NoSuchMethodException ignored) {
        }
        // 使用 TypeCompatibility.findBestMatch 选择最具体的兼容构造函数
        List<Constructor<?>> candidates = Arrays.asList(clazz.getConstructors());
        return TypeCompatibility.findBestMatch(candidates, argTypes);
    }

    /**
     * 尝试创建直接构造函数调用的 MethodHandle
     */
    public static MethodHandle tryCreateSpecializedConstructorHandle(Class<?> clazz, Object[] args, MethodType callSiteType) {
        try {
            // 根据参数类型查找构造函数
            Class<?>[] argTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
            // 尝试找到匹配的构造函数
            Constructor<?> constructor = findBestConstructor(clazz, argTypes);
            if (constructor == null) {
                return null;
            }
            MethodHandle mh = LOOKUP.unreflectConstructor(constructor);
            // 转换为接受 Object[] 参数的形式
            mh = mh.asSpreader(Object[].class, args.length);
            // 添加一个被忽略的 className 参数使签名变为 (String, Object[])Object
            mh = MethodHandles.dropArguments(mh, 0, String.class);
            return mh.asType(callSiteType);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 Lookup 实例
     */
    public static MethodHandles.Lookup getLookup() {
        return LOOKUP;
    }

    /**
     * 查找最佳匹配构造函数（供 ReflectionHelper 调用）
     */
    public static Constructor<?> findBestMatch(Class<?> clazz, Class<?>[] argTypes) {
        List<Constructor<?>> candidates = ConstructorCache.getConstructors(clazz);
        if (candidates.isEmpty()) {
            throw new MemberAccessError("No public constructor found for class: " + clazz.getName());
        }
        return findBestConstructorMatch(candidates, argTypes);
    }
}
