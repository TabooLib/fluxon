package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;
import static org.tabooproject.fluxon.runtime.Type.*;

/**
 * 数学函数库
 * 所有函数通过 @FluxonFunction 注解注册，编译器可直接生成 INVOKESTATIC
 *
 * @author sky
 */
public class FunctionMath {

    public static void init(FluxonRuntime runtime) {
        // 数学常数
        runtime.registerVariable("PI", Math.PI);
        runtime.registerVariable("E", Math.E);
        // 扫描注册所有 @FluxonFunction 方法
        FluxonFunctionScanner.register(runtime, FunctionMath.class);
        // random 系列：有副作用，不适合 DirectBinding
        runtime.registerFunction("random", returns(D).noParams(), ctx -> ctx.setReturnDouble(Math.random()));
        runtime.registerFunction("random", returns(I).params(I), ctx -> {
            int end = ctx.getInt(0);
            if (end <= 0) throw new IllegalArgumentException("random " + end + " must be positive");
            ctx.setReturnInt((int) (Math.random() * end));
        });
        runtime.registerFunction("random", returns(D).params(D), ctx -> {
            double end = ctx.getDouble(0);
            if (end <= 0) throw new IllegalArgumentException("random " + end + " must be positive");
            ctx.setReturnDouble(Math.random() * end);
        });
        runtime.registerFunction("random", returns(I).params(I, I), ctx -> {
            int start = ctx.getInt(0);
            int end = ctx.getInt(1);
            if (start >= end) throw new IllegalArgumentException("random " + start + " must be less than " + end);
            ctx.setReturnInt(start + (int) (Math.random() * (end - start)));
        });
        runtime.registerFunction("random", returns(D).params(D, D), ctx -> {
            double start = ctx.getDouble(0);
            double end = ctx.getDouble(1);
            if (start >= end) throw new IllegalArgumentException("random " + start + " must be less than " + end);
            ctx.setReturnDouble(start + Math.random() * (end - start));
        });
    }

    // region 区间限制与插值

    // 组合逻辑函数（不适合单方法注解，保持手动注册）
    // 拆成类型明确的静态重载后可由 Scanner 生成 DirectBinding，同时保留旧参数顺序。
    @FluxonFunction("clamp")
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    @FluxonFunction("clamp")
    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(value, max));
    }

    @FluxonFunction("clamp")
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    @FluxonFunction
    public static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    // region min

    @FluxonFunction("min")
    public static int min(int a, int b) {
        return Math.min(a, b);
    }

    @FluxonFunction("min")
    public static long min(long a, long b) {
        return Math.min(a, b);
    }

    @FluxonFunction("min")
    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    // region max

    @FluxonFunction("max")
    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    @FluxonFunction("max")
    public static long max(long a, long b) {
        return Math.max(a, b);
    }

    @FluxonFunction("max")
    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    // region abs

    @FluxonFunction("abs")
    public static int abs(int value) {
        return Math.abs(value);
    }

    @FluxonFunction("abs")
    public static long abs(long value) {
        return Math.abs(value);
    }

    @FluxonFunction("abs")
    public static double abs(double value) {
        return Math.abs(value);
    }

    // region 取整

    @FluxonFunction
    public static long round(double value) {
        return Math.round(value);
    }

    @FluxonFunction
    public static double floor(double value) {
        return Math.floor(value);
    }

    @FluxonFunction
    public static double ceil(double value) {
        return Math.ceil(value);
    }

    // region 三角函数

    @FluxonFunction
    public static double sin(double value) {
        return Math.sin(value);
    }

    @FluxonFunction
    public static double cos(double value) {
        return Math.cos(value);
    }

    @FluxonFunction
    public static double tan(double value) {
        return Math.tan(value);
    }

    @FluxonFunction
    public static double asin(double value) {
        if (value < -1.0 || value > 1.0) {
            throw new ArithmeticException("asin input must be between -1 and 1");
        }
        return Math.asin(value);
    }

    @FluxonFunction
    public static double acos(double value) {
        if (value < -1.0 || value > 1.0) {
            throw new ArithmeticException("acos input must be between -1 and 1");
        }
        return Math.acos(value);
    }

    @FluxonFunction
    public static double atan(double value) {
        return Math.atan(value);
    }

    @FluxonFunction
    public static double atan2(double y, double x) {
        return Math.atan2(y, x);
    }

    // region 指数与对数

    @FluxonFunction
    public static double exp(double value) {
        return Math.exp(value);
    }

    @FluxonFunction
    public static double log(double value) {
        if (value <= 0) {
            throw new ArithmeticException("log input must be positive");
        }
        return Math.log(value);
    }

    @FluxonFunction
    public static double log10(double value) {
        if (value <= 0) {
            throw new ArithmeticException("log10 input must be positive");
        }
        return Math.log10(value);
    }

    // region 幂与根

    @FluxonFunction
    public static double pow(double base, double exponent) {
        return Math.pow(base, exponent);
    }

    @FluxonFunction("pow")
    public static int powInt(int base, int exponent) {
        return (int) Math.pow(base, exponent);
    }

    @FluxonFunction("pow")
    public static long powLong(long base, long exponent) {
        return (long) Math.pow(base, exponent);
    }

    @FluxonFunction
    public static double sqrt(double value) {
        if (value < 0) {
            throw new ArithmeticException("Cannot take square root of negative number");
        }
        return Math.sqrt(value);
    }

    @FluxonFunction
    public static double cbrt(double value) {
        return Math.cbrt(value);
    }

    @FluxonFunction
    public static double hypot(double x, double y) {
        return Math.hypot(x, y);
    }

    // region 角度转换

    @FluxonFunction("rad")
    public static double rad(double degrees) {
        return Math.toRadians(degrees);
    }

    @FluxonFunction("deg")
    public static double deg(double radians) {
        return Math.toDegrees(radians);
    }

    // region 符号

    @FluxonFunction("sign")
    public static int signInt(int value) {
        return Integer.signum(value);
    }

    @FluxonFunction("sign")
    public static int signLong(long value) {
        return Long.signum(value);
    }

    @FluxonFunction("sign")
    public static double signDouble(double value) {
        return Math.signum(value);
    }
}
