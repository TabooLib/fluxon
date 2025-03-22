package org.tabooproject.fluxon.parser.definitions;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 定义接口
 * 所有定义类型的基础接口
 */
public interface Definition extends ParseResult {
    @Override
    default ResultType getType() {
        return ResultType.DEFINITION;
    }
}