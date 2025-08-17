package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.runtime.Function;

import java.util.Collections;
import java.util.List;

/**
 * 函数声明
 * 用于在编译阶段检测合法函数
 */
public class SymbolFunction implements Callable {
    private final String name;
    private final List<Integer> parameterCounts;
    private final int maxParameterCount;

    public SymbolFunction(String name, int parameterCount) {
        this.name = name;
        this.parameterCounts = Collections.singletonList(parameterCount);
        this.maxParameterCount = parameterCount;
    }

    public SymbolFunction(String name, List<Integer> parameterCounts) {
        this.name = name;
        this.parameterCounts = parameterCounts;
        this.maxParameterCount = parameterCounts.isEmpty() ? 0 : Collections.max(parameterCounts);
    }

    public String getName() {
        return name;
    }

    /**
     * 获取参数数量列表
     *
     * @return 参数数量列表
     */
    @Override
    public List<Integer> getParameterCounts() {
        return parameterCounts;
    }

    /**
     * 获取最大参数数量
     *
     * @return 最大参数数量
     */
    @Override
    public int getMaxParameterCount() {
        return maxParameterCount;
    }

    /**
     * 检查是否支持指定的参数数量
     *
     * @param count 参数数量
     * @return 是否支持
     */
    @Override
    public boolean supportsParameterCount(int count) {
        return parameterCounts.contains(count);
    }

    public static SymbolFunction of(Function function) {
        return new SymbolFunction(function.getName(), function.getParameterCounts());
    }

    @Override
    public String toString() {
        return "SymbolFunction{" +
                ", name='" + name + '\'' +
                ", parameterCounts=" + parameterCounts +
                ", maxParameterCount=" + maxParameterCount +
                '}';
    }
}