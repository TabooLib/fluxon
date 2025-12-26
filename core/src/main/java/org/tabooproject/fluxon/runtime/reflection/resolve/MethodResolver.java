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
     * 单次遍历同时检查精确匹配和收集兼容匹配，最后选择最具体的
     */
    public static Method findBestMethod(Class<?> targetClass, String methodName, Class<?>[] argTypes) {
        List<Method> compatibleCandidates = null;
        for (Method method : targetClass.getMethods()) {
            if (!method.getName().equals(methodName)) continue;
            Class<?>[] paramTypes = method.getParameterTypes();
            // 精确匹配直接返回
            if (Arrays.equals(paramTypes, argTypes)) {
                return method;
            }
            // 收集所有兼容匹配
            if (TypeCompatibility.isParametersCompatible(paramTypes, argTypes)) {
                if (compatibleCandidates == null) {
                    compatibleCandidates = new ArrayList<>(4);
                }
                compatibleCandidates.add(method);
            }
        }
        // 从兼容候选中选择最具体的
        if (compatibleCandidates != null) {
            return TypeCompatibility.findBestMatch(compatibleCandidates, argTypes);
        }
        return null;
    }

    /**
     * 尝试创建直接方法调用的 MethodHandle
     * <p>
     * 以下情况返回 null，走 fallback 路径：
     * <ul>
     *   <li>参数包含 null（无法确定具体类型，需要 findBestMatch 选择最具体的重载）</li>
     *   <li>匹配到 varargs 方法（需要 VarargsHandler 进行参数打包）</li>
     * </ul>
     */
    public static MethodHandle tryCreateSpecializedMethodHandle(Class<?> targetClass, String methodName, Object[] args, MethodType callSiteType) {
        try {
            Class<?>[] argTypes = TypeCompatibility.tryGetArgTypes(args);
            if (argTypes == null) {
                return null;
            }
            Method method = findBestMethod(targetClass, methodName, argTypes);
            if (method == null || method.isVarArgs()) {
                return null;
            }
            MethodHandle mh;
            try {
                mh = LOOKUP.unreflect(method);
            } catch (IllegalAccessException e) {
                try {
                    method.setAccessible(true);
                    mh = MethodHandles.lookup().unreflect(method);
                } catch (SecurityException | IllegalAccessException ex) {
                    return null;
                }
            }
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
