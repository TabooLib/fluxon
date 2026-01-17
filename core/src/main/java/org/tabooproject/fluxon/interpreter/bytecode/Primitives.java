package org.tabooproject.fluxon.interpreter.bytecode;

import org.jetbrains.annotations.Nullable;

public class Primitives {

    /**
     * 将基本类型转换为对应的包装类
     * 如果不是基本类型，则直接返回原类型
     *
     * @param type 要转换的类型
     * @return 包装类或原类型
     */
    public static Class<?> boxToClass(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    /**
     * 获取基本类型对应的包装类内部名称
     */
    @Nullable
    public static String getWrapperClassName(Class<?> primitiveType) {
        if (primitiveType == int.class) return "java/lang/Integer";
        if (primitiveType == long.class) return "java/lang/Long";
        if (primitiveType == double.class) return "java/lang/Double";
        if (primitiveType == float.class) return "java/lang/Float";
        if (primitiveType == boolean.class) return "java/lang/Boolean";
        if (primitiveType == byte.class) return "java/lang/Byte";
        if (primitiveType == short.class) return "java/lang/Short";
        if (primitiveType == char.class) return "java/lang/Character";
        if (primitiveType == void.class) return "java/lang/Void";
        return null;
    }

    /**
     * 获取基本类型对应的拆箱方法名
     */
    @Nullable
    public static String getUnboxingMethodName(Class<?> primitiveType) {
        if (primitiveType == boolean.class) return "booleanValue";
        if (primitiveType == byte.class) return "byteValue";
        if (primitiveType == short.class) return "shortValue";
        if (primitiveType == int.class) return "intValue";
        if (primitiveType == long.class) return "longValue";
        if (primitiveType == float.class) return "floatValue";
        if (primitiveType == double.class) return "doubleValue";
        if (primitiveType == char.class) return "charValue";
        return null;
    }

    /**
     * 计算类型的特异性分数（继承深度）
     * 分数越高表示类型越具体，用于重载解析时的优先级排序
     * <p>
     * 分数规则：
     * - null/Object: 0
     * - 接口: 1（最通用的引用类型）
     * - 具体类: 10 + 继承深度（确保比接口更具体）
     * - 基本类型: 100（最具体）
     *
     * @param type 要计算特异性的类型
     * @return 特异性分数
     */
    public static int getTypeSpecificity(Class<?> type) {
        if (type == null || type == Object.class) {
            return 0;
        }
        if (type.isPrimitive()) {
            return 100;
        }
        if (type.isInterface()) {
            return 1;
        }
        // 计算到 Object 的继承深度，基础分 10 确保比接口高
        int depth = 10;
        Class<?> current = type;
        while (current != null && current != Object.class) {
            depth++;
            current = current.getSuperclass();
        }
        return depth;
    }
}
