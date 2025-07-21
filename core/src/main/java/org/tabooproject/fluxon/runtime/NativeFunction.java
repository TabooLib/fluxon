package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.SymbolFunction;

import java.util.List;

/**
 * 原生函数类
 * 表示由 Java 实现的内置函数
 */
public class NativeFunction implements Function, Symbolic {

    private final SymbolFunction symbolInfo;
    private final NativeCallable callable;
    private final boolean isAsync;

    public NativeFunction(SymbolFunction symbolInfo, NativeCallable callable) {
        this(symbolInfo, callable, false);
    }

    public NativeFunction(SymbolFunction symbolInfo, NativeCallable callable, boolean isAsync) {
        this.symbolInfo = symbolInfo;
        this.callable = callable;
        this.isAsync = isAsync;
    }

    @NotNull
    @Override
    public String getName() {
        return symbolInfo.getName();
    }

    @NotNull
    @Override
    public List<Integer> getParameterCounts() {
        return symbolInfo.getParameterCounts();
    }

    @Override
    public boolean isAsync() {
        return isAsync;
    }

    @Override
    public Object call(@NotNull final FunctionContext context) {
        return callable.call(context);
    }

    @Override
    public SymbolFunction getInfo() {
        return symbolInfo;
    }

    public NativeCallable getCallable() {
        return callable;
    }

    @Override
    public String toString() {
        return "NativeFunction{" +
                "symbolInfo=" + symbolInfo +
                ", isAsync=" + isAsync +
                '}';
    }

    /**
     * 原生函数接口
     */
    @FunctionalInterface
    public interface NativeCallable {

        /**
         * 调用原生函数
         *
         * @param context 函数上下文，包含调用目标、参数列表和环境
         * @return 返回值
         */
        Object call(@NotNull FunctionContext context);
    }
} 