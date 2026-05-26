package org.tabooproject.fluxon.parser.expression;

/**
 * 循环缓存可透明递归的表达式。
 * 循环缓存要求比普通纯表达式更严格，避免放行集合构造、索引读取等额外观察点。
 */
public interface LoopCacheTransparentExpression {
}
