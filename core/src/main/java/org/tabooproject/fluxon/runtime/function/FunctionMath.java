package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

/**
 * 数学函数库
 * 使用类型精确的重载，避免运行时类型判断
 *
 * @author sky
 */
public class FunctionMath {

    public static void init(FluxonRuntime runtime) {
        // 数学常数
        runtime.registerVariable("PI", Math.PI);
        runtime.registerVariable("E", Math.E);

        // min - 类型精确重载
        runtime.registerFunction("min", returns(Type.I).params(Type.I, Type.I), ctx -> {
            ctx.setReturnInt(Math.min(ctx.getInt(0), ctx.getInt(1)));
        });
        runtime.registerFunction("min", returns(Type.J).params(Type.J, Type.J), ctx -> {
            ctx.setReturnLong(Math.min(ctx.getLong(0), ctx.getLong(1)));
        });
        runtime.registerFunction("min", returns(Type.D).params(Type.D, Type.D), ctx -> {
            ctx.setReturnDouble(Math.min(ctx.getDouble(0), ctx.getDouble(1)));
        });

        // max - 类型精确重载
        runtime.registerFunction("max", returns(Type.I).params(Type.I, Type.I), ctx -> {
            ctx.setReturnInt(Math.max(ctx.getInt(0), ctx.getInt(1)));
        });
        runtime.registerFunction("max", returns(Type.J).params(Type.J, Type.J), ctx -> {
            ctx.setReturnLong(Math.max(ctx.getLong(0), ctx.getLong(1)));
        });
        runtime.registerFunction("max", returns(Type.D).params(Type.D, Type.D), ctx -> {
            ctx.setReturnDouble(Math.max(ctx.getDouble(0), ctx.getDouble(1)));
        });

        // clamp - 类型精确重载
        runtime.registerFunction("clamp", returns(Type.I).params(Type.I, Type.I, Type.I), ctx -> {
            ctx.setReturnInt(Math.max(ctx.getInt(1), Math.min(ctx.getInt(0), ctx.getInt(2))));
        });
        runtime.registerFunction("clamp", returns(Type.J).params(Type.J, Type.J, Type.J), ctx -> {
            ctx.setReturnLong(Math.max(ctx.getLong(1), Math.min(ctx.getLong(0), ctx.getLong(2))));
        });
        runtime.registerFunction("clamp", returns(Type.D).params(Type.D, Type.D, Type.D), ctx -> {
            ctx.setReturnDouble(Math.max(ctx.getDouble(1), Math.min(ctx.getDouble(0), ctx.getDouble(2))));
        });

        // abs - 类型精确重载
        runtime.registerFunction("abs", returns(Type.I).params(Type.I), ctx -> {
            ctx.setReturnInt(Math.abs(ctx.getInt(0)));
        });
        runtime.registerFunction("abs", returns(Type.J).params(Type.J), ctx -> {
            ctx.setReturnLong(Math.abs(ctx.getLong(0)));
        });
        runtime.registerFunction("abs", returns(Type.D).params(Type.D), ctx -> {
            ctx.setReturnDouble(Math.abs(ctx.getDouble(0)));
        });

        // round
        runtime.registerFunction("round", returns(Type.J).params(Type.D), ctx -> {
            ctx.setReturnLong(Math.round(ctx.getDouble(0)));
        });

        // floor/ceil
        runtime.registerFunction("floor", returns(Type.D).params(Type.D), ctx -> {
            ctx.setReturnDouble(Math.floor(ctx.getDouble(0)));
        });
        runtime.registerFunction("ceil", returns(Type.D).params(Type.D), ctx -> {
            ctx.setReturnDouble(Math.ceil(ctx.getDouble(0)));
        });

        // 三角函数
        runtime.registerFunction("sin", returns(Type.D).params(Type.D), ctx -> ctx.setReturnDouble(Math.sin(ctx.getDouble(0))));
        runtime.registerFunction("cos", returns(Type.D).params(Type.D), ctx -> ctx.setReturnDouble(Math.cos(ctx.getDouble(0))));
        runtime.registerFunction("tan", returns(Type.D).params(Type.D), ctx -> ctx.setReturnDouble(Math.tan(ctx.getDouble(0))));
        runtime.registerFunction("asin", returns(Type.D).params(Type.D), ctx -> {
            double value = ctx.getDouble(0);
            if (value < -1.0 || value > 1.0) {
                throw new ArithmeticException("asin input must be between -1 and 1");
            }
            ctx.setReturnDouble(Math.asin(value));
        });
        runtime.registerFunction("acos", returns(Type.D).params(Type.D), ctx -> {
            double value = ctx.getDouble(0);
            if (value < -1.0 || value > 1.0) {
                throw new ArithmeticException("acos input must be between -1 and 1");
            }
            ctx.setReturnDouble(Math.acos(value));
        });
        runtime.registerFunction("atan", returns(Type.D).params(Type.D), ctx -> ctx.setReturnDouble(Math.atan(ctx.getDouble(0))));
        runtime.registerFunction("atan2", returns(Type.D).params(Type.D, Type.D), ctx -> ctx.setReturnDouble(Math.atan2(ctx.getDouble(0), ctx.getDouble(1))));

        // 指数与对数
        runtime.registerFunction("exp", returns(Type.D).params(Type.D), ctx -> ctx.setReturnDouble(Math.exp(ctx.getDouble(0))));
        runtime.registerFunction("log", returns(Type.D).params(Type.D), ctx -> {
            double value = ctx.getDouble(0);
            if (value <= 0) {
                throw new ArithmeticException("log input must be positive");
            }
            ctx.setReturnDouble(Math.log(value));
        });
        runtime.registerFunction("log10", returns(Type.D).params(Type.D), ctx -> {
            double value = ctx.getDouble(0);
            if (value <= 0) {
                throw new ArithmeticException("log10 input must be positive");
            }
            ctx.setReturnDouble(Math.log10(value));
        });

        // pow - 类型精确重载
        runtime.registerFunction("pow", returns(Type.D).params(Type.D, Type.D), ctx -> {
            ctx.setReturnDouble(Math.pow(ctx.getDouble(0), ctx.getDouble(1)));
        });
        runtime.registerFunction("pow", returns(Type.I).params(Type.I, Type.I), ctx -> {
            ctx.setReturnInt((int) Math.pow(ctx.getInt(0), ctx.getInt(1)));
        });
        runtime.registerFunction("pow", returns(Type.J).params(Type.J, Type.J), ctx -> {
            ctx.setReturnLong((long) Math.pow(ctx.getLong(0), ctx.getLong(1)));
        });

        // sqrt
        runtime.registerFunction("sqrt", returns(Type.D).params(Type.D), ctx -> {
            double value = ctx.getDouble(0);
            if (value < 0) {
                throw new ArithmeticException("Cannot take square root of negative number");
            }
            ctx.setReturnDouble(Math.sqrt(value));
        });

        // cbrt (立方根)
        runtime.registerFunction("cbrt", returns(Type.D).params(Type.D), ctx -> {
            ctx.setReturnDouble(Math.cbrt(ctx.getDouble(0)));
        });

        // random - 无参版本返回 [0, 1) 的 double
        runtime.registerFunction("random", returns(Type.D).noParams(), ctx -> {
            ctx.setReturnDouble(Math.random());
        });
        // random(end) - int 版本返回 [0, end) 的整数
        runtime.registerFunction("random", returns(Type.I).params(Type.I), ctx -> {
            int end = ctx.getInt(0);
            if (end <= 0) throw new IllegalArgumentException("random " + end + " must be positive");
            ctx.setReturnInt((int) (Math.random() * end));
        });
        // random(end) - double 版本返回 [0, end) 的浮点数
        runtime.registerFunction("random", returns(Type.D).params(Type.D), ctx -> {
            double end = ctx.getDouble(0);
            if (end <= 0) throw new IllegalArgumentException("random " + end + " must be positive");
            ctx.setReturnDouble(Math.random() * end);
        });
        // random(start, end) - int 版本返回 [start, end) 的整数
        runtime.registerFunction("random", returns(Type.I).params(Type.I, Type.I), ctx -> {
            int start = ctx.getInt(0);
            int end = ctx.getInt(1);
            if (start >= end) throw new IllegalArgumentException("random " + start + " must be less than " + end);
            ctx.setReturnInt(start + (int) (Math.random() * (end - start)));
        });
        // random(start, end) - double 版本返回 [start, end) 的浮点数
        runtime.registerFunction("random", returns(Type.D).params(Type.D, Type.D), ctx -> {
            double start = ctx.getDouble(0);
            double end = ctx.getDouble(1);
            if (start >= end) throw new IllegalArgumentException("random " + start + " must be less than " + end);
            ctx.setReturnDouble(start + Math.random() * (end - start));
        });

        // 角度与弧度转换
        runtime.registerFunction("rad", returns(Type.D).params(Type.D), ctx -> ctx.setReturnDouble(Math.toRadians(ctx.getDouble(0))));
        runtime.registerFunction("deg", returns(Type.D).params(Type.D), ctx -> ctx.setReturnDouble(Math.toDegrees(ctx.getDouble(0))));

        // 插值
        runtime.registerFunction("lerp", returns(Type.D).params(Type.D, Type.D, Type.D), ctx -> {
            double start = ctx.getDouble(0);
            double end = ctx.getDouble(1);
            double t = ctx.getDouble(2);
            ctx.setReturnDouble(start + (end - start) * t);
        });

        // sign - 符号函数
        runtime.registerFunction("sign", returns(Type.I).params(Type.I), ctx -> {
            ctx.setReturnInt(Integer.signum(ctx.getInt(0)));
        });
        runtime.registerFunction("sign", returns(Type.I).params(Type.J), ctx -> {
            ctx.setReturnInt(Long.signum(ctx.getLong(0)));
        });
        runtime.registerFunction("sign", returns(Type.D).params(Type.D), ctx -> {
            ctx.setReturnDouble(Math.signum(ctx.getDouble(0)));
        });

        // hypot - 直角三角形斜边
        runtime.registerFunction("hypot", returns(Type.D).params(Type.D, Type.D), ctx -> {
            ctx.setReturnDouble(Math.hypot(ctx.getDouble(0), ctx.getDouble(1)));
        });
    }
}
