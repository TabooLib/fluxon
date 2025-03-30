package org.tabooproject.fluxon.parser.expression;

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
    
    /**
     * 获取表达式具体类型
     * 
     * @return 表达式类型枚举值
     */
    ExpressionType getExpressionType();
    
    /**
     * 生成带缩进的伪代码表示
     *
     * @param indent 缩进级别
     * @return 伪代码字符串
     */
    default String toPseudoCode(int indent) {
        return "/* 未实现的表达式 */";
    }
}