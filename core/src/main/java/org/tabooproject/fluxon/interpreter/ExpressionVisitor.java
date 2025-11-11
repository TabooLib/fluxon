package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.parser.expression.Expression;

/**
 * 表达式求值器
 * 处理所有表达式类型的求值
 */
public class ExpressionVisitor extends AbstractVisitor {

    public ExpressionVisitor(Interpreter interpreter) {
        super(interpreter);
    }

    @Override
    public Object visitExpression(Expression expression) {
        return expression.getExpressionType().evaluator.evaluate(interpreter, expression);
    }
}