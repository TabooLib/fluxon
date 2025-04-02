package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;

/**
 * 访问者接口
 * 用于对不同类型的节点进行访问和评估
 */
public interface Visitor {

    /**
     * 访问表达式节点
     */
    Object visitExpression(Expression expression);

    /**
     * 访问语句节点
     */
    Object visitStatement(Statement statement);

    /**
     * 访问定义节点
     */
    Object visitDefinition(Definition definition);
}