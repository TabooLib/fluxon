package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.ClosureFunction;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.LambdaExpression;

/**
 * Lambda 表达式求值器
 * 将 Lambda 表达式转换为闭包函数对象
 */
public class LambdaEvaluator extends ExpressionEvaluator<LambdaExpression> {

    @Override
    public Object evaluate(Interpreter interpreter, LambdaExpression result) {
        return new ClosureFunction(result, interpreter, interpreter.getEnvironment());
    }

    @Override
    public ExpressionType getType() {
        return ExpressionType.LAMBDA;
    }
}
