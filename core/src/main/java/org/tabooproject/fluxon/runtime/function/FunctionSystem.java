package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.*;

import java.util.Arrays;
import java.util.List;

public class FunctionSystem {

    @SuppressWarnings({"DataFlowIssue"})
    public static void init(FluxonRuntime runtime) {
        runtime.registerFunction("print", 1, context -> {
            if (0 < context.getArgumentCount()) {
                context.getEnvironment().getOut().println(context.getArgBoxed(0));
            } else {
                context.getEnvironment().getOut().println();
            }
        });
        runtime.registerFunction("error", 1, context -> {
            if (0 < context.getArgumentCount()) {
                context.getEnvironment().getErr().println(context.getArgBoxed(0));
            } else {
                context.getEnvironment().getErr().println();
            }
        });
        runtime.registerFunction("sleep", 1, context -> {
            int sleepMillis = context.getAsInt(0);
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                throw new RuntimeException("Sleep function interrupted", e);
            }
        });
        runtime.registerFunction("forName", 1, context -> {
            Object arg = context.getRef(0);
            String className = arg != null ? arg.toString() : null;
            try {
                context.setReturnRef(Class.forName(className));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found: " + className, e);
            }
        });
        runtime.registerFunction("call", Arrays.asList(1, 2), context -> {
            Object func = context.getRef(0);
            Object[] parameters;
            if (1 < context.getArgumentCount()) {
                parameters = ((List<?>) context.getRef(1)).toArray();
            } else {
                parameters = new Object[0];
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
        runtime.registerFunction("this", 0, context -> {
            Environment environment = context.getEnvironment();
            Object target = environment.getTarget();
            while (target == null && environment.getParent() != null) {
                environment = environment.getParent();
                target = environment.getTarget();
            }
            context.setReturnRef(target);
        });
        runtime.registerFunction("throw", 1, context -> {
            Object o = context.getArgBoxed(0);
            if (o instanceof Error) {
                throw (Error) o;
            } else {
                throw new RuntimeException(o.toString());
            }
        });
    }
}
