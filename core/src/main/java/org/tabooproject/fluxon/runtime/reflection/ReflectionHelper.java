package org.tabooproject.fluxon.runtime.reflection;

import org.tabooproject.fluxon.interpreter.bytecode.BytecodeUtils;
import org.tabooproject.fluxon.runtime.error.MemberAccessError;
import org.tabooproject.fluxon.runtime.error.MemberNotFoundError;
import org.tabooproject.fluxon.util.StringUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.reflect.Array.*;

/**
 * 反射缓存
 * 使用 MethodHandle 缓存反射访问，提供高性能的成员访问能力
 */
public class ReflectionHelper {

    // ==================== 缓存结构 ====================

    // 字段 Getter 缓存：Class -> fieldName -> MethodHandle
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, MethodHandle>> FIELD_CACHE = new ConcurrentHashMap<>();
    // 方法缓存：Class -> methodName -> argCount -> [argTypes -> MethodHandle]
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, ArityCache>> METHOD_CACHE = new ConcurrentHashMap<>();
    // 方法索引（按方法名快速查找所有重载）
    private static final ConcurrentHashMap<Class<?>, Map<String, List<Method>>> METHOD_INDEX = new ConcurrentHashMap<>();
    // 构造函数缓存：Class -> ArityCache
    private static final ConcurrentHashMap<Class<?>, ArityCache> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();
    // 构造函数索引：Class -> List<Constructor<?>>
    private static final ConcurrentHashMap<Class<?>, List<Constructor<?>>> CONSTRUCTOR_INDEX = new ConcurrentHashMap<>();
    // Lookup 实例（可访问 public 成员）
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    // ==================== 预缓存常量 ====================

    // 数值类型等级映射（使用 IdentityHashMap 实现 O(1) 查找）
    private static final IdentityHashMap<Class<?>, Integer> NUMERIC_RANK = new IdentityHashMap<>(16);

    static {
        // 初始化数值类型等级
        NUMERIC_RANK.put(byte.class, 1);
        NUMERIC_RANK.put(Byte.class, 1);
        NUMERIC_RANK.put(short.class, 2);
        NUMERIC_RANK.put(Short.class, 2);
        NUMERIC_RANK.put(char.class, 2);
        NUMERIC_RANK.put(Character.class, 2);
        NUMERIC_RANK.put(int.class, 3);
        NUMERIC_RANK.put(Integer.class, 3);
        NUMERIC_RANK.put(long.class, 4);
        NUMERIC_RANK.put(Long.class, 4);
        NUMERIC_RANK.put(float.class, 5);
        NUMERIC_RANK.put(Float.class, 5);
        NUMERIC_RANK.put(double.class, 6);
        NUMERIC_RANK.put(Double.class, 6);
    }

    // ==================== 公共 API ====================

    /**
     * 获取字段值（优先 MethodHandle）
     */
    public static Object getField(Object target, String fieldName) throws Throwable {
        if (target == null) {
            throw new NullPointerException("Cannot access field '" + fieldName + "' on null object");
        }
        Class<?> clazz = target.getClass();
        // 1. 快速路径：检查缓存（避免创建 Key 对象）
        ConcurrentHashMap<String, MethodHandle> classCache = FIELD_CACHE.get(clazz);
        if (classCache != null) {
            MethodHandle cached = classCache.get(fieldName);
            if (cached != null) {
                return cached.invoke(target);
            }
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
        // 1. 快速路径：多级缓存查找（避免创建 MethodKey 对象）
        ConcurrentHashMap<String, ArityCache> classCache = METHOD_CACHE.get(clazz);
        if (classCache != null) {
            ArityCache arityCache = classCache.get(methodName);
            if (arityCache != null) {
                Class<?>[] argTypes = getArgTypes(args);
                MethodHandle cached = arityCache.get(argCount, argTypes);
                if (cached != null) {
                    // 使用 invoke 而非 invokeWithArguments（更快）
                    return invokeSpread(cached, target, args);
                }
            }
        }
        // 2. 慢路径：查找并缓存
        return invokeMethodSlow(target, clazz, methodName, args);
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
        ArityCache arityCache = CONSTRUCTOR_CACHE.get(clazz);
        if (arityCache != null) {
            Class<?>[] argTypes = getArgTypes(args);
            MethodHandle cached = arityCache.get(argCount, argTypes);
            if (cached != null) {
                return invokeConstructorSpread(cached, args);
            }
        }
        // 2. 慢路径：查找并缓存
        return invokeConstructorSlow(clazz, args);
    }

    // ==================== 慢路径实现 ====================

    /**
     * 字段访问慢路径
     */
    private static Object getFieldSlow(Object target, Class<?> clazz, String fieldName) throws Throwable {
        // 检查是否为静态字段
        Field field = findField(clazz, fieldName);
        if (field != null && Modifier.isStatic(field.getModifiers())) {
            return field.get(null);
        }
        // 创建 MethodHandle
        MethodHandle getter;
        try {
            if (field != null) {
                getter = LOOKUP.unreflectGetter(field);
            } else {
                getter = findGetterMethod(clazz, fieldName);
            }
        } catch (IllegalAccessException e) {
            throw new MemberAccessError("Cannot access field: " + fieldName, e);
        }
        if (getter == null) {
            throw new MemberNotFoundError(clazz, fieldName);
        }
        // 缓存
        FIELD_CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>()).put(fieldName, getter);
        return getter.invoke(target);
    }

    /**
     * 方法调用慢路径
     */
    private static Object invokeMethodSlow(Object target, Class<?> clazz, String methodName, Object[] args) throws Throwable {
        Class<?>[] argTypes = getArgTypes(args);
        int argCount = args.length;
        // 查找方法
        List<Method> candidates = METHOD_INDEX
                .computeIfAbsent(clazz, ReflectionHelper::buildMethodIndex)
                .getOrDefault(methodName, Collections.emptyList());
        if (candidates.isEmpty()) {
            throw new MemberNotFoundError(clazz, methodName, argTypes);
        }
        // 匹配最佳方法
        Method best = findBestMatch(candidates, argTypes);
        // varargs 方法特殊处理（不缓存）
        if (best.isVarArgs()) {
            return invokeVarargsMethod(best, target, args);
        }
        // 创建优化的 MethodHandle
        try {
            MethodHandle mh = LOOKUP.unreflect(best);
            // 转换为 (Object, Object[]) -> Object 形式，使用 asSpreader
            MethodHandle adapted = mh.asType(mh.type().changeReturnType(Object.class).changeParameterType(0, Object.class));
            // 将剩余参数转换为 spreader
            if (argCount > 0) {
                // 先转换参数类型为 Object
                MethodType genericType = MethodType.genericMethodType(argCount + 1);
                adapted = adapted.asType(genericType);
                adapted = adapted.asSpreader(Object[].class, argCount);
            }
            // 缓存
            METHOD_CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(methodName, k -> new ArityCache())
                    .put(argCount, argTypes, adapted);
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
     * 调用varargs方法（处理参数打包）
     */
    private static Object invokeVarargsMethod(Method method, Object target, Object[] args) throws Throwable {
        Object[] newArgs = packVarargsArguments(method.getParameterTypes(), args);
        return method.invoke(target, newArgs);
    }

    /**
     * 构造函数调用慢路径
     */
    private static Object invokeConstructorSlow(Class<?> clazz, Object[] args) throws Throwable {
        Class<?>[] argTypes = getArgTypes(args);
        int argCount = args.length;
        // 查找构造函数
        List<Constructor<?>> candidates = CONSTRUCTOR_INDEX.computeIfAbsent(clazz, ReflectionHelper::buildConstructorIndex);
        if (candidates.isEmpty()) {
            throw new MemberAccessError("No public constructor found for class: " + clazz.getName());
        }
        // 匹配最佳构造函数
        Constructor<?> best = findBestConstructorMatch(candidates, argTypes);
        // varargs 构造函数特殊处理（不缓存）
        if (best.isVarArgs()) {
            return invokeVarargsConstructor(best, args);
        }
        // 创建优化的 MethodHandle
        try {
            MethodHandle mh = LOOKUP.unreflectConstructor(best);
            // 转换为 (Object[]) -> Object 形式
            MethodHandle adapted;
            if (argCount > 0) {
                MethodType genericType = MethodType.genericMethodType(argCount);
                adapted = mh.asType(genericType.changeReturnType(Object.class));
                adapted = adapted.asSpreader(Object[].class, argCount);
            } else {
                adapted = mh.asType(MethodType.methodType(Object.class));
            }
            // 缓存
            CONSTRUCTOR_CACHE.computeIfAbsent(clazz, k -> new ArityCache()).put(argCount, argTypes, adapted);
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

    /**
     * 调用 varargs 构造函数（处理参数打包）
     */
    private static Object invokeVarargsConstructor(Constructor<?> ctor, Object[] args) throws Throwable {
        Object[] newArgs = packVarargsArguments(ctor.getParameterTypes(), args);
        return ctor.newInstance(newArgs);
    }

    /**
     * 打包 varargs 参数
     * 将原始参数数组转换为符合 varargs 方法/构造函数签名的参数数组
     *
     * @param paramTypes 方法/构造函数的参数类型（最后一个是数组类型）
     * @param args       原始参数
     * @return 打包后的参数数组
     */
    private static Object[] packVarargsArguments(Class<?>[] paramTypes, Object[] args) {
        int fixedParamCount = paramTypes.length - 1;
        Class<?> varargType = paramTypes[fixedParamCount].getComponentType();
        // 构建新的参数数组
        Object[] newArgs = new Object[paramTypes.length];
        // 复制固定参数
        System.arraycopy(args, 0, newArgs, 0, fixedParamCount);
        // 打包 varargs 参数
        int varargCount = args.length - fixedParamCount;
        Object varargArray = newInstance(varargType, varargCount);
        for (int i = 0; i < varargCount; i++) {
            set(varargArray, i, args[fixedParamCount + i]);
        }
        newArgs[fixedParamCount] = varargArray;
        return newArgs;
    }

    /**
     * 构建类的构造函数索引
     */
    private static List<Constructor<?>> buildConstructorIndex(Class<?> clazz) {
        return new ArrayList<>(Arrays.asList(clazz.getConstructors()));
    }

    /**
     * 匹配最佳构造函数（支持varargs）
     */
    private static Constructor<?> findBestConstructorMatch(List<Constructor<?>> candidates, Class<?>[] argTypes) {
        Constructor<?> varargsFallback = null;
        for (Constructor<?> c : candidates) {
            Class<?>[] paramTypes = c.getParameterTypes();
            // 优先级 1: 精确匹配
            if (Arrays.equals(paramTypes, argTypes)) {
                return c;
            }
            // 优先级 2: 赋值兼容（非 varargs）
            if (!c.isVarArgs() && isAssignable(paramTypes, argTypes)) {
                return c;
            }
            // 记录 varargs 备选
            if (c.isVarArgs() && varargsFallback == null && isVarargsConstructorAssignable(c, argTypes)) {
                varargsFallback = c;
            }
        }
        // 优先级 3: varargs 匹配
        if (varargsFallback != null) {
            return varargsFallback;
        }
        throw new MemberAccessError("No matching constructor found for argument types: " + Arrays.toString(argTypes));
    }

    /**
     * 检查 varargs 构造函数是否匹配
     */
    private static boolean isVarargsConstructorAssignable(Constructor<?> ctor, Class<?>[] argTypes) {
        return isVarargsParametersCompatible(ctor.getParameterTypes(), argTypes);
    }

    // ==================== 字段查找 ====================

    /**
     * 查找字段（支持继承链和接口常量）
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        // 直接使用 getField - 它会搜索整个继承链包括接口
        try {
            return clazz.getField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * 查找 getter 方法（getField, field, isField）
     */
    private static MethodHandle findGetterMethod(Class<?> clazz, String fieldName) throws IllegalAccessException {
        String capitalized = StringUtils.capitalize(fieldName);
        // 尝试三种模式
        String[] patterns = {"get" + capitalized, fieldName, "is" + capitalized};
        for (String methodName : patterns) {
            try {
                Method method = clazz.getMethod(methodName);
                if (method.getParameterCount() == 0) {
                    return LOOKUP.unreflect(method);
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    // ==================== 方法索引与匹配 ====================

    /**
     * 构建类的方法索引（延迟初始化）
     */
    private static Map<String, List<Method>> buildMethodIndex(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        Map<String, List<Method>> index = new HashMap<>(methods.length);
        for (Method method : methods) {
            index.computeIfAbsent(method.getName(), k -> new ArrayList<>(4)).add(method);
        }
        return index;
    }

    /**
     * 匹配最佳方法（支持varargs）
     */
    private static Method findBestMatch(List<Method> candidates, Class<?>[] argTypes) {
        Method varargsFallback = null;
        for (Method m : candidates) {
            Class<?>[] paramTypes = m.getParameterTypes();
            // 优先级 1: 精确匹配
            if (Arrays.equals(paramTypes, argTypes)) {
                return m;
            }
            // 优先级 2: 赋值兼容（非 varargs）
            if (!m.isVarArgs() && isAssignable(paramTypes, argTypes)) {
                return m; // 找到第一个兼容的就返回
            }
            // 记录 varargs 备选
            if (m.isVarArgs() && varargsFallback == null && isVarargsAssignable(m, argTypes)) {
                varargsFallback = m;
            }
        }
        // 优先级 3: varargs 匹配
        if (varargsFallback != null) {
            return varargsFallback;
        }
        throw new MemberNotFoundError(candidates.get(0).getDeclaringClass(), candidates.get(0).getName(), argTypes);
    }

    /**
     * 检查varargs方法是否匹配
     */
    private static boolean isVarargsAssignable(Method method, Class<?>[] argTypes) {
        return isVarargsParametersCompatible(method.getParameterTypes(), argTypes);
    }

    /**
     * 检查 varargs 参数类型是否兼容
     *
     * @param paramTypes 方法/构造函数的参数类型（最后一个是数组类型）
     * @param argTypes   实际参数类型
     * @return 是否兼容
     */
    private static boolean isVarargsParametersCompatible(Class<?>[] paramTypes, Class<?>[] argTypes) {
        int fixedParamCount = paramTypes.length - 1;
        if (argTypes.length < fixedParamCount) {
            return false;
        }
        // 检查固定参数
        for (int i = 0; i < fixedParamCount; i++) {
            if (!isTypeCompatible(paramTypes[i], argTypes[i])) {
                return false;
            }
        }
        // 检查 varargs 参数
        Class<?> varargType = paramTypes[fixedParamCount].getComponentType();
        for (int i = fixedParamCount; i < argTypes.length; i++) {
            if (!isTypeCompatible(varargType, argTypes[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查参数类型是否可赋值
     */
    private static boolean isAssignable(Class<?>[] paramTypes, Class<?>[] argTypes) {
        if (paramTypes.length != argTypes.length) {
            return false;
        }
        for (int i = 0; i < paramTypes.length; i++) {
            if (!isTypeCompatible(paramTypes[i], argTypes[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查单个类型是否兼容
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isTypeCompatible(Class<?> param, Class<?> arg) {
        if (arg == null) {
            return !param.isPrimitive();
        }
        if (param.isPrimitive() || arg.isPrimitive()) {
            return isPrimitiveCompatible(param, arg);
        }
        return param.isAssignableFrom(arg);
    }

    /**
     * 检查原始类型兼容性
     */
    private static boolean isPrimitiveCompatible(Class<?> param, Class<?> arg) {
        Class<?> paramBoxed = BytecodeUtils.boxToClass(param);
        Class<?> argBoxed = BytecodeUtils.boxToClass(arg);
        if (paramBoxed == argBoxed) { // 使用 == 替代 equals（Class 对象唯一）
            return true;
        }
        // 数值类型拓宽转换
        Integer fromRank = NUMERIC_RANK.get(argBoxed);
        Integer toRank = NUMERIC_RANK.get(paramBoxed);
        if (fromRank != null && toRank != null) {
            return fromRank <= toRank;
        }
        return paramBoxed.isAssignableFrom(argBoxed);
    }

    /**
     * 获取参数类型数组
     */
    private static Class<?>[] getArgTypes(Object[] args) {
        int len = args.length;
        Class<?>[] types = new Class<?>[len];
        for (int i = 0; i < len; i++) {
            Object arg = args[i];
            types[i] = arg != null ? arg.getClass() : null;
        }
        return types;
    }
}
