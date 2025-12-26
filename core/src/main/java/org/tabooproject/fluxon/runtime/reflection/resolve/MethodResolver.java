package org.tabooproject.fluxon.runtime.reflection.resolve;

import org.tabooproject.fluxon.runtime.error.MemberNotFoundError;
import org.tabooproject.fluxon.runtime.reflection.ReflectionHelper;
import org.tabooproject.fluxon.runtime.reflection.cache.MethodCache;
import org.tabooproject.fluxon.runtime.reflection.util.TypeCompatibility;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 方法解析器
 * 负责查找和匹配方法
 */
public final class MethodResolver {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    private MethodResolver() {}

    /**
     * 构建类的方法索引（延迟初始化）
     */
    public static Map<String, List<Method>> buildMethodIndex(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        Map<String, List<Method>> index = new HashMap<>(methods.length);
        for (Method method : methods) {
            index.computeIfAbsent(method.getName(), k -> new ArrayList<>(4)).add(method);
        }
        return index;
    }

    /**
     * 匹配最佳方法（支持 varargs）
     */
    public static Method findBestMatch(List<Method> candidates, Class<?>[] argTypes) {
        Method result = TypeCompatibility.findBestMatch(candidates, argTypes);
        if (result == null) {
            throw new MemberNotFoundError(candidates.get(0).getDeclaringClass(), candidates.get(0).getName(), argTypes);
        }
        return result;
    }

    /**
     * 查找最佳匹配的方法（用于 Bootstrap）
     */
    public static Method findBestMethod(Class<?> targetClass, String methodName, Class<?>[] argTypes) {
        // 首先尝试精确匹配
        try {
            return targetClass.getMethod(methodName, argTypes);
        } catch (NoSuchMethodException ignored) {
        }
        // 尝试找到兼容的方法
        for (Method method : targetClass.getMethods()) {
            if (!method.getName().equals(methodName)) continue;
            if (TypeCompatibility.isParametersCompatible(method.getParameterTypes(), argTypes)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 尝试创建直接方法调用的 MethodHandle
     */
    public static MethodHandle tryCreateSpecializedMethodHandle(Class<?> targetClass, String methodName, Object[] args, MethodType callSiteType) {
        try {
            // 根据参数类型查找方法
            Class<?>[] argTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
            // 尝试精确匹配
            Method method = findBestMethod(targetClass, methodName, argTypes);
            if (method == null) {
                return null;
            }
            // 确保方法可访问（处理嵌套类等情况）
            method.setAccessible(true);
            MethodHandle mh = MethodHandles.lookup().unreflect(method);
            // 转换为接受 Object[] 参数的形式
            mh = mh.asSpreader(Object[].class, args.length);
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
     * 查找最佳匹配方法（供 ReflectionHelper 调用）
     */
    public static Method findBestMatch(Class<?> clazz, String methodName, Class<?>[] argTypes) {
        List<Method> candidates = MethodCache.getMethods(clazz, methodName);
        if (candidates.isEmpty()) {
            throw new MemberNotFoundError(clazz, methodName, argTypes);
        }
        return findBestMatch(candidates, argTypes);
    }
}
