package org.tabooproject.fluxon.runtime;

import org.tabooproject.fluxon.interpreter.bytecode.BytecodeUtils;
import org.tabooproject.fluxon.runtime.error.MemberAccessError;
import org.tabooproject.fluxon.runtime.error.MemberNotFoundError;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反射缓存
 * 使用 MethodHandle 缓存反射访问，提供高性能的成员访问能力
 */
public class ReflectionCache {

    // 字段 Getter 缓存
    private static final ConcurrentHashMap<FieldKey, MethodHandle> FIELD_CACHE = new ConcurrentHashMap<>();

    // 方法缓存（按实际调用参数类型缓存）
    private static final ConcurrentHashMap<MethodKey, MethodHandle> METHOD_CACHE = new ConcurrentHashMap<>();

    // 方法索引（按方法名快速查找所有重载）
    private static final ConcurrentHashMap<Class<?>, Map<String, List<Method>>> METHOD_INDEX = new ConcurrentHashMap<>();

    // Lookup 实例（可访问 public 成员）
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    /**
     * 获取字段值（优先 MethodHandle）
     */
    public static Object getField(Object target, String fieldName) throws Throwable {
        if (target == null) {
            throw new NullPointerException("Cannot access field '" + fieldName + "' on null object");
        }
        Class<?> clazz = target.getClass();
        FieldKey key = new FieldKey(clazz, fieldName);
        MethodHandle getter = FIELD_CACHE.computeIfAbsent(key, k -> {
            try {
                // 1. 尝试直接字段访问
                Field field = findField(clazz, fieldName);
                if (field != null) {
                    return LOOKUP.unreflectGetter(field);
                }
                // 2. 降级：尝试无参方法 (getField, field, isField)
                return findGetterMethod(clazz, fieldName);
            } catch (IllegalAccessException e) {
                throw new MemberAccessError("Cannot access field: " + fieldName, e);
            }
        });
        if (getter == null) {
            throw new MemberNotFoundError(clazz, fieldName);
        }
        return getter.invoke(target);
    }

    /**
     * 调用方法（支持重载，多态缓存）
     */
    public static Object invokeMethod(Object target, String methodName, Object... args) throws Throwable {
        if (target == null) {
            throw new NullPointerException("Cannot invoke method '" + methodName + "' on null object");
        }
        Class<?> clazz = target.getClass();
        Class<?>[] argTypes = getArgTypes(args);
        MethodKey exactKey = new MethodKey(clazz, methodName, argTypes);
        // 1. 精确缓存命中
        MethodHandle cached = METHOD_CACHE.get(exactKey);
        if (cached != null) {
            return invokeHandle(cached, target, args);
        }
        // 2. 缓存未命中：查找并缓存
        MethodHandle method = findAndCacheMethod(clazz, methodName, argTypes);
        return invokeHandle(method, target, args);
    }

    /**
     * 查找字段（支持继承链）
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 查找 getter 方法（getField, field, isField）
     */
    private static MethodHandle findGetterMethod(Class<?> clazz, String fieldName) throws IllegalAccessException {
        String[] patterns = {"get" + capitalize(fieldName), fieldName, "is" + capitalize(fieldName)};
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

    /**
     * 查找方法并缓存（处理重载）
     */
    private static MethodHandle findAndCacheMethod(Class<?> clazz, String methodName, Class<?>[] argTypes) {
        // 查找所有同名方法（使用索引）
        List<Method> candidates = METHOD_INDEX
                .computeIfAbsent(clazz, ReflectionCache::buildMethodIndex)
                .getOrDefault(methodName, Collections.emptyList());
        if (candidates.isEmpty()) {
            throw new MemberNotFoundError(clazz, methodName, argTypes);
        }
        // 匹配最佳方法
        Method best = findBestMatch(candidates, argTypes);
        try {
            // 创建 MethodHandle
            MethodHandle mh = LOOKUP.unreflect(best);
            // 类型适配（自动装箱/拆箱、向上转型）
            MethodType targetType = MethodType.methodType(Object.class, prependObjectClass(getObjectTypes(argTypes)));
            MethodHandle adapted = mh.asType(targetType);
            // 缓存（使用实际参数类型作为键）
            MethodKey key = new MethodKey(clazz, methodName, argTypes);
            METHOD_CACHE.put(key, adapted);
            return adapted;
        } catch (IllegalAccessException e) {
            throw new MemberAccessError("Cannot access method: " + methodName, e);
        }
    }

    /**
     * 构建类的方法索引（延迟初始化）
     */
    private static Map<String, List<Method>> buildMethodIndex(Class<?> clazz) {
        Map<String, List<Method>> index = new HashMap<>();
        for (Method method : clazz.getMethods()) { // 仅 public 方法
            index.computeIfAbsent(method.getName(), k -> new ArrayList<>()).add(method);
        }
        return index;
    }

    /**
     * 匹配最佳方法
     */
    private static Method findBestMatch(List<Method> candidates, Class<?>[] argTypes) {
        // 优先级 1: 精确匹配
        for (Method m : candidates) {
            if (Arrays.equals(m.getParameterTypes(), argTypes)) {
                return m;
            }
        }
        // 优先级 2: 赋值兼容（所有参数都可赋值）
        List<Method> compatible = new ArrayList<>();
        for (Method m : candidates) {
            if (isAssignable(m.getParameterTypes(), argTypes)) {
                compatible.add(m);
            }
        }
        // 优先级 3: 选择最具体的方法（参数类型最接近）
        if (compatible.size() == 1) {
            return compatible.get(0);
        }
        if (compatible.size() > 1) {
            return findMostSpecific(compatible, argTypes);
        }
        // 没有找到匹配的方法
        throw new MemberNotFoundError(candidates.get(0).getDeclaringClass(), candidates.get(0).getName(), argTypes);
    }

    /**
     * 检查参数类型是否可赋值
     */
    private static boolean isAssignable(Class<?>[] paramTypes, Class<?>[] argTypes) {
        if (paramTypes.length != argTypes.length) {
            return false;
        }
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> param = paramTypes[i];
            Class<?> arg = argTypes[i];
            // 处理 null 参数
            if (arg == null) {
                if (param.isPrimitive()) {
                    return false;
                }
                continue;
            }
            // 处理装箱/拆箱
            if (param.isPrimitive() || arg.isPrimitive()) {
                if (!isPrimitiveCompatible(param, arg)) {
                    return false;
                }
            } else {
                if (!param.isAssignableFrom(arg)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 检查原始类型兼容性（支持装箱/拆箱）
     */
    private static boolean isPrimitiveCompatible(Class<?> param, Class<?> arg) {
        Class<?> paramBoxed = BytecodeUtils.boxToClass(param);
        Class<?> argBoxed = BytecodeUtils.boxToClass(arg);
        return paramBoxed.equals(argBoxed) || paramBoxed.isAssignableFrom(argBoxed);
    }

    /**
     * 查找最具体的方法（基于类型特异性）
     * 选择参数类型特异性总和最高的方法
     */
    private static Method findMostSpecific(List<Method> methods, Class<?>[] argTypes) {
        Method best = null;
        int bestScore = -1;
        for (Method method : methods) {
            int score = 0;
            Class<?>[] paramTypes = method.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                score += BytecodeUtils.getTypeSpecificity(paramType);
            }
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        return best != null ? best : methods.get(0);
    }

    /**
     * 调用 MethodHandle
     */
    private static Object invokeHandle(MethodHandle handle, Object target, Object[] args) throws Throwable {
        Object[] allArgs = new Object[args.length + 1];
        allArgs[0] = target;
        System.arraycopy(args, 0, allArgs, 1, args.length);
        return handle.invokeWithArguments(allArgs);
    }

    /**
     * 获取参数类型数组
     */
    private static Class<?>[] getArgTypes(Object[] args) {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] != null ? args[i].getClass() : null;
        }
        return types;
    }

    /**
     * 转换为 Object 类型数组
     */
    private static Class<?>[] getObjectTypes(Class<?>[] types) {
        Class<?>[] objectTypes = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            objectTypes[i] = Object.class;
        }
        return objectTypes;
    }

    /**
     * 在数组前添加 Object.class
     */
    private static Class<?>[] prependObjectClass(Class<?>[] types) {
        Class<?>[] result = new Class<?>[types.length + 1];
        result[0] = Object.class;
        System.arraycopy(types, 0, result, 1, types.length);
        return result;
    }

    /**
     * 首字母大写
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    // ========== 缓存键 ==========

    /**
     * 字段缓存键
     */
    private static class FieldKey {

        private final Class<?> clazz;
        private final String fieldName;
        private final int hash;

        FieldKey(Class<?> clazz, String fieldName) {
            this.clazz = clazz;
            this.fieldName = fieldName;
            this.hash = Objects.hash(clazz, fieldName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FieldKey)) return false;
            FieldKey fieldKey = (FieldKey) o;
            return clazz.equals(fieldKey.clazz) && fieldName.equals(fieldKey.fieldName);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    /**
     * 方法缓存键
     */
    private static class MethodKey {

        private final Class<?> clazz;
        private final String methodName;
        private final Class<?>[] paramTypes;
        private final int hash;

        MethodKey(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
            this.clazz = clazz;
            this.methodName = methodName;
            this.paramTypes = paramTypes;
            this.hash = Objects.hash(clazz, methodName, Arrays.hashCode(paramTypes));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodKey)) return false;
            MethodKey methodKey = (MethodKey) o;
            return clazz.equals(methodKey.clazz) && methodName.equals(methodKey.methodName) && Arrays.equals(paramTypes, methodKey.paramTypes);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
