package org.tabooproject.fluxon.lexer;

/**
 * 扫描模式枚举
 * 用于支持字符串插值的嵌套词法分析
 */
enum ScanMode {
    NORMAL,           // 普通代码模式
    STRING_DOUBLE,    // 双引号字符串内
    STRING_SINGLE     // 单引号字符串内
}
