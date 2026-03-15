package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.*;

import java.util.List;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;
import static org.tabooproject.fluxon.runtime.Type.*;

/**
 * 系统函数库
 * 纯函数通过 @FluxonFunction 注解注册，需要 Environment 的函数保持手动注册
 *
 * @author sky
 */
public class FunctionSystem {

    @SuppressWarnings("DataFlowIssue")
    public static void init(FluxonRuntime runtime) {
        // 扫描注册所有 @FluxonFunction 方法
        FluxonFunctionScanner.register(runtime, FunctionSystem.class);
        // 需要 Environment 的函数（不适合 DirectBinding）
        runtime.registerFunction("print", returns(VOID).params(OBJECT), context -> {
            if (0 < context.getArgumentCount()) {
                context.getEnvironment().getOut().println(context.getArgBoxed(0));
            } else {
                context.getEnvironment().getOut().println();
            }
        });
        runtime.registerFunction("error", returns(VOID).params(OBJECT), context -> {
            if (0 < context.getArgumentCount()) {
                context.getEnvironment().getErr().println(context.getArgBoxed(0));
            } else {
                context.getEnvironment().getErr().println();
            }
        });
        // call 需要 FunctionContextPool，保持手动注册
        runtime.registerFunction("call", returns(OBJECT).params(OBJECT, OBJECT), context -> {
            Object func = context.getRef(0);
            Object[] parameters;
            if (context.getArgumentCount() < 2) {
                parameters = new Object[0];
            } else {
                parameters = ((List<?>) context.getRef(1)).toArray();
            }
            FunctionContextPool pool = context.getPool();
            if (func instanceof Function) {
                try (FunctionContext<?> borrowed = pool.borrowCopy(context, parameters)) {
                    ((Function) func).call(borrowed);
                    context.setReturnRef(borrowed.getReturnRef());
                }
            } else {
                Function function = context.getEnvironment().getFunction(func.toString());
                try (FunctionContext<?> borrowed = pool.borrowCopy(context, parameters)) {
                    function.call(borrowed);
                    context.setReturnRef(borrowed.getReturnRef());
                }
            }
        });
        // this 需要遍历 Environment 链
        runtime.registerFunction("this", returns(OBJECT).noParams(), context -> {
            Environment environment = context.getEnvironment();
            Object target = environment.getTarget();
            while (target == null && environment.getParent() != null) {
                environment = environment.getParent();
                target = environment.getTarget();
            }
            context.setReturnRef(target);
        });
        // throw 抛异常，需要区分 Error 和普通对象，保持手动注册
        // 不适合 @FluxonFunction：抛出的异常类型不确定（Error vs RuntimeException）
        runtime.registerFunction("throw", returns(VOID).params(OBJECT), context -> {
            Object o = context.getArgBoxed(0);
            if (o instanceof Error) {
                throw (Error) o;
            } else {
                throw new RuntimeException(o.toString());
            }
        });
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
    public static Class<?> forName(Object className) {
        try {
            return Class.forName(className.toString());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
    }
}
