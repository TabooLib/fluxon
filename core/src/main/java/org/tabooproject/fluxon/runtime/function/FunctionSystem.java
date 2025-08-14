package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;

import java.util.Arrays;
import java.util.List;

public class FunctionSystem {

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
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
            Object[] args = context.getArguments();
            int seconds = ((Number) args[0]).intValue();
            try {
                Thread.sleep(seconds);
            } catch (InterruptedException e) {
                throw new RuntimeException("Sleep function interrupted", e);
            }
            return null;
        });
        runtime.registerFunction("forName", 1, (context) -> {
            Object[] args = context.getArguments();
            String className = (String) args[0];
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
                parameters = ((List<Object>) context.getArgument(1)).toArray();
            } else {
                parameters = new Object[0];
            }
            // 调用函数
            if (func instanceof Function) {
                return ((Function) func).call(new FunctionContext<>(context.getTarget(), parameters, context.getEnvironment()));
            } else {
                Function function = context.getEnvironment().getFunction(func.toString());
                return function.call(new FunctionContext<>(context.getTarget(), parameters, context.getEnvironment()));
            }
        });
        runtime.registerFunction("throw", 1, (context) -> {
            throw new RuntimeException(context.getArgument(0).toString());
        });
    }
}
