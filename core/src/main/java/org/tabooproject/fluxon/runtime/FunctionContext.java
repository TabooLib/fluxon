package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.runtime.error.ArgumentTypeMismatchError;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.Arrays;

/**
 * 函数调用上下文
 * 封装函数调用所需的所有信息：目标对象、参数列表和环境
 */
public class FunctionContext<Target> {

    public static final Type TYPE = new Type(FunctionContext.class);

    @NotNull
    private final Function function;
    @Nullable
    private final Target target;
    private final Object[] arguments;
    private final int argumentCount;
    @NotNull
    private final Environment environment;

    public FunctionContext(@NotNull Function function, @Nullable Target target, Object[] arguments, @NotNull Environment environment) {
        this.function = function;
        this.target = target;
        this.arguments = arguments;
        this.argumentCount = arguments.length;
        this.environment = environment;
    }

    public boolean hasArgument(int index) {
        return index < argumentCount;
    }

    @Nullable
    public Object getArgument(int index) {
        if (index >= argumentCount) {
            return null;
        }
        return arguments[index];
    }

    @Nullable
    public <T> T getArgumentByType(int index, @NotNull Class<T> type) {
        Object argument = getArgument(index);
        if (argument == null) return null;
        // 修复：传入对象本身，而不是 Class 对象
        if (Intrinsics.isCompatibleType(type, argument)) {
            return type.cast(argument);
        }
        throw new ArgumentTypeMismatchError(this, index, type, argument);
    }

    @NotNull
    public Number getNumber(int index) {
        Object argument = getArgument(index);
        if (argument instanceof Number) {
            return ((Number) argument);
        }
        throw new ArgumentTypeMismatchError(this, index, Number.class, argument);
    }

    @Nullable
    public Number getNumberOrNull(int index) {
        return getArgumentByType(index, Number.class);
    }

    public boolean getBoolean(int index) {
        Object argument = getArgument(index);
        return Coerce.asBoolean(argument).orElse(false);
    }

    @Nullable
    public String getString(int index) {
        Object argument = getArgument(index);
        if (argument == null) return null;
        return argument.toString();
    }

    @NotNull
    public Function getFunction(int index) {
        Object argument = getArgument(index);
        if (argument instanceof Function) {
            return (Function) argument;
        }
        throw new ArgumentTypeMismatchError(this, index, Function.class, argument);
    }

    @Nullable
    public String getFunctionOrNull(int index) {
        Object argument = getArgument(index);
        if (argument instanceof Function) {
            return ((Function) argument).getName();
        }
        return null;
    }

    @NotNull
    public Function getFunction() {
        return function;
    }

    @Nullable
    public Target getTarget() {
        return target;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public int getArgumentCount() {
        return argumentCount;
    }

    @NotNull
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public String toString() {
        return "FunctionContext{" +
                "function=" + function +
                ", target=" + target +
                ", arguments=" + Arrays.toString(arguments) +
                ", environment=" + environment +
                '}';
    }
}
