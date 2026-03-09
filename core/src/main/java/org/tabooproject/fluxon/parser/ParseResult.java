package org.tabooproject.fluxon.parser;

/**
 * 解析结果接口
 * 所有解析结果类型的基础接口
 */
public interface ParseResult {

    /**
     * 获取结果类型
     */
    ResultType getType();

    /**
     * 生成伪代码表示
     */
    String toPseudoCode();

    /**
     * 结果类型枚举
     */
    enum ResultType {
        DEFINITION,
        EXPRESSION,
        STATEMENT,
        ANNOTATION
    }
}