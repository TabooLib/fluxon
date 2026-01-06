package org.tabooproject.fluxon.lexer;

/**
 * 字符串上下文，用于追踪插值嵌套
 */
class StringContext {
    final ScanMode mode;       // 字符串类型（单引号/双引号）
    int braceDepth;            // 当前插值内的大括号深度

    StringContext(ScanMode mode) {
        this.mode = mode;
        this.braceDepth = 0;
    }
}
