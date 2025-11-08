package org.tabooproject.fluxon.runtime.error;

import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntimeError;

/**
 * 没有找到函数异常
 * 在运行时由 Intrinsics#callFunction 抛出
 */
public class FunctionNotFoundError extends FluxonRuntimeError {

    private final Environment environment;
    private final Object target;
    private final String name;
    private final Object[] arguments;
    private final int pos;
    private final int exPos;

    public FunctionNotFoundError(Environment environment, Object target, String name, Object[] arguments, int pos, int exPos) {
        super("Function not found: " + name);
        this.environment = environment;
        this.target = target;
        this.name = name;
        this.arguments = arguments;
        this.pos = pos;
        this.exPos = exPos;
    }

     public Environment getEnvironment() {
        return environment;
    }

     public Object getTarget() {
        return target;
    }

    public String getName() {
        return name;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public int getPos() {
        return pos;
    }

    public int getExPos() {
        return exPos;
    }
}
