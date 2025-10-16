package org.tabooproject.fluxon.interpreter.error;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.runtime.FunctionContext;

/**
 * 函数参数不匹配
 */
public class ArgumentTypeMismatchException extends RuntimeException {

    private final FunctionContext<?> context;
    private final int index;
    private final Class<?> expect;

    public ArgumentTypeMismatchException(FunctionContext<?> context, int index, @NotNull Class<?> expect, @Nullable Object actual) {
        super("Argument " + index + " expect " + expect.getSimpleName() + " but got " + (actual == null ? "null" : actual.getClass().getSimpleName()) + " (" + actual + ")");
        this.context = context;
        this.index = index;
        this.expect = expect;
    }

    /**
     * 获取函数上下文
     * @return 函数上下文
     */
    public FunctionContext<?> getContext() {
        return context;
    }

    /**
     * 获取参数索引
     */
    public int getIndex() {
        return index;
    }

    /**
     * 获取期望类型
     */
    public Class<?> getExpect() {
        return expect;
    }
}
