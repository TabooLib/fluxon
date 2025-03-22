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
    
    /**
     * 生成带缩进的伪代码表示
     *
     * @param indent 缩进级别
     * @return 伪代码字符串
     */
    default String toPseudoCode(int indent) {
        return "/* 未实现的语句 */";
    }
}