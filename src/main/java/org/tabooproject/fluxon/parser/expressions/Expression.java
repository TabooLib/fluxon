package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 表达式基础接口
 * 所有表达式类型的基础接口
 */
public interface Expression extends ParseResult {
    @Override
    default ResultType getType() {
        return ResultType.EXPRESSION;
    }
}