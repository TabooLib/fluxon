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
     * 生成伪代码表示
     *
     * @return 伪代码字符串
     */
    default String toPseudoCode() {
        return toPseudoCode(0);
    }

    /**
     * 生成带缩进的伪代码表示
     *
     * @param indent 缩进级别
     * @return 伪代码字符串
     */
    String toPseudoCode(int indent);

    
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