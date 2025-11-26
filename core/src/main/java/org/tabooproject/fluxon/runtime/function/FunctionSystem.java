package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.FunctionContextPool;

import java.util.Arrays;
import java.util.List;

public class FunctionSystem {

    @SuppressWarnings({"DataFlowIssue"})
    public static void init(FluxonRuntime runtime) {
        runtime.registerFunction("print", 1, (context) -> {
            Object[] args = context.getArguments();
            if (args.length > 0) {
                System.out.println(args[0]);
            } else {
                System.out.println();
            }
            return null;
        });
        runtime.registerFunction("error", 1, (context) -> {
            Object[] args = context.getArguments();
            if (args.length > 0) {
                System.err.println(args[0]);
            } else {
                System.err.println();
            }
            return null;
        });
        runtime.registerFunction("sleep", 1, (context) -> {
            int sleepMillis = context.getNumber(0).intValue();
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                throw new RuntimeException("Sleep function interrupted", e);
            }
            return null;
        });
        runtime.registerFunction("forName", 1, (context) -> {
            String className = context.getString(0);
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found: " + className, e);
            }
        });
        runtime.registerFunction("call", Arrays.asList(1, 2), (context) -> {
            Object func = context.getArgument(0);
            // 获取可选的参数
            Object[] parameters;
            if (context.hasArgument(1)) {
                parameters = context.getArgumentByType(1, List.class).toArray();
            } else {
                parameters = new Object[0];
            }
            // 调用函数
            if (func instanceof Function) {
                FunctionContextPool pool = FunctionContextPool.local();
                FunctionContext<?> borrowed = pool.borrow(context.getFunction(), context.getTarget(), parameters, context.getEnvironment());
                try {
                    return ((Function) func).call(borrowed);
                } finally {
                    pool.release(borrowed);
                }
            } else {
                Function function = context.getEnvironment().getFunction(func.toString());
                FunctionContextPool pool = FunctionContextPool.local();
                FunctionContext<?> borrowed = pool.borrow(context.getFunction(), context.getTarget(), parameters, context.getEnvironment());
                try {
                    return function.call(borrowed);
                } finally {
                    pool.release(borrowed);
                }
            }
        });
        runtime.registerFunction("this", 0, (context) -> {
            Environment environment = context.getEnvironment();
            Object target = context.getEnvironment().getTarget();
            while (target == null && environment.getParent() != null) {
                environment = environment.getParent();
                target = environment.getTarget();
            }
            return target;
        });
        runtime.registerFunction("throw", 1, (context) -> {
            Object o = context.getArgument(0);
            if (o instanceof Error) {
                throw (Error) o;
            } else {
                throw new RuntimeException(o.toString());
            }
        });
    }
}
