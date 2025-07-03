package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.interpreter.error.EvaluatorNotFoundException;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.runtime.Environment;

/**
 * 表达式求值器
 * 处理所有表达式类型的求值
 */
public class ExpressionVisitor extends AbstractVisitor {

    public ExpressionVisitor(Interpreter interpreter, Environment environment) {
        super(interpreter, environment);
    }

    @Override
    public Object visitExpression(Expression expression) {
        ExpressionEvaluator<Expression> evaluator = registry.getExpression(expression.getExpressionType());
        if (evaluator != null) {
            return evaluator.evaluate(interpreter, expression);
        }
        throw new EvaluatorNotFoundException("Unknown expression type: " + expression.getClass().getName());
    }
}