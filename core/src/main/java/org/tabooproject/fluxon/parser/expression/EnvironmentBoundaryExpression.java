package org.tabooproject.fluxon.parser.expression;

/**
 * 需要独立 Environment 的表达式。
 * 这类节点会观察 Environment 身份、切换 target 或延后执行，不能复用调用方环境。
 */
public interface EnvironmentBoundaryExpression {
}
