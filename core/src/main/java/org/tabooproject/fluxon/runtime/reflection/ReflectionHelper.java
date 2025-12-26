package org.tabooproject.fluxon.runtime.reflection;

import org.tabooproject.fluxon.runtime.reflection.cache.ConstructorCache;
import org.tabooproject.fluxon.runtime.reflection.cache.FieldCache;
import org.tabooproject.fluxon.runtime.reflection.cache.MethodCache;
import org.tabooproject.fluxon.runtime.reflection.resolve.ConstructorResolver;
import org.tabooproject.fluxon.runtime.reflection.resolve.FieldResolver;
import org.tabooproject.fluxon.runtime.reflection.resolve.MethodResolver;
import org.tabooproject.fluxon.runtime.reflection.util.TypeCompatibility;
import org.tabooproject.fluxon.runtime.reflection.util.VarargsHandler;
import org.tabooproject.fluxon.runtime.error.MemberAccessError;
import org.tabooproject.fluxon.runtime.error.MemberNotFoundError;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;

/**
 * 反射缓存门面类
 * 使用 MethodHandle 缓存反射访问，提供高性能的成员访问能力
 * <p>
 * 实际实现委托给具体的类：
 * - {@link FieldCache}, {@link MethodCache}, {@link ConstructorCache} 处理缓存
 * - {@link FieldResolver}, {@link MethodResolver}, {@link ConstructorResolver} 处理解析
 * - {@link TypeCompatibility} 处理类型兼容性检查
 * - {@link VarargsHandler} 处理 varargs 参数
 */
public class ReflectionHelper {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    /**
     * 获取字段值（优先 MethodHandle）
     */
    public static Object getField(Object target, String fieldName) throws Throwable {
        if (target == null) {
            throw new NullPointerException("Cannot access field '" + fieldName + "' on null object");
        }
        Class<?> clazz = target.getClass();
        // 1. 快速路径：检查缓存
        MethodHandle cached = FieldCache.get(clazz, fieldName);
        if (cached != null) {
            return cached.invoke(target);
        }
        // 2. 慢路径：查找并缓存
        return getFieldSlow(target, clazz, fieldName);
    }

    /**
     * 调用方法（支持重载、多态缓存和varargs）
     */
    public static Object invokeMethod(Object target, String methodName, Object... args) throws Throwable {
        if (target == null) {
            throw new NullPointerException("Cannot invoke method '" + methodName + "' on null object");
        }
        Class<?> clazz = target.getClass();
        int argCount = args.length;
        // 1. 快速路径：多级缓存查找
        Class<?>[] argTypes = TypeCompatibility.getArgTypes(args);
        MethodHandle cached = MethodCache.get(clazz, methodName, argCount, argTypes);
        if (cached != null) {
            return invokeSpread(cached, target, args);
        }
        // 2. 慢路径：查找并缓存
        return invokeMethodSlow(target, clazz, methodName, args, argTypes);
    }

    /**
     * 调用构造函数（支持重载、多态缓存和varargs）
     *
     * @param clazz 目标类
     * @param args  构造函数参数
     * @return 新创建的实例
     */
    public static Object invokeConstructor(Class<?> clazz, Object... args) throws Throwable {
        int argCount = args.length;
        // 1. 快速路径：缓存查找
        Class<?>[] argTypes = TypeCompatibility.getArgTypes(args);
        MethodHandle cached = ConstructorCache.get(clazz, argCount, argTypes);
        if (cached != null) {
            return invokeConstructorSpread(cached, args);
        }
        // 2. 慢路径：查找并缓存
        return invokeConstructorSlow(clazz, args, argTypes);
    }

    // ==================== 慢路径实现 ====================

    /**
     * 字段访问慢路径
     */
    private static Object getFieldSlow(Object target, Class<?> clazz, String fieldName) throws Throwable {
        Field field = FieldResolver.findField(clazz, fieldName);
        // 创建 MethodHandle
        MethodHandle getter;
        try {
            if (field != null) {
                getter = LOOKUP.unreflectGetter(field);
                // 静态字段：添加被忽略的参数，统一为 (Object)Object 签名
                if (Modifier.isStatic(field.getModifiers())) {
                    getter = MethodHandles.dropArguments(getter, 0, Object.class);
                }
            } else {
                getter = FieldResolver.tryCreateGetterHandle(clazz, fieldName);
            }
        } catch (IllegalAccessException e) {
            throw new MemberAccessError("Cannot access field: " + fieldName, e);
        }
        if (getter == null) {
            throw new MemberNotFoundError(clazz, fieldName);
        }
        // 缓存（静态字段和实例字段统一使用 (Object)Object 签名）
        FieldCache.put(clazz, fieldName, getter);
        return getter.invoke(target);
    }

    /**
     * 方法调用慢路径
     */
    private static Object invokeMethodSlow(Object target, Class<?> clazz, String methodName, Object[] args, Class<?>[] argTypes) throws Throwable {
        int argCount = args.length;
        // 查找方法
        Method best = MethodResolver.findBestMatch(clazz, methodName, argTypes);
        // varargs 方法特殊处理（不缓存）
        if (best.isVarArgs()) {
            return VarargsHandler.invokeVarargsMethod(best, target, args);
        }
        // 创建优化的 MethodHandle
        try {
            MethodHandle mh = LOOKUP.unreflect(best);
            boolean isStatic = Modifier.isStatic(best.getModifiers());
            MethodHandle adapted;
            if (isStatic) {
                // 静态方法：添加一个被忽略的 receiver 参数，统一为 (Object, ...) 签名
                adapted = mh.asType(mh.type().changeReturnType(Object.class));
                adapted = MethodHandles.dropArguments(adapted, 0, Object.class);
            } else {
                // 实例方法：转换 receiver 类型为 Object
                adapted = mh.asType(mh.type().changeReturnType(Object.class).changeParameterType(0, Object.class));
            }
            // 将剩余参数转换为 spreader
            if (argCount > 0) {
                MethodType genericType = MethodType.genericMethodType(argCount + 1);
                adapted = adapted.asType(genericType);
                adapted = adapted.asSpreader(Object[].class, argCount);
            }
            // 缓存
            MethodCache.put(clazz, methodName, argCount, argTypes, adapted);
            return invokeSpread(adapted, target, args);
        } catch (IllegalAccessException e) {
            throw new MemberAccessError("Cannot access method: " + methodName, e);
        }
    }

    /**
     * 使用 spreader 调用 MethodHandle（比 invokeWithArguments 快）
     */
    private static Object invokeSpread(MethodHandle handle, Object target, Object[] args) throws Throwable {
        if (args.length == 0) {
            return handle.invoke(target);
        }
        return handle.invoke(target, args);
    }

    /**
     * 构造函数调用慢路径
     */
    private static Object invokeConstructorSlow(Class<?> clazz, Object[] args, Class<?>[] argTypes) throws Throwable {
        int argCount = args.length;
        // 匹配最佳构造函数
        Constructor<?> best = ConstructorResolver.findBestMatch(clazz, argTypes);
        // varargs 构造函数特殊处理（不缓存）
        if (best.isVarArgs()) {
            return VarargsHandler.invokeVarargsConstructor(best, args);
        }
        // 创建优化的 MethodHandle
        try {
            MethodHandle mh = LOOKUP.unreflectConstructor(best);
            MethodHandle adapted;
            if (argCount > 0) {
                MethodType genericType = MethodType.genericMethodType(argCount);
                adapted = mh.asType(genericType.changeReturnType(Object.class));
                adapted = adapted.asSpreader(Object[].class, argCount);
            } else {
                adapted = mh.asType(MethodType.methodType(Object.class));
            }
            // 缓存
            ConstructorCache.put(clazz, argCount, argTypes, adapted);
            return invokeConstructorSpread(adapted, args);
        } catch (IllegalAccessException e) {
            throw new MemberAccessError("Cannot access constructor of class: " + clazz.getName(), e);
        }
    }

    /**
     * 使用 spreader 调用构造函数 MethodHandle
     */
    private static Object invokeConstructorSpread(MethodHandle handle, Object[] args) throws Throwable {
        if (args.length == 0) {
            return handle.invoke();
        }
        return handle.invoke(args);
    }
}
