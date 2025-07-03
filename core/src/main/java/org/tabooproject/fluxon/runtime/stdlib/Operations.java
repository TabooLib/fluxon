package org.tabooproject.fluxon.runtime.stdlib;

import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.error.OperationException;

// @formatter:off
public final class Operations {

    public static final Type TYPE = new Type(Operations.class);

    /**
     * 检查操作数是否为数字
     *
     * @param operand 操作数
     */
    public static void checkNumberOperand(Object operand) {
        if (operand instanceof Number) return;
        throw new OperationException("Operands must be numbers.");
    }

    /**
     * 检查操作数是否都是数字
     *
     * @param left  左操作数
     * @param right 右操作数
     */
    public static void checkNumberOperands(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) return;
        throw new OperationException("Operands must be numbers.");
    }

    /**
     * 评估布尔值，判断对象是否为真
     *
     * @param value 要判断的对象
     * @return 布尔值结果
     */
    public static boolean isTrue(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        if (value instanceof String) return !((String) value).isEmpty();
        return true;
    }

    /**
     * 判断两个对象是否相等
     *
     * @param a 第一个对象
     * @param b 第二个对象
     * @return 是否相等
     */
    public static boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue()) == 0;
        } else {
            return a.equals(b);
        }
    }

    /**
     * 对两个操作数进行加法运算
     * 如果两个操作数都是数字类型，则进行数字加法
     * 否则将两个操作数转换为字符串后进行拼接
     *
     * @param a 第一个操作数
     * @param b 第二个操作数
     * @return 运算结果
     */
    public static Object add(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            return addNumbers((Number) a, (Number) b);
        } else {
            return String.valueOf(a) + b;
        }
    }

    /**
     * 对两个数字进行加法运算
     * 根据操作数类型自动选择合适的数值类型进行计算
     * 当发生溢出时会进行溢出处理
     *
     * @param a 第一个数字
     * @param b 第二个数字
     * @return 加法运算结果
     */
    public static Number addNumbers(Number a, Number b) {
        try {
            switch (getCommonType(a, b)) {
                case DOUBLE: return a.doubleValue() + b.doubleValue();
                case FLOAT:  return a.floatValue() + b.floatValue();
                case LONG:   return java.lang.Math.addExact(a.longValue(), b.longValue());
                default:     return java.lang.Math.addExact(a.intValue(), b.intValue());
            }
        } catch (ArithmeticException e) {
            return handleOverflow(a, b, '+');
        }
    }

    /**
     * 对两个操作数进行减法运算
     *
     * @param a 被减数
     * @param b 减数
     * @return 减法运算结果
     * @throws OperationException 当操作数不是数字类型时抛出异常
     */
    public static Object subtract(Object a, Object b) {
        checkNumberOperands(a, b);
        return subtractNumbers((Number) a, (Number) b);
    }

    /**
     * 对两个数字进行减法运算
     * 根据操作数类型自动选择合适的数值类型进行计算
     * 当发生溢出时会进行溢出处理
     *
     * @param a 被减数
     * @param b 减数
     * @return 减法运算结果
     */
    public static Number subtractNumbers(Number a, Number b) {
        try {
            switch (getCommonType(a, b)) {
                case DOUBLE: return a.doubleValue() - b.doubleValue();
                case FLOAT:  return a.floatValue() - b.floatValue();
                case LONG:   return java.lang.Math.subtractExact(a.longValue(), b.longValue());
                default:     return java.lang.Math.subtractExact(a.intValue(), b.intValue());
            }
        } catch (ArithmeticException e) {
            return handleOverflow(a, b, '-');
        }
    }

    /**
     * 对两个操作数进行乘法运算
     *
     * @param a 第一个操作数
     * @param b 第二个操作数
     * @return 乘法运算结果
     * @throws OperationException 当操作数不是数字类型时抛出异常
     */
    public static Object multiply(Object a, Object b) {
        checkNumberOperands(a, b);
        return multiplyNumbers((Number) a, (Number) b);
    }

    /**
     * 对两个数字进行乘法运算
     * 根据操作数类型自动选择合适的数值类型进行计算
     * 当发生溢出时会进行溢出处理
     *
     * @param a 第一个数字
     * @param b 第二个数字
     * @return 乘法运算结果
     */
    public static Number multiplyNumbers(Number a, Number b) {
        try {
            switch (getCommonType(a, b)) {
                case DOUBLE: return a.doubleValue() * b.doubleValue();
                case FLOAT:  return a.floatValue() * b.floatValue();
                case LONG:   return java.lang.Math.multiplyExact(a.longValue(), b.longValue());
                default:     return java.lang.Math.multiplyExact(a.intValue(), b.intValue());
            }
        } catch (ArithmeticException e) {
            return handleOverflow(a, b, '*');
        }
    }

    /**
     * 对两个操作数进行除法运算
     *
     * @param a 被除数
     * @param b 除数
     * @return 除法运算结果
     * @throws OperationException 当操作数不是数字类型时抛出异常
     * @throws ArithmeticException 当除数为 0 时抛出异常
     */
    public static Object divide(Object a, Object b) {
        checkNumberOperands(a, b);
        return divideNumbers((Number) a, (Number) b);
    }

    public static Number divideNumbers(Number a, Number b) {
        double divisor = b.doubleValue();
        if (divisor == 0) throw new ArithmeticException("Division by zero");
        // 浮点类型直接运算
        if (isFloating(a) || isFloating(b)) {
            return a.doubleValue() / divisor;
        }
        // 整数除法优化
        long la = a.longValue(), lb = b.longValue();
        if (la % lb == 0) {
            return tryPreserveIntType(la / lb, a, b);
        }
        return (double)la / divisor;
    }

    /**
     * 对两个操作数进行取模运算
     *
     * @param a 被除数
     * @param b 除数
     * @return 取模运算结果
     * @throws OperationException 当操作数不是数字类型时抛出异常
     * @throws ArithmeticException 当除数为 0 时抛出异常
     */
    public static Object modulo(Object a, Object b) {
        checkNumberOperands(a, b);
        return moduloNumbers((Number) a, (Number) b);
    }

    public static Number moduloNumbers(Number a, Number b) {
        double divisor = b.doubleValue();
        if (divisor == 0) throw new ArithmeticException("Modulo by zero");
        switch (getCommonType(a, b)) {
            case DOUBLE: return a.doubleValue() % divisor;
            case FLOAT:  return a.floatValue() % b.floatValue();
            case LONG:   return a.longValue() % b.longValue();
            default:     return a.intValue() % b.intValue();
        }
    }

    /**
     * 比较两个数字是否满足大于关系
     *
     * @param a 第一个操作数
     * @param b 第二个操作数
     * @return 如果 a > b 返回 true，否则返回 false
     */
    public static boolean isGreater(Object a, Object b) {
        return compare(a, b) > 0;
    }

    /**
     * 比较两个数字是否满足大于等于关系
     *
     * @param a 第一个操作数
     * @param b 第二个操作数
     * @return 如果 a >= b 返回 true，否则返回 false
     */
    public static boolean isGreaterEqual(Object a, Object b) {
        return compare(a, b) >= 0;
    }

    /**
     * 比较两个数字是否满足小于关系
     *
     * @param a 第一个操作数
     * @param b 第二个操作数
     * @return 如果 a < b 返回 true，否则返回 false
     */
    public static boolean isLess(Object a, Object b) {
        return compare(a, b) < 0;
    }

    /**
     * 比较两个数字是否满足小于等于关系
     *
     * @param a 第一个操作数
     * @param b 第二个操作数
     * @return 如果 a <= b 返回 true，否则返回 false
     */
    public static boolean isLessEqual(Object a, Object b) {
        return compare(a, b) <= 0;
    }

    /**
     * 比较两个数字的大小
     *
     * @param a 第一个操作数
     * @param b 第二个操作数
     * @return 如果 a > b 返回正数，a < b 返回负数，a = b 返回 0
     * @throws OperationException 当操作数不是数字类型时抛出异常
     */
    public static int compare(Object a, Object b) {
        checkNumberOperands(a, b);
        return compareNumbers((Number) a, (Number) b);
    }

    public static int compareNumbers(Number a, Number b) {
        NumType ta = getType(a), tb = getType(b);
        if (ta == tb) {  // 同类数值快速比较
            switch (ta) {
                case DOUBLE: return Double.compare(a.doubleValue(), b.doubleValue());
                case FLOAT:  return Float.compare(a.floatValue(), b.floatValue());
                case LONG:   return Long.compare(a.longValue(), b.longValue());
                default:     return Integer.compare(a.intValue(), b.intValue());
            }
        }
        // 混合类型比较
        if (isFloating(a) || isFloating(b)) {
            return Double.compare(a.doubleValue(), b.doubleValue());
        }
        return Long.compare(a.longValue(), b.longValue());
    }

    /**
     * 对数字进行取反操作
     * 根据操作数类型自动选择合适的数值类型进行计算
     * 当发生溢出时会进行溢出处理
     *
     * @param n 要取反的数字
     * @return 取反后的结果
     */
    public static Number negateNumber(Number n) {
        try {
            switch (getType(n)) {
                case DOUBLE: return -n.doubleValue();
                case FLOAT:  return -n.floatValue();
                case LONG:   return java.lang.Math.negateExact(n.longValue());
                default:     return java.lang.Math.negateExact(n.intValue());
            }
        } catch (ArithmeticException e) {
            // 溢出时统一升级到 double 计算
            return -n.doubleValue();
        }
    }

    // 类型优先级枚举
    private enum NumType { INTEGER, LONG, FLOAT, DOUBLE }

    // 快速类型判断（避免多次instanceof）
    private static NumType getType(Number n) {
        if (n instanceof Integer || n instanceof Short || n instanceof Byte) return NumType.INTEGER;
        if (n instanceof Long) return NumType.LONG;
        if (n instanceof Float) return NumType.FLOAT;
        if (n instanceof Double) return NumType.DOUBLE;
        throw new IllegalArgumentException("Unsupported number type: " + n.getClass());
    }

    // 获取共同运算类型（快速路径）
    private static NumType getCommonType(Number a, Number b) {
        NumType ta = getType(a), tb = getType(b);
        return ta.ordinal() > tb.ordinal() ? ta : tb;
    }

    // 判断是否浮点类型
    private static boolean isFloating(Number n) {
        return n instanceof Double || n instanceof Float;
    }

    // 溢出处理
    private static Number handleOverflow(Number a, Number b, char op) {
        // 溢出时统一升级到 double 计算
        double da = a.doubleValue(), db = b.doubleValue();
        switch (op) {
            case '+': return da + db;
            case '-': return da - db;
            case '*': return da * db;
            default:  throw new ArithmeticException("Arithmetic overflow");
        }
    }

    // 整数类型溢出处理
    private static Number tryPreserveIntType(long result, Number originalA, Number originalB) {
        // 原始类型均为整数类型时尝试返回int
        if (!isFloating(originalA) && !isFloating(originalB)) {
            if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
                return (int)result;
            }
        }
        return result;
    }
}
// @formatter:on