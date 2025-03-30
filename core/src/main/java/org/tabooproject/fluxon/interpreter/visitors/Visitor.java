package org.tabooproject.fluxon.interpreter.visitors;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definitions.Definition;
import org.tabooproject.fluxon.parser.expressions.Expression;
import org.tabooproject.fluxon.parser.statements.Statement;

/**
 * 访问者接口
 * 用于对不同类型的节点进行访问和评估
 */
public interface Visitor {
    /**
     * 访问表达式节点
     *
     * @param expression 表达式对象
     * @return 评估结果
     */
    Object visitExpression(Expression expression);

    /**
     * 访问语句节点
     *
     * @param statement 语句对象
     * @return 评估结果
     */
    Object visitStatement(Statement statement);

    /**
     * 访问定义节点
     *
     * @param definition 定义对象
     * @return 评估结果
     */
    Object visitDefinition(Definition definition);

    /**
     * 访问解析结果
     *
     * @param result 解析结果
     * @return 评估结果
     */
    Object visit(ParseResult result);
} 