package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 函数调用上下文
 * 封装函数调用所需的所有信息：目标对象、参数列表和环境
 */
public class FunctionContext {

    @Nullable
    private final Object target;
    private final Object[] arguments;
    @NotNull
    private final Environment environment;

    public FunctionContext(@Nullable Object target, Object[] arguments, @NotNull Environment environment) {
        this.target = target;
        this.arguments = arguments;
        this.environment = environment;
    }

    @Nullable
    public Object getTarget() {
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
