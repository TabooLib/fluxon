package org.tabooproject.fluxon.parser;

import java.util.function.Consumer;

/**
 * AST 子节点容器。
 * 用于 MapEntry、WhenBranch 等非 ParseResult 包装对象暴露内部表达式。
 */
public interface ParseResultContainer {

    /**
     * 遍历包装对象中的直接子节点。
     */
    void forEachChild(Consumer<ParseResult> consumer);
}
