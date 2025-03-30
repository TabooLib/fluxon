package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.AwaitExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AwaitEvaluator extends ExpressionEvaluator<AwaitExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.AWAIT;
    }

    @Override
    public Object evaluate(Interpreter interpreter, AwaitExpression result) {
        // 评估 await 表达式中的值
        Object value = interpreter.evaluate(result.getExpression());
        // 处理不同类型的异步结果
        if (value instanceof CompletableFuture<?>) {
            // 如果是 CompletableFuture，等待其完成并返回结果
            try {
                return ((CompletableFuture<?>) value).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Error while awaiting future: " + e.getMessage(), e);
            }
        } else if (value instanceof Future<?>) {
            // 如果是普通的 Future，等待其完成并返回结果
            try {
                return ((Future<?>) value).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Error while awaiting future: " + e.getMessage(), e);
            }
        }
        // 如果不是异步类型，直接返回值
        return value;
    }
}
