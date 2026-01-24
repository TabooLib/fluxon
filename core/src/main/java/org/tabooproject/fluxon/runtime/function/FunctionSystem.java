package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.*;

import java.util.Arrays;
import java.util.List;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class FunctionSystem {

    @SuppressWarnings({"DataFlowIssue"})
    public static void init(FluxonRuntime runtime) {
        runtime.registerFunction("print", returns(Type.VOID).params(Type.OBJECT), context -> {
            if (0 < context.getArgumentCount()) {
                context.getEnvironment().getOut().println(context.getArgBoxed(0));
            } else {
                context.getEnvironment().getOut().println();
            }
        });
        runtime.registerFunction("error", returns(Type.VOID).params(Type.OBJECT), context -> {
            if (0 < context.getArgumentCount()) {
                context.getEnvironment().getErr().println(context.getArgBoxed(0));
            } else {
                context.getEnvironment().getErr().println();
            }
        });
        runtime.registerFunction("sleep", returns(Type.VOID).params(Type.I), context -> {
            int sleepMillis = context.getAsInt(0);
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                throw new RuntimeException("Sleep function interrupted", e);
            }
        });
        runtime.registerFunction("forName", returns(Type.CLASS).params(Type.OBJECT), context -> {
            Object arg = context.getRef(0);
            String className = arg != null ? arg.toString() : null;
            try {
                context.setReturnRef(Class.forName(className));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found: " + className, e);
            }
        });
        // call 支持 1-2 个参数
        runtime.registerFunction("call", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT), context -> {
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
        runtime.registerFunction("this", returns(Type.OBJECT).noParams(), context -> {
            Environment environment = context.getEnvironment();
            Object target = environment.getTarget();
            while (target == null && environment.getParent() != null) {
                environment = environment.getParent();
                target = environment.getTarget();
            }
            context.setReturnRef(target);
        });
        runtime.registerFunction("throw", returns(Type.VOID).params(Type.OBJECT), context -> {
            Object o = context.getArgBoxed(0);
            if (o instanceof Error) {
                throw (Error) o;
            } else {
                throw new RuntimeException(o.toString());
            }
        });
    }
}
