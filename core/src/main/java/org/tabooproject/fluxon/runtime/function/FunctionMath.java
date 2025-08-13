package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Operations;

import java.util.Arrays;

public class FunctionMath {

    public static void init(FluxonRuntime runtime) {
        // 数学常数
        runtime.registerVariable("PI", Math.PI);
        runtime.registerVariable("E", Math.E);

        // 最大最小值
        runtime.registerFunction("min", 2, (context) -> {
            Object[] args = context.getArguments();
            if (Operations.compare(args[0], args[1]) < 0) {
                return args[0];
            }
            return args[1];
        });
        runtime.registerFunction("max", 2, (context) -> {
            Object[] args = context.getArguments();
            if (Operations.compare(args[0], args[1]) > 0) {
                return args[0];
            }
            return args[1];
        });
        runtime.registerFunction("clamp", 3, (context) -> {
            Object[] args = context.getArguments();
            Number num = validateAndGetNumber(args[0]);
            Number min = validateAndGetNumber(args[1]);
            Number max = validateAndGetNumber(args[2]);
            double clamped = Math.max(min.doubleValue(), Math.min(num.doubleValue(), max.doubleValue()));
            // 尝试保持原始类型
            if (num instanceof Integer && min instanceof Integer && max instanceof Integer) {
                return (int) clamped;
            } else if (num instanceof Long && min instanceof Long && max instanceof Long) {
                return (long) clamped;
            } else if (num instanceof Float && min instanceof Float && max instanceof Float) {
                return (float) clamped;
            }
            return clamped;
        });

        // 绝对值
        runtime.registerFunction("abs", 1, (context) -> {
            Object[] args = context.getArguments();
            Number num = validateAndGetNumber(args[0]);
            double value = num.doubleValue();
            double result = Math.abs(value);
            // 尝试保持原始类型
            if (num instanceof Integer) {
                int intValue = num.intValue();
                if (intValue != Integer.MIN_VALUE) {
                    return Math.abs(intValue);
                }
            } else if (num instanceof Long) {
                long longValue = num.longValue();
                if (longValue != Long.MIN_VALUE) {
                    return Math.abs(longValue);
                }
            } else if (num instanceof Float) {
                return (float) result;
            }
            return result;
        });

        // 取整函数
        runtime.registerFunction("round", 1, (context) -> {
            Object[] args = context.getArguments();
            Number num = validateAndGetNumber(args[0]);
            long result = Math.round(num.doubleValue());
            return preserveIntegerType(result);
        });
        runtime.registerFunction("floor", 1, (context) -> {
            Object[] args = context.getArguments();
            Number num = validateAndGetNumber(args[0]);
            double result = Math.floor(num.doubleValue());
            return preserveIntegerTypeFromDouble(result);
        });
        runtime.registerFunction("ceil", 1, (context) -> {
            Object[] args = context.getArguments();
            Number num = validateAndGetNumber(args[0]);
            double result = Math.ceil(num.doubleValue());
            return preserveIntegerTypeFromDouble(result);
        });

        // 三角函数
        runtime.registerFunction("sin", 1, (context) -> Math.sin(validateAndGetNumber(context.getArguments()[0]).doubleValue()));
        runtime.registerFunction("cos", 1, (context) -> Math.cos(validateAndGetNumber(context.getArguments()[0]).doubleValue()));
        runtime.registerFunction("tan", 1, (context) -> Math.tan(validateAndGetNumber(context.getArguments()[0]).doubleValue()));
        runtime.registerFunction("asin", 1, (context) -> {
            Object[] args = context.getArguments();
            double value = validateAndGetNumber(args[0]).doubleValue();
            validateRange(value, -1.0, 1.0, "asin input must be between -1 and 1");
            return Math.asin(value);
        });
        runtime.registerFunction("acos", 1, (context) -> {
            Object[] args = context.getArguments();
            double value = validateAndGetNumber(args[0]).doubleValue();
            validateRange(value, -1.0, 1.0, "acos input must be between -1 and 1");
            return Math.acos(value);
        });
        runtime.registerFunction("atan", 1, (context) -> Math.atan(validateAndGetNumber(context.getArguments()[0]).doubleValue()));

        // 指数与对数
        runtime.registerFunction("exp", 1, (context) -> Math.exp(validateAndGetNumber(context.getArguments()[0]).doubleValue()));
        runtime.registerFunction("log", 1, (context) -> {
            Object[] args = context.getArguments();
            double value = validateAndGetNumber(args[0]).doubleValue();
            validatePositive(value, "log input must be positive");
            return Math.log(value);
        });
        runtime.registerFunction("pow", 2, (context) -> {
            Object[] args = context.getArguments();
            Number base = validateAndGetNumber(args[0]);
            Number exponent = validateAndGetNumber(args[1]);
            double result = Math.pow(base.doubleValue(), exponent.doubleValue());
            // 如果指数是整数且结果是整数，尝试返回整数类型
            double expValue = exponent.doubleValue();
            if (expValue == Math.rint(expValue) && result == Math.rint(result) && !Double.isInfinite(result)) {
                return preserveIntegerTypeFromDouble(result);
            }
            return result;
        });
        runtime.registerFunction("sqrt", 1, (context) -> {
            Object[] args = context.getArguments();
            double value = validateAndGetNumber(args[0]).doubleValue();
            validatePositive(value, "Cannot take square root of negative number");
            return Math.sqrt(value);
        });

        // 随机数生成函数
        // random() / random(end) / random(start, end)
        runtime.registerFunction("random", Arrays.asList(0, 1, 2),(context) -> {
            Object[] args = context.getArguments();
            switch (args.length) {
                case 0:
                    return Math.random();
                case 1:
                    return generateRandomSingle(args[0]);
                case 2:
                    return generateRandomRange(args[0], args[1]);
                default:
                    throw new IllegalArgumentException("random function accepts 0, 1, or 2 arguments, got " + args.length);
            }
        });

        // 角度与弧度转换
        runtime.registerFunction("rad", 1, (context) -> Math.toRadians(validateAndGetNumber(context.getArguments()[0]).doubleValue()));
        runtime.registerFunction("deg", 1, (context) -> Math.toDegrees(validateAndGetNumber(context.getArguments()[0]).doubleValue()));

        // 插值
        runtime.registerFunction("lerp", 3, (context) -> {
            Object[] args = context.getArguments();
            Number start = validateAndGetNumber(args[0]);
            Number end = validateAndGetNumber(args[1]);
            Number t = validateAndGetNumber(args[2]);
            return start.doubleValue() + (end.doubleValue() - start.doubleValue()) * t.doubleValue();
        });
    }

    /**
     * 验证参数是数字并返回 Number 对象
     */
    private static Number validateAndGetNumber(Object arg) {
        Operations.checkNumberOperand(arg);
        return (Number) arg;
    }

    /**
     * 验证数值为正数
     */
    private static void validatePositive(double value, String message) {
        if (value <= 0) {
            throw new ArithmeticException(message);
        }
    }

    /**
     * 验证数值在指定范围内
     */
    private static void validateRange(double value, double min, double max, String message) {
        if (value < min || value > max) {
            throw new ArithmeticException(message);
        }
    }

    /**
     * 判断数字是否为整数类型
     */
    private static boolean isIntegerType(Number n) {
        return n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte;
    }

    /**
     * 尝试将 long 结果保持为整数类型
     */
    private static Number preserveIntegerType(long result) {
        if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
            return (int) result;
        }
        return result;
    }

    /**
     * 尝试将 double 结果保持为整数类型（如果是整数值）
     */
    private static Number preserveIntegerTypeFromDouble(double result) {
        if (result == Math.rint(result) && !Double.isInfinite(result)) {
            long longResult = (long) result;
            return preserveIntegerType(longResult);
        }
        return result;
    }

    /**
     * 尝试保持两个数字的共同类型
     */
    private static Number preserveCommonType(Number result, Number original1, Number original2) {
        if (original1 instanceof Integer && original2 instanceof Integer) {
            long longResult = result.longValue();
            if (longResult >= Integer.MIN_VALUE && longResult <= Integer.MAX_VALUE && longResult == result.doubleValue()) {
                return (int) longResult;
            }
        }
        if (original1 instanceof Float && original2 instanceof Float) {
            return result.floatValue();
        }
        return result;
    }

    /**
     * 根据原始类型保持结果类型
     */
    private static Number preserveOriginalType(Number result, Number original) {
        if (original instanceof Integer) {
            long longResult = result.longValue();
            if (longResult >= Integer.MIN_VALUE && longResult <= Integer.MAX_VALUE && longResult == result.doubleValue()) {
                return (int) longResult;
            }
        } else if (original instanceof Float) {
            return result.floatValue();
        }
        return result;
    }

    /**
     * 生成单参数随机数 random(end)
     */
    private static Number generateRandomSingle(Object endArg) {
        Number end = validateAndGetNumber(endArg);
        if (isIntegerType(end)) {
            long endValue = end.longValue();
            if (endValue <= 0) {
                throw new IllegalArgumentException("random end value must be positive");
            }
            long result = (long) (Math.random() * endValue);
            return preserveOriginalType(result, end);
        } else {
            double endValue = end.doubleValue();
            if (endValue <= 0.0) {
                throw new IllegalArgumentException("random end value must be positive");
            }
            double result = Math.random() * endValue;
            return preserveOriginalType(result, end);
        }
    }

    /**
     * 生成范围随机数 random(start, end)
     */
    private static Number generateRandomRange(Object startArg, Object endArg) {
        Number start = validateAndGetNumber(startArg);
        Number end = validateAndGetNumber(endArg);
        // 检查范围有效性
        if (Operations.compare(start, end) >= 0) {
            throw new IllegalArgumentException("random start value must be less than end value");
        }
        if (isIntegerType(start) && isIntegerType(end)) {
            long startValue = start.longValue();
            long endValue = end.longValue();
            long range = endValue - startValue;
            long result = startValue + (long) (Math.random() * range);
            return preserveCommonType(result, start, end);
        } else {
            double startValue = start.doubleValue();
            double endValue = end.doubleValue();
            double range = endValue - startValue;
            double result = startValue + Math.random() * range;
            return preserveCommonType(result, start, end);
        }
    }
}
