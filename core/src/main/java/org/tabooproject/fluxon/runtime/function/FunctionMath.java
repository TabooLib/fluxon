package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.util.List;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class FunctionMath {

    public static void init(FluxonRuntime runtime) {
        // 数学常数
        runtime.registerVariable("PI", Math.PI);
        runtime.registerVariable("E", Math.E);

        // min - 保持整数类型如果两个参数都是整数
        runtime.registerFunction("min", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT), ctx -> {
            Object a = ctx.getArgBoxed(0);
            Object b = ctx.getArgBoxed(1);
            if (a instanceof Long || b instanceof Long) {
                ctx.setReturnRef(Math.min(toLong(a), toLong(b)));
            } else if (a instanceof Double || b instanceof Double || a instanceof Float || b instanceof Float) {
                ctx.setReturnRef(Math.min(toDouble(a), toDouble(b)));
            } else {
                ctx.setReturnRef(Math.min(toInt(a), toInt(b)));
            }
        });

        // max - 保持整数类型如果两个参数都是整数
        runtime.registerFunction("max", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT), ctx -> {
            Object a = ctx.getArgBoxed(0);
            Object b = ctx.getArgBoxed(1);
            if (a instanceof Long || b instanceof Long) {
                ctx.setReturnRef(Math.max(toLong(a), toLong(b)));
            } else if (a instanceof Double || b instanceof Double || a instanceof Float || b instanceof Float) {
                ctx.setReturnRef(Math.max(toDouble(a), toDouble(b)));
            } else {
                ctx.setReturnRef(Math.max(toInt(a), toInt(b)));
            }
        });

        // clamp
        runtime.registerFunction("clamp", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT, Type.OBJECT), ctx -> {
            Object value = ctx.getArgBoxed(0);
            Object min = ctx.getArgBoxed(1);
            Object max = ctx.getArgBoxed(2);
            if (value instanceof Long || min instanceof Long || max instanceof Long) {
                ctx.setReturnRef(Math.max(toLong(min), Math.min(toLong(value), toLong(max))));
            } else if (value instanceof Double || min instanceof Double || max instanceof Double) {
                ctx.setReturnRef(Math.max(toDouble(min), Math.min(toDouble(value), toDouble(max))));
            } else {
                ctx.setReturnRef(Math.max(toInt(min), Math.min(toInt(value), toInt(max))));
            }
        });

        // abs
        runtime.registerFunction("abs", returns(Type.OBJECT).params(Type.OBJECT), ctx -> {
            Object value = ctx.getArgBoxed(0);
            if (value instanceof Long) {
                ctx.setReturnRef(Math.abs((Long) value));
            } else if (value instanceof Double) {
                ctx.setReturnRef(Math.abs((Double) value));
            } else if (value instanceof Float) {
                ctx.setReturnRef(Math.abs((Float) value));
            } else {
                ctx.setReturnRef(Math.abs(toInt(value)));
            }
        });

        // round
        runtime.registerFunction("round", returns(Type.OBJECT).params(Type.OBJECT), ctx -> {
            Object value = ctx.getArgBoxed(0);
            ctx.setReturnRef(Math.round(toDouble(value)));
        });

        // floor/ceil
        runtime.registerFunction("floor", returns(Type.D).params(Type.OBJECT), ctx -> {
            ctx.setReturnDouble(Math.floor(toDouble(ctx.getArgBoxed(0))));
        });
        runtime.registerFunction("ceil", returns(Type.D).params(Type.OBJECT), ctx -> {
            ctx.setReturnDouble(Math.ceil(toDouble(ctx.getArgBoxed(0))));
        });

        // 三角函数
        runtime.registerFunction("sin", returns(Type.D).params(Type.OBJECT), ctx -> ctx.setReturnDouble(Math.sin(toDouble(ctx.getArgBoxed(0)))));
        runtime.registerFunction("cos", returns(Type.D).params(Type.OBJECT), ctx -> ctx.setReturnDouble(Math.cos(toDouble(ctx.getArgBoxed(0)))));
        runtime.registerFunction("tan", returns(Type.D).params(Type.OBJECT), ctx -> ctx.setReturnDouble(Math.tan(toDouble(ctx.getArgBoxed(0)))));
        runtime.registerFunction("asin", returns(Type.D).params(Type.OBJECT), ctx -> {
            double value = toDouble(ctx.getArgBoxed(0));
            validateRange(value, -1.0, 1.0, "asin input must be between -1 and 1");
            ctx.setReturnDouble(Math.asin(value));
        });
        runtime.registerFunction("acos", returns(Type.D).params(Type.OBJECT), ctx -> {
            double value = toDouble(ctx.getArgBoxed(0));
            validateRange(value, -1.0, 1.0, "acos input must be between -1 and 1");
            ctx.setReturnDouble(Math.acos(value));
        });
        runtime.registerFunction("atan", returns(Type.D).params(Type.OBJECT), ctx -> ctx.setReturnDouble(Math.atan(toDouble(ctx.getArgBoxed(0)))));

        // 指数与对数
        runtime.registerFunction("exp", returns(Type.D).params(Type.OBJECT), ctx -> ctx.setReturnDouble(Math.exp(toDouble(ctx.getArgBoxed(0)))));
        runtime.registerFunction("log", returns(Type.D).params(Type.OBJECT), ctx -> {
            double value = toDouble(ctx.getArgBoxed(0));
            validatePositive(value, "log input must be positive");
            ctx.setReturnDouble(Math.log(value));
        });

        // pow
        runtime.registerFunction("pow", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT), ctx -> {
            Object base = ctx.getArgBoxed(0);
            Object exp = ctx.getArgBoxed(1);
            double result = Math.pow(toDouble(base), toDouble(exp));
            // 如果结果是整数且两个参数都是整数类型，返回整数
            if (result == Math.floor(result) && !(base instanceof Double) && !(base instanceof Float)
                    && !(exp instanceof Double) && !(exp instanceof Float)) {
                if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
                    ctx.setReturnRef((int) result);
                } else {
                    ctx.setReturnRef((long) result);
                }
            } else {
                ctx.setReturnRef(result);
            }
        });

        runtime.registerFunction("sqrt", returns(Type.D).params(Type.OBJECT), ctx -> {
            double value = toDouble(ctx.getArgBoxed(0));
            validatePositive(value, "Cannot take square root of negative number");
            ctx.setReturnDouble(Math.sqrt(value));
        });

        // 随机数生成函数 - 支持 0/1/2 个参数
        runtime.registerFunction("random", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT), ctx -> {
            int argCount = ctx.getArgumentCount();
            if (argCount == 0) {
                ctx.setReturnDouble(Math.random());
            } else if (argCount == 1) {
                Object end = ctx.getArgBoxed(0);
                if (end instanceof Double || end instanceof Float) {
                    double endVal = toDouble(end);
                    if (endVal <= 0) throw new IllegalArgumentException("random end value must be positive");
                    ctx.setReturnDouble(Math.random() * endVal);
                } else {
                    int endVal = toInt(end);
                    if (endVal <= 0) throw new IllegalArgumentException("random end value must be positive");
                    ctx.setReturnRef((int) (Math.random() * endVal));
                }
            } else {
                Object start = ctx.getArgBoxed(0);
                Object end = ctx.getArgBoxed(1);
                if (start instanceof Double || start instanceof Float || end instanceof Double || end instanceof Float) {
                    double startVal = toDouble(start);
                    double endVal = toDouble(end);
                    if (startVal >= endVal) throw new IllegalArgumentException("random start value must be less than end value");
                    ctx.setReturnDouble(startVal + Math.random() * (endVal - startVal));
                } else {
                    int startVal = toInt(start);
                    int endVal = toInt(end);
                    if (startVal >= endVal) throw new IllegalArgumentException("random start value must be less than end value");
                    ctx.setReturnRef(startVal + (int) (Math.random() * (endVal - startVal)));
                }
            }
        });

        // 角度与弧度转换
        runtime.registerFunction("rad", returns(Type.D).params(Type.OBJECT), ctx -> ctx.setReturnDouble(Math.toRadians(toDouble(ctx.getArgBoxed(0)))));
        runtime.registerFunction("deg", returns(Type.D).params(Type.OBJECT), ctx -> ctx.setReturnDouble(Math.toDegrees(toDouble(ctx.getArgBoxed(0)))));

        // 插值
        runtime.registerFunction("lerp", returns(Type.D).params(Type.OBJECT, Type.OBJECT, Type.OBJECT), ctx -> {
            double start = toDouble(ctx.getArgBoxed(0));
            double end = toDouble(ctx.getArgBoxed(1));
            double t = toDouble(ctx.getArgBoxed(2));
            ctx.setReturnDouble(start + (end - start) * t);
        });
    }

    private static int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new IllegalArgumentException("Expected number but got: " + (value == null ? "null" : value.getClass().getName()));
    }

    private static long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new IllegalArgumentException("Expected number but got: " + (value == null ? "null" : value.getClass().getName()));
    }

    private static double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new IllegalArgumentException("Expected number but got: " + (value == null ? "null" : value.getClass().getName()));
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
}
