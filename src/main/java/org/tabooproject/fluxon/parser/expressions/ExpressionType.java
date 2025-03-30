package org.tabooproject.fluxon.parser.expressions;

/**
 * 表达式类型枚举
 * 用于区分不同类型的表达式
 */
public enum ExpressionType {

    // 标识符
    IDENTIFIER,

    // 字面量
    INT_LITERAL,
    LONG_LITERAL,
    FLOAT_LITERAL,
    DOUBLE_LITERAL,
    STRING_LITERAL,
    BOOLEAN_LITERAL,
    NULL_LITERAL,

    // 高级字面量
    LIST_LITERAL,
    MAP_LITERAL,
    RANGE,

    // 表达式
    IF,
    FOR,
    WHEN,
    WHILE,

    // 一元、二元和逻辑表达式
    UNARY,
    BINARY,
    LOGICAL,

    // 赋值表达式
    ASSIGNMENT,
    // 函数调用
    FUNCTION_CALL,

    AWAIT,
    REFERENCE,
    ELVIS,
    GROUPING
} 