package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数重载集合
 * 存储同名函数的多个重载版本，并根据参数类型选择最佳重载
 *
 * @author sky
 */
public class OverloadSet {

    private final String name;
    private final List<Function> overloads = new ArrayList<>();

    // 缓存：单一重载时直接返回，避免解析开销
    private Function singleOverload;

    public OverloadSet(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Function> getOverloads() {
        return overloads;
    }

    /**
     * 添加重载
     */
    public void add(Function function) {
        overloads.add(function);
        singleOverload = overloads.size() == 1 ? function : null;
    }

    /**
     * 移除重载
     */
    public boolean remove(Function function) {
        boolean removed = overloads.remove(function);
        if (removed) {
            singleOverload = overloads.size() == 1 ? overloads.get(0) : null;
        }
        return removed;
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return overloads.isEmpty();
    }

    /**
     * 获取重载数量
     */
    public int size() {
        return overloads.size();
    }

    /**
     * 根据参数类型解析最佳重载（编译期）
     *
     * @param argTypes 参数类型数组
     * @return 最佳匹配的函数，如果没有匹配返回 null
     */
    @Nullable
    public Function resolve(Type[] argTypes) {
        // 单一重载快速路径
        if (singleOverload != null) {
            FunctionSignature sig = singleOverload.getSignature();
            if (sig != null && computeMatchScore(sig, argTypes) >= 0) {
                return singleOverload;
            }
            // 签名不匹配或无签名，仍然返回（运行时检查）
            return singleOverload;
        }
        Function bestMatch = null;
        int bestScore = -1;
        for (Function f : overloads) {
            FunctionSignature sig = f.getSignature();
            if (sig == null) {
                // 无签名的函数作为兜底
                if (bestMatch == null) {
                    bestMatch = f;
                }
                continue;
            }
            int score = computeMatchScore(sig, argTypes);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = f;
            }
        }
        return bestMatch;
    }

    /**
     * 获取第一个重载（用于无参数时）
     */
    @Nullable
    public Function first() {
        return overloads.isEmpty() ? null : overloads.get(0);
    }

    /**
     * 计算签名与参数类型的匹配分数
     *
     * @return 匹配分数，-1 表示不匹配
     */
    private int computeMatchScore(FunctionSignature sig, Type[] argTypes) {
        int paramCount = sig.getParameterCount();
        int argCount = argTypes.length;
        // 参数数量检查
        if (argCount != paramCount) {
            return -1;
        }
        int score = 0;
        Type[] paramTypes = sig.getParameterTypes();
        for (int i = 0; i < argCount; i++) {
            Type expected = paramTypes[i];
            Type actual = argTypes[i];
            int paramScore = computeParamScore(actual, expected);
            if (paramScore < 0) {
                return -1;
            }
            score += paramScore;
        }
        return score;
    }

    /**
     * 计算单个参数的匹配分数
     */
    private int computeParamScore(Type actual, Type expected) {
        // 未知类型，以最低分接受
        if (actual == null) {
            return 0;
        }
        // 精确匹配
        if (actual == expected || actual.equals(expected)) {
            return 4;
        }
        // 目标是 OBJECT，接受一切
        if (expected == Type.OBJECT) {
            return 0;
        }
        Class<?> actualClass = actual.getSource();
        Class<?> expectedClass = expected.getSource();
        // 数值类型拓宽
        if (isNumeric(actual) && isNumeric(expected)) {
            if (isNumericAssignable(actualClass, expectedClass)) {
                return 2;
            }
            return -1;
        }
        // 数值类型到 NUMBER
        if (isNumeric(actual) && expected == Type.NUMBER) {
            return 1;
        }
        // 引用类型继承
        if (!actual.isPrimitive() && !expected.isPrimitive()) {
            if (expectedClass.isAssignableFrom(actualClass)) {
                int distance = computeInheritanceDistance(actualClass, expectedClass);
                return Math.max(1, 3 - distance);
            }
            return -1;
        }
        // 源是 OBJECT（动态类型）
        if (actual == Type.OBJECT) {
            return 0;
        }
        return -1;
    }

    /**
     * 检查是否为数值类型
     */
    private boolean isNumeric(Type type) {
        return type == Type.I || type == Type.J || type == Type.F || type == Type.D
                || type == Type.INT || type == Type.LONG || type == Type.FLOAT || type == Type.DOUBLE
                || type == Type.NUMBER;
    }

    /**
     * 检查数值类型是否可以拓宽赋值
     */
    private boolean isNumericAssignable(Class<?> from, Class<?> to) {
        // int -> long, float, double
        if (from == int.class || from == Integer.class) {
            return to == int.class || to == Integer.class
                    || to == long.class || to == Long.class
                    || to == float.class || to == Float.class
                    || to == double.class || to == Double.class;
        }
        // long -> float, double
        if (from == long.class || from == Long.class) {
            return to == long.class || to == Long.class
                    || to == float.class || to == Float.class
                    || to == double.class || to == Double.class;
        }
        // float -> double
        if (from == float.class || from == Float.class) {
            return to == float.class || to == Float.class
                    || to == double.class || to == Double.class;
        }
        // double -> double
        if (from == double.class || from == Double.class) {
            return to == double.class || to == Double.class;
        }
        return false;
    }

    /**
     * 计算继承距离
     */
    private int computeInheritanceDistance(Class<?> from, Class<?> to) {
        if (from == to) return 0;
        int distance = 0;
        Class<?> current = from;
        while (current != null && current != to) {
            for (Class<?> iface : current.getInterfaces()) {
                if (to.isAssignableFrom(iface)) {
                    return distance + 1;
                }
            }
            current = current.getSuperclass();
            distance++;
        }
        return distance;
    }

    @Override
    public String toString() {
        return "OverloadSet{" + name + ", overloads=" + overloads.size() + "}";
    }
}
