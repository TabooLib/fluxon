package org.tabooproject.fluxon.parser.statement;

/**
 * 语句类型枚举
 * 用于区分不同类型的语句
 */
public enum StatementType {

    // 表达式语句
    EXPRESSION_STATEMENT,
    
    // 代码块
    BLOCK,

    // 跳出语句
    BREAK,

    // 继续语句
    CONTINUE,
    
    // 返回语句
    RETURN
} 