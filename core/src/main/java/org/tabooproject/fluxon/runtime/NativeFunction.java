package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.parser.SymbolFunction;

import java.util.List;

/**
 * 原生函数类
 * 表示由 Java 实现的内置函数
 */
public class NativeFunction implements Function {

    private final SymbolFunction symbolInfo;
    private final NativeCallable callable;

    public NativeFunction(SymbolFunction symbolInfo, NativeCallable callable) {
        this.symbolInfo = symbolInfo;
        this.callable = callable;
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
        return false;
    }

    @Override
    public Object call(Object[] args) {
        return callable.call(args);
    }

    public SymbolFunction getSymbolInfo() {
        return symbolInfo;
    }

    public NativeCallable getCallable() {
        return callable;
    }

    /**
     * 原生函数接口
     */
    @FunctionalInterface
    public interface NativeCallable {
        /**
         * 调用原生函数
         *
         * @param args 参数列表
         * @return 返回值
         */
        Object call(Object[] args);
    }
} 