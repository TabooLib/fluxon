package org.tabooproject.fluxon.parser;

/**
 * 函数作用域快照
 * 用于保存和恢复函数解析上下文
 */
public class FunctionScopeSnapshot {
    private final String previousFunction;
    private final boolean isBreakable;
    private final boolean isContinuable;
    private final boolean isContextCall;

    public FunctionScopeSnapshot(String previousFunction, boolean isBreakable, boolean isContinuable, boolean isContextCall) {
        this.previousFunction = previousFunction;
        this.isBreakable = isBreakable;
        this.isContinuable = isContinuable;
        this.isContextCall = isContextCall;
    }

    public String getPreviousFunction() {
        return previousFunction;
    }

    public boolean isBreakable() {
        return isBreakable;
    }

    public boolean isContinuable() {
        return isContinuable;
    }

    public boolean isContextCall() {
        return isContextCall;
    }
}
