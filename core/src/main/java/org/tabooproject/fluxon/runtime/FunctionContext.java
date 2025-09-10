package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 函数调用上下文
 * 封装函数调用所需的所有信息：目标对象、参数列表和环境
 */
public class FunctionContext<Target> {

    public static final Type TYPE = new Type(FunctionContext.class);

    @Nullable
    private final Target target;
    private final Object[] arguments;
    @NotNull
    private final Environment environment;

    public FunctionContext(@Nullable Target target, Object[] arguments, @NotNull Environment environment) {
        this.target = target;
        this.arguments = arguments;
        this.environment = environment;
    }

    public boolean hasArgument(int index) {
        return index < arguments.length;
    }

    @Nullable
    public Object getArgument(int index) {
        if (index >= arguments.length) {
            return null;
        }
        return arguments[index];
    }

    @NotNull
    public Number getNumber(int index) {
        Object argument = getArgument(index);
        if (argument instanceof Number) {
            return ((Number) argument).intValue();
        }
        throw new IllegalArgumentException("Argument " + index + " is not a number");
    }

    @Nullable
    public Number getNumberOrNull(int index) {
        Object argument = getArgument(index);
        if (argument == null) {
            return null;
        }
        if (argument instanceof Number) {
            return ((Number) argument).intValue();
        }
        throw new IllegalArgumentException("Argument " + index + " is not a number");
    }

    @Nullable
    public Target getTarget() {
        return target;
    }

    public Object[] getArguments() {
        return arguments;
    }

    @NotNull
    public Environment getEnvironment() {
        return environment;
    }
}
