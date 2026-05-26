package org.tabooproject.fluxon.parser;

import java.util.function.Consumer;

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
     * 遍历当前 AST 节点的直接子节点。
     * 优化与分析逻辑必须通过该接口递归结构，避免在调用方穷举所有表达式类型。
     */
    default void forEachChild(Consumer<ParseResult> consumer) {
        ParseResultChildren.forEach(this, consumer);
    }

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
