package org.tabooproject.fluxon.parser.definition;

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
    
    /**
     * 生成带缩进的伪代码表示
     *
     * @param indent 缩进级别
     * @return 伪代码字符串
     */
    default String toPseudoCode(int indent) {
        return "/* 未实现的定义 */";
    }
}