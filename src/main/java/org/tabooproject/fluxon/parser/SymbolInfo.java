package org.tabooproject.fluxon.parser;

import java.util.Collections;
import java.util.List;

/**
 * 符号信息类
 */
public class SymbolInfo {
    private final SymbolType type;
    private final String name;
    private final List<Integer> parameterCounts;
    private final int maxParameterCount;

    public SymbolInfo(SymbolType type, String name, int parameterCount) {
        this.type = type;
        this.name = name;
        this.parameterCounts = Collections.singletonList(parameterCount);
        this.maxParameterCount = parameterCount;
    }

    public SymbolInfo(SymbolType type, String name, List<Integer> parameterCounts) {
        this.type = type;
        this.name = name;
        this.parameterCounts = parameterCounts;
        this.maxParameterCount = parameterCounts.isEmpty() ? 0 : Collections.max(parameterCounts);
    }

    public String getName() {
        return name;
    }

    public SymbolType getType() {
        return type;
    }

    /**
     * 获取参数数量列表
     *
     * @return 参数数量列表
     */
    public List<Integer> getParameterCounts() {
        return parameterCounts;
    }

    /**
     * 获取最大参数数量
     *
     * @return 最大参数数量
     */
    public int getMaxParameterCount() {
        return maxParameterCount;
    }

    /**
     * 检查是否支持指定的参数数量
     *
     * @param count 参数数量
     * @return 是否支持
     */
    public boolean supportsParameterCount(int count) {
        return parameterCounts.contains(count);
    }

    @Override
    public String toString() {
        return "SymbolInfo{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", parameterCounts=" + parameterCounts +
                ", maxParameterCount=" + maxParameterCount +
                '}';
    }
}