package org.tabooproject.fluxon.parser.expression;

/**
 * 可透明递归的表达式。
 * 节点自身不引入额外运行时观察点，优化器可继续检查其子表达式。
 */
public interface TransparentExpression {
}
