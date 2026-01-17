package org.tabooproject.fluxon.runtime.reflection.util;

import org.tabooproject.fluxon.interpreter.bytecode.Primitives;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * 类型兼容性检查工具类
 * 提供原始类型装箱、数值类型拓宽转换等类型兼容性检查
 */
public final class TypeCompatibility {

    private TypeCompatibility() {}

    // 数值类型等级映射（使用 IdentityHashMap 实现 O(1) 查找）
    private static final IdentityHashMap<Class<?>, Integer> NUMERIC_RANK = new IdentityHashMap<>(16);

    static {
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

    /**
     * 检查参数类型数组是否可赋值给形参类型数组
     */
    public static boolean isAssignable(Class<?>[] paramTypes, Class<?>[] argTypes) {
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
    public static boolean isTypeCompatible(Class<?> param, Class<?> arg) {
        if (arg == null) {
            return !param.isPrimitive();
        }
        if (param.isPrimitive() || arg.isPrimitive()) {
            return isPrimitiveCompatible(param, arg);
        }
        return param.isAssignableFrom(arg);
    }

    /**
     * 检查原始类型兼容性（包括装箱和数值拓宽）
     */
    public static boolean isPrimitiveCompatible(Class<?> param, Class<?> arg) {
        Class<?> paramBoxed = Primitives.boxToClass(param);
        Class<?> argBoxed = Primitives.boxToClass(arg);
        if (paramBoxed == argBoxed) {
            return true;
        }
        // char 只能拓宽到 int/long/float/double，不能接受其他类型
        // 其他数值类型也不能缩窄到 char
        if (paramBoxed == Character.class) {
            return false;
        }
        // 数值类型拓宽转换（char 的 rank=2，可拓宽到 int/long/float/double）
        Integer fromRank = NUMERIC_RANK.get(argBoxed);
        Integer toRank = NUMERIC_RANK.get(paramBoxed);
        if (fromRank != null && toRank != null) {
            return fromRank <= toRank;
        }
        return paramBoxed.isAssignableFrom(argBoxed);
    }

    /**
     * 检查类型是否兼容（考虑自动装箱和数值拓宽）- 用于 Bootstrap
     */
    public static boolean isAssignableFrom(Class<?> param, Class<?> arg) {
        if (param.isAssignableFrom(arg)) return true;
        // 委托给 isPrimitiveCompatible 处理装箱和数值拓宽
        if (param.isPrimitive() || arg.isPrimitive()) {
            return isPrimitiveCompatible(param, arg);
        }
        return false;
    }

    /**
     * 检查参数类型是否兼容（用于方法/构造函数匹配）
     */
    public static boolean isParametersCompatible(Class<?>[] paramTypes, Class<?>[] argTypes) {
        if (paramTypes.length != argTypes.length) {
            return false;
        }
        for (int i = 0; i < paramTypes.length; i++) {
            // null 只能赋给非原始类型
            if (argTypes[i] == null) {
                if (paramTypes[i].isPrimitive()) {
                    return false;
                }
            } else if (!isAssignableFrom(paramTypes[i], argTypes[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取参数类型数组（null 参数返回 null 类型）
     */
    public static Class<?>[] getArgTypes(Object[] args) {
        int len = args.length;
        Class<?>[] types = new Class<?>[len];
        for (int i = 0; i < len; i++) {
            Object arg = args[i];
            types[i] = arg != null ? arg.getClass() : null;
        }
        return types;
    }

    /**
     * 尝试获取参数类型数组（用于 specialized handle 创建）
     * 如果任何参数为 null，返回 null 表示无法创建 specialized handle
     *
     * @return 参数类型数组，或 null（如果包含 null 参数）
     */
    public static Class<?>[] tryGetArgTypes(Object[] args) {
        int len = args.length;
        Class<?>[] types = new Class<?>[len];
        for (int i = 0; i < len; i++) {
            Object arg = args[i];
            if (arg == null) {
                return null;
            }
            types[i] = arg.getClass();
        }
        return types;
    }

    /**
     * 提取参数类型数组（用于 PIC，null 用 Void.class 表示）
     */
    public static Class<?>[] extractArgTypes(Object[] args) {
        Class<?>[] argTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = args[i] != null ? args[i].getClass() : Void.class;
        }
        return argTypes;
    }

    /**
     * 检查 varargs 参数类型是否兼容
     *
     * @param paramTypes 方法/构造函数的参数类型（最后一个是数组类型）
     * @param argTypes   实际参数类型
     * @return 是否兼容
     */
    public static boolean isVarargsCompatible(Class<?>[] paramTypes, Class<?>[] argTypes) {
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
     * 匹配最佳 Executable（Method 或 Constructor）
     * 按优先级顺序：精确匹配 > 最具体的赋值兼容（非 varargs）> varargs 匹配
     *
     * @param candidates 候选列表
     * @param argTypes   实际参数类型
     * @param <T>        Executable 的子类型
     * @return 匹配的 Executable，如果没有匹配则返回 null
     */
    public static <T extends Executable> T findBestMatch(List<T> candidates, Class<?>[] argTypes) {
        T varargsFallback = null;
        List<T> compatibleCandidates = null; // 延迟初始化
        for (T executable : candidates) {
            Class<?>[] paramTypes = executable.getParameterTypes();
            // 优先级 1: 精确匹配（直接返回）
            if (Arrays.equals(paramTypes, argTypes)) {
                return executable;
            }
            // 优先级 2: 赋值兼容（非 varargs）- 收集所有兼容候选
            if (!executable.isVarArgs() && isAssignable(paramTypes, argTypes)) {
                if (compatibleCandidates == null) {
                    compatibleCandidates = new ArrayList<>(4);
                }
                compatibleCandidates.add(executable);
            }
            // 优先级 3: varargs 备选
            if (executable.isVarArgs() && varargsFallback == null && isVarargsCompatible(paramTypes, argTypes)) {
                varargsFallback = executable;
            }
        }
        // 从兼容候选中选择最具体的
        if (compatibleCandidates != null) {
            if (compatibleCandidates.size() == 1) {
                return compatibleCandidates.get(0);
            }
            return selectMostSpecific(compatibleCandidates);
        }
        // 优先级 3: varargs 匹配
        return varargsFallback;
    }

    /**
     * 从多个兼容候选中选择最具体的 Executable
     * 基于 JLS 15.12.2.5 的简化实现：参数类型更窄的更具体
     */
    private static <T extends Executable> T selectMostSpecific(List<T> candidates) {
        T best = candidates.get(0);
        for (int i = 1; i < candidates.size(); i++) {
            T current = candidates.get(i);
            if (isMoreSpecific(current, best)) {
                best = current;
            }
        }
        return best;
    }

    /**
     * 判断 a 是否比 b 更具体
     * 如果 a 的所有参数类型都可以赋值给 b 的对应参数类型，则 a 更具体
     */
    private static boolean isMoreSpecific(Executable a, Executable b) {
        Class<?>[] aParams = a.getParameterTypes();
        Class<?>[] bParams = b.getParameterTypes();
        if (aParams.length != bParams.length) {
            return false;
        }
        boolean aMoreSpecific = true;
        for (int i = 0; i < aParams.length; i++) {
            // a 的参数类型必须是 b 的参数类型的子类型（或相同）
            if (!bParams[i].isAssignableFrom(aParams[i])) {
                aMoreSpecific = false;
                break;
            }
        }
        return aMoreSpecific;
    }
}
