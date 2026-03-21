package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.*;

import java.util.List;

/**
 * 系统函数库
 * 所有函数通过 @FluxonFunction 注解注册
 * 需要运行时上下文的函数在签名中声明 FunctionContext 参数，由 Scanner 自动注入
 *
 * @author sky
 */
public class FunctionSystem {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, FunctionSystem.class);
    }

    @FluxonFunction
    public static void print(FunctionContext<?> ctx, Object value) {
        if (ctx.getArgumentCount() > 0) {
            ctx.getEnvironment().getOut().println(value);
        } else {
            ctx.getEnvironment().getOut().println();
        }
    }

    @FluxonFunction("error")
    public static void error(FunctionContext<?> ctx, Object value) {
        if (ctx.getArgumentCount() > 0) {
            ctx.getEnvironment().getErr().println(value);
        } else {
            ctx.getEnvironment().getErr().println();
        }
    }

    // 通过函数引用或名称动态调用函数
    @FluxonFunction
    public static Object call(FunctionContext<?> ctx, Object func, Object args) {
        Object[] parameters;
        if (ctx.getArgumentCount() < 2) {
            parameters = new Object[0];
        } else {
            parameters = ((List<?>) args).toArray();
        }
        FunctionContextPool pool = ctx.getPool();
        if (func instanceof Function) {
            try (FunctionContext<?> borrowed = pool.borrowCopy(ctx, parameters)) {
                ((Function) func).call(borrowed);
                return borrowed.getReturnRef();
            }
        }
        Function function = ctx.getEnvironment().getFunction(func.toString());
        try (FunctionContext<?> borrowed = pool.borrowCopy(ctx, parameters)) {
            function.call(borrowed);
            return borrowed.getReturnRef();
        }
    }

    // 沿 Environment 父链向上查找第一个非 null 的 target
    @FluxonFunction("this")
    public static Object thisTarget(FunctionContext<?> ctx) {
        Environment environment = ctx.getEnvironment();
        Object target = environment.getTarget();
        while (target == null && environment.getParent() != null) {
            environment = environment.getParent();
            target = environment.getTarget();
        }
        return target;
    }

    // 抛出异常，Error 直接抛出，其他包装为 RuntimeException
    @FluxonFunction("throw")
    public static void throwError(FunctionContext<?> ctx, Object value) {
        if (value instanceof Error) {
            throw (Error) value;
        }
        throw new RuntimeException(value.toString());
    }

    @FluxonFunction
    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException("Sleep function interrupted", e);
        }
    }

    @FluxonFunction
    public static Class<?> forName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
    }
}
