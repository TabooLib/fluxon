package org.tabooproject.fluxon.parser;

/**
 * 解析结果接口
 * 所有解析结果类型的基础接口
 */
public interface ParseResult {
    /**
     * 获取结果类型
     *
     * @return 结果类型
     */
    ResultType getType();
    
    /**
     * 结果类型枚举
     */
    enum ResultType {
        // 表达式
        EXPRESSION,
        // 语句
        STATEMENT,
        // 定义
        DEFINITION
    }
}