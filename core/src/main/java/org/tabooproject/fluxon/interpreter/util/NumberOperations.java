package org.tabooproject.fluxon.interpreter.util;

/**
 * 数值操作辅助类
 * 提供各种数值运算功能
 */
public class NumberOperations {
    /**
     * 加法运算
     *
     * @param a 第一个操作数
     * @param b 第二个操作数
     * @return 加法结果
     */
    public static Object addNumbers(Object a, Object b) {
        if (!(a instanceof Number) || !(b instanceof Number)) {
            throw new RuntimeException("Operands for addition must be numbers: " + a + " + " + b);
        }
        
        // 按照类型优先级判断，返回对应类型结果
        if (a instanceof Double || b instanceof Double) {
            return ((Number) a).doubleValue() + ((Number) b).doubleValue();
        } else if (a instanceof Float || b instanceof Float) {
            return ((Number) a).floatValue() + ((Number) b).floatValue();
        } else if (a instanceof Long || b instanceof Long) {
            return ((Number) a).longValue() + ((Number) b).longValue();
        } else {
            // 处理 Integer 加法并检测溢出
            try {
                return Math.addExact(((Number) a).intValue(), ((Number) b).intValue());
            } catch (ArithmeticException e) {
                // 发生溢出，升级到 Long
                return ((Number) a).longValue() + ((Number) b).longValue();
            }
        }
    }

    /**
     * 减法运算
     *
     * @param a 第一个操作数
     * @param b 第二个操作数
     * @return 减法结果
     */
    public static Object subtractNumbers(Object a, Object b) {
        if (!(a instanceof Number) || !(b instanceof Number)) {
            throw new RuntimeException("Operands for subtraction must be numbers: " + a + " - " + b);
        }
        
        if (a instanceof Double || b instanceof Double) {
            return ((Number) a).doubleValue() - ((Number) b).doubleValue();
        } else if (a instanceof Float || b instanceof Float) {
            return ((Number) a).floatValue() - ((Number) b).floatValue();
        } else if (a instanceof Long || b instanceof Long) {
            return ((Number) a).longValue() - ((Number) b).longValue();
        } else {
            // 处理 Integer 减法并检测溢出
            try {
                return Math.subtractExact(((Number) a).intValue(), ((Number) b).intValue());
            } catch (ArithmeticException e) {
                // 发生溢出，升级到 Long
                return ((Number) a).longValue() - ((Number) b).longValue();
            }
        }
    }

    /**
     * 乘法运算
     *
     * @param a 第一个操作数
     * @param b 第二个操作数
     * @return 乘法结果
     */
    public static Object multiplyNumbers(Object a, Object b) {
        if (!(a instanceof Number) || !(b instanceof Number)) {
            throw new RuntimeException("Operands for multiplication must be numbers: " + a + " * " + b);
        }
        
        if (a instanceof Double || b instanceof Double) {
            return ((Number) a).doubleValue() * ((Number) b).doubleValue();
        } else if (a instanceof Float || b instanceof Float) {
            return ((Number) a).floatValue() * ((Number) b).floatValue();
        } else if (a instanceof Long || b instanceof Long) {
            try {
                return Math.multiplyExact(((Number) a).longValue(), ((Number) b).longValue());
            } catch (ArithmeticException e) {
                // Long 乘法溢出，升级到 Double
                return ((Number) a).doubleValue() * ((Number) b).doubleValue();
            }
        } else {
            // 处理 Integer 乘法并检测溢出
            try {
                return Math.multiplyExact(((Number) a).intValue(), ((Number) b).intValue());
            } catch (ArithmeticException e) {
                // 发生溢出，尝试升级到 Long
                try {
                    return Math.multiplyExact(((Number) a).longValue(), ((Number) b).longValue());
                } catch (ArithmeticException e2) {
                    // Long 也溢出，升级到 Double
                    return ((Number) a).doubleValue() * ((Number) b).doubleValue();
                }
            }
        }
    }

    /**
     * 除法运算
     *
     * @param a 第一个操作数
     * @param b 第二个操作数
     * @return 除法结果
     */
    public static Object divideNumbers(Object a, Object b) {
        if (!(a instanceof Number) || !(b instanceof Number)) {
            throw new RuntimeException("Operands for division must be numbers: " + a + " / " + b);
        }
        
        // 检查除数是否为零
        double bValue = ((Number) b).doubleValue();
        if (bValue == 0) {
            throw new RuntimeException("Divisor cannot be zero");
        }
        
        // 如果操作数中有浮点类型，返回浮点结果
        if (a instanceof Double || b instanceof Double) {
            return ((Number) a).doubleValue() / ((Number) b).doubleValue();
        } else if (a instanceof Float || b instanceof Float) {
            return ((Number) a).floatValue() / ((Number) b).floatValue();
        } else {
            // 处理整数除法
            long aLong = ((Number) a).longValue();
            long bLong = ((Number) b).longValue();
            
            // 检查是否能整除
            if (aLong % bLong == 0) {
                long result = aLong / bLong;
                // 根据原始操作数类型确定返回类型
                if (a instanceof Integer && b instanceof Integer && result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
                    return (int) result;
                }
                return result;
            } else {
                // 不能整除，返回浮点结果
                // 优先使用 double 避免精度问题
                return ((Number) a).doubleValue() / ((Number) b).doubleValue();
            }
        }
    }

    /**
     * 取模运算
     *
     * @param a 第一个操作数
     * @param b 第二个操作数
     * @return 取模结果
     */
    public static Object moduloNumbers(Object a, Object b) {
        if (!(a instanceof Number) || !(b instanceof Number)) {
            throw new RuntimeException("Operands for modulo operation must be numbers: " + a + " % " + b);
        }
        
        // 检查模数是否为零
        double bValue = ((Number) b).doubleValue();
        if (bValue == 0) {
            throw new RuntimeException("Modulus cannot be zero");
        }
        
        // 按照类型优先级处理
        if (a instanceof Double || b instanceof Double) {
            return ((Number) a).doubleValue() % ((Number) b).doubleValue();
        } else if (a instanceof Float || b instanceof Float) {
            return ((Number) a).floatValue() % ((Number) b).floatValue();
        } else if (a instanceof Long || b instanceof Long) {
            return ((Number) a).longValue() % ((Number) b).longValue();
        } else {
            return ((Number) a).intValue() % ((Number) b).intValue();
        }
    }

    /**
     * 数值比较
     *
     * @param a 第一个操作数
     * @param b 第二个操作数
     * @return 比较结果，-1表示小于，0表示等于，1表示大于
     */
    public static int compareNumbers(Object a, Object b) {
        if (!(a instanceof Number) || !(b instanceof Number)) {
            throw new RuntimeException("Operands for comparison must be numbers: " + a + " and " + b);
        }
        // 使用 Double.compare 进行统一比较，避免精度问题
        return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
    }

    /**
     * 数值取负
     *
     * @param value 操作数
     * @return 取负结果
     */
    public static Object negateNumber(Object value) {
        if (!(value instanceof Number)) {
            throw new RuntimeException("Cannot apply negation to non-numeric type: " + value);
        }
        
        // 根据原始类型返回相应类型的结果
        if (value instanceof Integer) {
            try {
                return Math.negateExact((Integer) value);
            } catch (ArithmeticException e) {
                // 处理 Integer.MIN_VALUE 的特殊情况
                return -((Number) value).longValue();
            }
        } else if (value instanceof Long) {
            try {
                return Math.negateExact((Long) value);
            } catch (ArithmeticException e) {
                // 处理 Long.MIN_VALUE 的特殊情况
                return -((Number) value).doubleValue();
            }
        } else if (value instanceof Float) {
            return -((Float) value);
        } else if (value instanceof Double) {
            return -((Double) value);
        } else {
            // 其他 Number 类型
            return -((Number) value).doubleValue();
        }
    }
} 