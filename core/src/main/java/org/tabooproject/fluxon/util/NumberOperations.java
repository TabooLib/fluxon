package org.tabooproject.fluxon.util;

// @formatter:off
public final class NumberOperations {

    public static Number addNumbers(Number a, Number b) {
        try {
            switch (getCommonType(a, b)) {
                case DOUBLE: return a.doubleValue() + b.doubleValue();
                case FLOAT:  return a.floatValue() + b.floatValue();
                case LONG:    return Math.addExact(a.longValue(), b.longValue());
                default:      return Math.addExact(a.intValue(), b.intValue());
            }
        } catch (ArithmeticException e) {
            return handleOverflow(a, b, '+');
        }
    }

    public static Number subtractNumbers(Number a, Number b) {
        try {
            switch (getCommonType(a, b)) {
                case DOUBLE: return a.doubleValue() - b.doubleValue();
                case FLOAT:  return a.floatValue() - b.floatValue();
                case LONG:   return Math.subtractExact(a.longValue(), b.longValue());
                default:     return Math.subtractExact(a.intValue(), b.intValue());
            }
        } catch (ArithmeticException e) {
            return handleOverflow(a, b, '-');
        }
    }

    public static Number multiplyNumbers(Number a, Number b) {
        try {
            switch (getCommonType(a, b)) {
                case DOUBLE: return a.doubleValue() * b.doubleValue();
                case FLOAT:  return a.floatValue() * b.floatValue();
                case LONG:   return Math.multiplyExact(a.longValue(), b.longValue());
                default:     return Math.multiplyExact(a.intValue(), b.intValue());
            }
        } catch (ArithmeticException e) {
            return handleOverflow(a, b, '*');
        }
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

    public static Number negateNumber(Number n) {
        try {
            switch (getType(n)) {
                case DOUBLE: return -n.doubleValue();
                case FLOAT:  return -n.floatValue();
                case LONG:   return Math.negateExact(n.longValue());
                default:     return Math.negateExact(n.intValue());
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