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
        runtime.registerFunction("min", 2, context -> {
            Number num0 = (Number) context.getRef(0);
            Number num1 = (Number) context.getRef(1);
            context.setReturnRef(Operations.compareNumbers(num0, num1) < 0 ? num0 : num1);
        });
        runtime.registerFunction("max", 2, context -> {
            Number num0 = (Number) context.getRef(0);
            Number num1 = (Number) context.getRef(1);
            context.setReturnRef(Operations.compareNumbers(num0, num1) > 0 ? num0 : num1);
        });
        runtime.registerFunction("clamp", 3, context -> {
            Number num = (Number) context.getRef(0);
            Number min = (Number) context.getRef(1);
            Number max = (Number) context.getRef(2);
            double clamped = Math.max(min.doubleValue(), Math.min(num.doubleValue(), max.doubleValue()));
            if (num instanceof Integer && min instanceof Integer && max instanceof Integer) {
                context.setReturnRef((int) clamped);
            } else if (num instanceof Long && min instanceof Long && max instanceof Long) {
                context.setReturnRef((long) clamped);
            } else if (num instanceof Float && min instanceof Float && max instanceof Float) {
                context.setReturnRef((float) clamped);
            } else {
                context.setReturnRef(clamped);
            }
        });

        // 绝对值
        runtime.registerFunction("abs", 1, context -> {
            Number num = (Number) context.getRef(0);
            double result = Math.abs(num.doubleValue());
            if (num instanceof Integer) {
                int intValue = num.intValue();
                if (intValue != Integer.MIN_VALUE) {
                    context.setReturnRef(Math.abs(intValue));
                    return;
                }
            } else if (num instanceof Long) {
                long longValue = num.longValue();
                if (longValue != Long.MIN_VALUE) {
                    context.setReturnRef(Math.abs(longValue));
                    return;
                }
            } else if (num instanceof Float) {
                context.setReturnRef((float) result);
                return;
            }
            context.setReturnRef(result);
        });

        // 取整函数
        runtime.registerFunction("round", 1, context -> {
            Number num = (Number) context.getRef(0);
            long result = Math.round(num.doubleValue());
            context.setReturnRef(preserveIntegerType(result));
        });
        runtime.registerFunction("floor", 1, context -> {
            Number num = (Number) context.getRef(0);
            double result = Math.floor(num.doubleValue());
            context.setReturnRef(preserveIntegerTypeFromDouble(result));
        });
        runtime.registerFunction("ceil", 1, context -> {
            Number num = (Number) context.getRef(0);
            double result = Math.ceil(num.doubleValue());
            context.setReturnRef(preserveIntegerTypeFromDouble(result));
        });

        // 三角函数
        runtime.registerFunction("sin", 1, context -> context.setReturnRef(Math.sin(((Number) context.getRef(0)).doubleValue())));
        runtime.registerFunction("cos", 1, context -> context.setReturnRef(Math.cos(((Number) context.getRef(0)).doubleValue())));
        runtime.registerFunction("tan", 1, context -> context.setReturnRef(Math.tan(((Number) context.getRef(0)).doubleValue())));
        runtime.registerFunction("asin", 1, context -> {
            double value = ((Number) context.getRef(0)).doubleValue();
            validateRange(value, -1.0, 1.0, "asin input must be between -1 and 1");
            context.setReturnRef(Math.asin(value));
        });
        runtime.registerFunction("acos", 1, context -> {
            double value = ((Number) context.getRef(0)).doubleValue();
            validateRange(value, -1.0, 1.0, "acos input must be between -1 and 1");
            context.setReturnRef(Math.acos(value));
        });
        runtime.registerFunction("atan", 1, context -> context.setReturnRef(Math.atan(((Number) context.getRef(0)).doubleValue())));

        // 指数与对数
        runtime.registerFunction("exp", 1, context -> context.setReturnRef(Math.exp(((Number) context.getRef(0)).doubleValue())));
        runtime.registerFunction("log", 1, context -> {
            double value = ((Number) context.getRef(0)).doubleValue();
            validatePositive(value, "log input must be positive");
            context.setReturnRef(Math.log(value));
        });
        runtime.registerFunction("pow", 2, context -> {
            Number base = (Number) context.getRef(0);
            Number exponent = (Number) context.getRef(1);
            double result = Math.pow(base.doubleValue(), exponent.doubleValue());
            double expValue = exponent.doubleValue();
            if (expValue == Math.rint(expValue) && result == Math.rint(result) && !Double.isInfinite(result)) {
                context.setReturnRef(preserveIntegerTypeFromDouble(result));
            } else {
                context.setReturnRef(result);
            }
        });
        runtime.registerFunction("sqrt", 1, context -> {
            double value = ((Number) context.getRef(0)).doubleValue();
            validatePositive(value, "Cannot take square root of negative number");
            context.setReturnRef(Math.sqrt(value));
        });

        // 随机数生成函数
        runtime.registerFunction("random", Arrays.asList(0, 1, 2), context -> {
            int argCount = context.getArgumentCount();
            switch (argCount) {
                case 0:
                    context.setReturnRef(Math.random());
                    break;
                case 1:
                    context.setReturnRef(generateRandomSingle(context.getRef(0)));
                    break;
                case 2:
                    context.setReturnRef(generateRandomRange(context.getRef(0), context.getRef(1)));
                    break;
                default:
                    throw new IllegalArgumentException("random function accepts 0, 1, or 2 arguments, got " + argCount);
            }
        });

        // 角度与弧度转换
        runtime.registerFunction("rad", 1, context -> context.setReturnRef(Math.toRadians(((Number) context.getRef(0)).doubleValue())));
        runtime.registerFunction("deg", 1, context -> context.setReturnRef(Math.toDegrees(((Number) context.getRef(0)).doubleValue())));

        // 插值
        runtime.registerFunction("lerp", 3, context -> {
            Number start = (Number) context.getRef(0);
            Number end = (Number) context.getRef(1);
            Number t = (Number) context.getRef(2);
            context.setReturnRef(start.doubleValue() + (end.doubleValue() - start.doubleValue()) * t.doubleValue());
        });
    }

    private static Number validateAndGetNumber(Object arg) {
        Operations.checkNumberOperand(arg);
        return (Number) arg;
    }

    private static void validatePositive(double value, String message) {
        if (value <= 0) {
            throw new ArithmeticException(message);
        }
    }

    private static void validateRange(double value, double min, double max, String message) {
        if (value < min || value > max) {
            throw new ArithmeticException(message);
        }
    }

    private static boolean isIntegerType(Number n) {
        return n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte;
    }

    private static Number preserveIntegerType(long result) {
        if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
            return (int) result;
        }
        return result;
    }

    private static Number preserveIntegerTypeFromDouble(double result) {
        if (result == Math.rint(result) && !Double.isInfinite(result)) {
            long longResult = (long) result;
            return preserveIntegerType(longResult);
        }
        return result;
    }

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

    private static Number generateRandomRange(Object startArg, Object endArg) {
        Number start = validateAndGetNumber(startArg);
        Number end = validateAndGetNumber(endArg);
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
