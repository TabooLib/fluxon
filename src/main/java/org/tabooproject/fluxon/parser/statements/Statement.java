package org.tabooproject.fluxon.parser.statements;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 语句接口
 * 所有语句类型的基础接口
 */
public interface Statement extends ParseResult {
    @Override
    default ResultType getType() {
        return ResultType.STATEMENT;
    }
}