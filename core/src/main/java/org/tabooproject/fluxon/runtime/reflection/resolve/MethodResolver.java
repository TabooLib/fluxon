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
     * 单次遍历同时检查精确匹配和兼容匹配
     */
    public static Method findBestMethod(Class<?> targetClass, String methodName, Class<?>[] argTypes) {
        Method compatible = null;
        for (Method method : targetClass.getMethods()) {
            if (!method.getName().equals(methodName)) continue;
            Class<?>[] paramTypes = method.getParameterTypes();
            // 精确匹配直接返回
            if (Arrays.equals(paramTypes, argTypes)) {
                return method;
            }
            // 记录第一个兼容匹配作为备选
            if (compatible == null && TypeCompatibility.isParametersCompatible(paramTypes, argTypes)) {
                compatible = method;
            }
        }
        return compatible;
    }

    /**
     * 尝试创建直接方法调用的 MethodHandle
     */
    public static MethodHandle tryCreateSpecializedMethodHandle(Class<?> targetClass, String methodName, Object[] args, MethodType callSiteType) {
        try {
            // 使用与 ReflectionHelper.invokeMethod 一致的类型提取方式
            Class<?>[] argTypes = TypeCompatibility.getArgTypes(args);
            // 尝试精确匹配
            Method method = findBestMethod(targetClass, methodName, argTypes);
            if (method == null) {
                return null;
            }
            // 尝试创建 MethodHandle，优先使用 publicLookup
            MethodHandle mh;
            try {
                mh = LOOKUP.unreflect(method);
            } catch (IllegalAccessException e) {
                // 方法不可公开访问，尝试使用 setAccessible
                try {
                    method.setAccessible(true);
                    mh = MethodHandles.lookup().unreflect(method);
                } catch (SecurityException | IllegalAccessException ex) {
                    // SecurityManager 阻止或模块系统限制，回退返回 null
                    return null;
                }
            }
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
