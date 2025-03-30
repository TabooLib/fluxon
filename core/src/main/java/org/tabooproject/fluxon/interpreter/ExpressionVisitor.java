package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.expression.*;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;

/**
 * 表达式求值器
 * 处理所有表达式类型的求值
 */
public class ExpressionVisitor extends AbstractVisitor {

    /**
     * 构造函数
     *
     * @param interpreter 解释器实例
     * @param environment 当前环境
     */
    public ExpressionVisitor(Interpreter interpreter, Environment environment) {
        super(interpreter, environment);
    }

    /**
     * 访问并评估表达式
     *
     * @param expression 表达式对象
     * @return 求值结果
     */
    @Override
    public Object visitExpression(Expression expression) {
        // 直接处理字面量
        switch (expression.getExpressionType()) {
            // 空值
            case NULL:
                return null;
            // 标识符
            case IDENTIFIER:
                return ((Identifier) expression).getValue();
            // 字符串
            case STRING_LITERAL:
                return ((StringLiteral) expression).getValue();
            // 整型
            case INT_LITERAL:
                return ((IntLiteral) expression).getValue();
            // 长整型
            case LONG_LITERAL:
                return ((LongLiteral) expression).getValue();
            // 单精度
            case FLOAT_LITERAL:
                return ((FloatLiteral) expression).getValue();
            // 双精度
            case DOUBLE_LITERAL:
                return ((DoubleLiteral) expression).getValue();
            // 布尔值
            case BOOLEAN_LITERAL:
                return ((BooleanLiteral) expression).getValue();
            // 其他表达式
            default:
                ExpressionEvaluator<Expression> evaluator = registry.getExpression(expression.getExpressionType());
                if (evaluator != null) {
                    return evaluator.evaluate(interpreter, expression);
                }
                throw new RuntimeException("Unknown expression type: " + expression.getClass().getName());
        }
    }

    /**
     * 不支持的其他类型的 visit 方法
     */
    @Override
    public Object visitStatement(Statement statement) {
        throw new UnsupportedOperationException("Expression evaluator does not support evaluating statements.");
    }

    @Override
    public Object visitDefinition(Definition definition) {
        throw new UnsupportedOperationException("Expression evaluator does not support evaluating definitions.");
    }
} 