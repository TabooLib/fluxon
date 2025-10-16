package org.tabooproject.fluxon.interpreter.error;

import org.tabooproject.fluxon.runtime.FunctionContext;

/**
 * 函数参数不匹配
 */
public class ArgumentTypeMismatchException extends RuntimeException {

    private final FunctionContext<?> context;
    private final int index;
    private final Class<?> expect;

    public ArgumentTypeMismatchException(FunctionContext<?> context, int index, Class<?> expect) {
        super("Argument " + index + " expect " + expect.getSimpleName() + " but got " + context.getArgument(index));
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
