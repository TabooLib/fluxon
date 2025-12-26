package org.tabooproject.fluxon.runtime.reflection;

import org.tabooproject.fluxon.interpreter.bytecode.BytecodeUtils;
import org.tabooproject.fluxon.runtime.error.MemberAccessError;
import org.tabooproject.fluxon.runtime.error.MemberNotFoundError;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反射缓存
 * 使用 MethodHandle 缓存反射访问，提供高性能的成员访问能力
 */
public class ReflectionHelper {

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
        // 先检查是否为静态字段（如接口常量）
        // 静态字段直接获取，不需要实例
        Field field = findField(clazz, fieldName);
        if (field != null && Modifier.isStatic(field.getModifiers())) {
            return field.get(null);
        }
        MethodHandle getter = FIELD_CACHE.computeIfAbsent(key, k -> {
            try {
                // 1. 尝试直接字段访问（非静态）
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
     * 调用方法（支持重载、多态缓存和varargs）
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
        // 2. 缓存未命中：查找方法
        List<Method> candidates = METHOD_INDEX
                .computeIfAbsent(clazz, ReflectionHelper::buildMethodIndex)
                .getOrDefault(methodName, Collections.emptyList());
        if (candidates.isEmpty()) {
            throw new MemberNotFoundError(clazz, methodName, argTypes);
        }
        // 匹配最佳方法
        Method best = findBestMatch(candidates, argTypes);
        // 3. 如果是 varargs 方法，需要特殊处理参数
        if (best.isVarArgs()) {
            return invokeVarargsMethod(best, target, args);
        }
        // 4. 非 varargs 方法：缓存并调用
        MethodHandle method = findAndCacheMethod(clazz, methodName, argTypes);
        return invokeHandle(method, target, args);
    }

    /**
     * 调用varargs方法（处理参数打包）
     */
    private static Object invokeVarargsMethod(Method method, Object target, Object[] args) throws Throwable {
        Class<?>[] paramTypes = method.getParameterTypes();
        int fixedParamCount = paramTypes.length - 1;
        Class<?> varargType = paramTypes[fixedParamCount].getComponentType();
        // 构建新的参数数组
        Object[] newArgs = new Object[paramTypes.length];
        // 复制固定参数
        System.arraycopy(args, 0, newArgs, 0, fixedParamCount);
        // 打包 varargs 参数
        int varargCount = args.length - fixedParamCount;
        Object varargArray = java.lang.reflect.Array.newInstance(varargType, varargCount);
        for (int i = 0; i < varargCount; i++) {
            java.lang.reflect.Array.set(varargArray, i, args[fixedParamCount + i]);
        }
        newArgs[fixedParamCount] = varargArray;
        // 使用反射调用（varargs 不缓存，因为参数数量可变）
        return method.invoke(target, newArgs);
    }

    /**
     * 查找字段（支持继承链和接口常量）
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        // 1. 先在类继承链中查找
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        // 2. 在所有实现的接口中查找常量字段
        return findInterfaceField(Objects.requireNonNull(clazz), fieldName, new HashSet<>());
    }

    /**
     * 在接口继承链中查找字段（递归搜索所有父接口）
     */
    private static Field findInterfaceField(Class<?> clazz, String fieldName, Set<Class<?>> visited) {
        // 遍历当前类/接口实现的所有接口
        for (Class<?> iface : clazz.getInterfaces()) {
            if (visited.contains(iface)) continue;
            visited.add(iface);
            try {
                return iface.getField(fieldName);
            } catch (NoSuchFieldException e) {
                // 递归搜索父接口
                Field found = findInterfaceField(iface, fieldName, visited);
                if (found != null) return found;
            }
        }
        // 如果有父类，也检查父类的接口
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return findInterfaceField(superClass, fieldName, visited);
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
     * 查找方法并缓存（处理重载和varargs）
     */
    private static MethodHandle findAndCacheMethod(Class<?> clazz, String methodName, Class<?>[] argTypes) {
        // 查找所有同名方法（使用索引）
        List<Method> candidates = METHOD_INDEX
                .computeIfAbsent(clazz, ReflectionHelper::buildMethodIndex)
                .getOrDefault(methodName, Collections.emptyList());
        if (candidates.isEmpty()) {
            throw new MemberNotFoundError(clazz, methodName, argTypes);
        }
        // 匹配最佳方法
        Method best = findBestMatch(candidates, argTypes);
        try {
            MethodHandle mh = LOOKUP.unreflect(best);
            // 对于 varargs 方法，需要特殊处理
            if (best.isVarArgs()) {
                // 将 varargs 方法转换为接受 Object[] 的形式
                mh = mh.asFixedArity();
                // 不缓存 varargs 方法，因为每次调用参数数量可能不同
                return mh;
            }
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
     * 匹配最佳方法（支持varargs）
     */
    private static Method findBestMatch(List<Method> candidates, Class<?>[] argTypes) {
        // 优先级 1: 精确匹配
        for (Method m : candidates) {
            if (Arrays.equals(m.getParameterTypes(), argTypes)) {
                return m;
            }
        }
        // 优先级 2: 赋值兼容（所有参数都可赋值，不含 varargs）
        List<Method> compatible = new ArrayList<>();
        for (Method m : candidates) {
            if (!m.isVarArgs() && isAssignable(m.getParameterTypes(), argTypes)) {
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
        // 优先级 4: 尝试 varargs 匹配
        for (Method m : candidates) {
            if (m.isVarArgs() && isVarargsAssignable(m, argTypes)) {
                return m;
            }
        }
        // 没有找到匹配的方法
        throw new MemberNotFoundError(candidates.get(0).getDeclaringClass(), candidates.get(0).getName(), argTypes);
    }

    /**
     * 检查varargs方法是否匹配
     */
    private static boolean isVarargsAssignable(Method method, Class<?>[] argTypes) {
        Class<?>[] paramTypes = method.getParameterTypes();
        int fixedParamCount = paramTypes.length - 1;
        // 参数数量必须 >= 固定参数数量
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
     * 检查参数类型是否可赋值（支持数值类型拓宽转换）
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
     * 检查单个类型是否兼容（支持数值类型拓宽转换）
     */
    private static boolean isTypeCompatible(Class<?> param, Class<?> arg) {
        // 处理 null 参数
        if (arg == null) {
            return !param.isPrimitive();
        }
        // 处理装箱/拆箱和数值拓宽
        if (param.isPrimitive() || arg.isPrimitive()) {
            return isPrimitiveCompatible(param, arg);
        }
        // 引用类型赋值兼容性
        return param.isAssignableFrom(arg);
    }

    /**
     * 检查原始类型兼容性（支持装箱/拆箱和数值拓宽转换）
     * 支持 Java 的数值拓宽规则：
     * - byte → short, int, long, float, double
     * - short → int, long, float, double
     * - char → int, long, float, double
     * - int → long, float, double
     * - long → float, double
     * - float → double
     */
    private static boolean isPrimitiveCompatible(Class<?> param, Class<?> arg) {
        Class<?> paramBoxed = BytecodeUtils.boxToClass(param);
        Class<?> argBoxed = BytecodeUtils.boxToClass(arg);
        // 完全匹配（包含装箱/拆箱）
        if (paramBoxed.equals(argBoxed)) {
            return true;
        }
        // 数值类型拓宽转换
        if (Number.class.isAssignableFrom(argBoxed) && Number.class.isAssignableFrom(paramBoxed)) {
            return isNumericWideningAllowed(argBoxed, paramBoxed);
        }
        // 普通引用类型赋值
        return paramBoxed.isAssignableFrom(argBoxed);
    }

    /**
     * 检查数值拓宽转换是否允许
     */
    private static boolean isNumericWideningAllowed(Class<?> from, Class<?> to) {
        // 拓宽转换优先级：byte < short < int < long < float < double
        int fromRank = getNumericRank(from);
        int toRank = getNumericRank(to);
        // 允许从较小类型转换到较大类型
        // 特殊情况：int/long 可以转换为 float/double（可能有精度损失但 Java 允许）
        return fromRank <= toRank;
    }

    /**
     * 获取数值类型的拓宽等级
     */
    private static int getNumericRank(Class<?> type) {
        if (type == Byte.class || type == byte.class) return 1;
        if (type == Short.class || type == short.class) return 2;
        if (type == Character.class || type == char.class) return 2; // char 与 short 同级
        if (type == Integer.class || type == int.class) return 3;
        if (type == Long.class || type == long.class) return 4;
        if (type == Float.class || type == float.class) return 5;
        if (type == Double.class || type == double.class) return 6;
        return 0; // 未知类型
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
}
