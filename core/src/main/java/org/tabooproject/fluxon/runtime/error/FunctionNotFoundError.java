package org.tabooproject.fluxon.runtime.error;

import org.tabooproject.fluxon.runtime.Environment;

import java.util.Optional;

/**
 * 没有找到函数异常
 * 在运行时由 Intrinsics#prepareCall 抛出
 */
public class FunctionNotFoundError extends FluxonRuntimeError {

    private final Environment environment;
    private final Object target;
    private final String name;
    private final int argCount;
    private final int pos;
    private final int exPos;

    public FunctionNotFoundError(Environment environment, Object target, String name, int argCount, int pos, int exPos) {
        super(Optional.ofNullable(target).map(i -> i.getClass().getSimpleName() + "::").orElse("") + name + "(args=" + argCount + ")");
        this.environment = environment;
        this.target = target;
        this.name = name;
        this.argCount = argCount;
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

    public int getArgCount() {
        return argCount;
    }

    public int getPos() {
        return pos;
    }

    public int getExPos() {
        return exPos;
    }
}
