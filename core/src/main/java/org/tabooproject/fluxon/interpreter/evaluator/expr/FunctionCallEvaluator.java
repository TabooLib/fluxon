package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.FunctionCall;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FunctionCallEvaluator extends ExpressionEvaluator<FunctionCall> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.FUNCTION_CALL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, FunctionCall result) {
        // 评估被调用者
        Object callee = interpreter.evaluate(result.getCallee());

        // 评估参数列表
        Object[] arguments = new Object[result.getArguments().size()];
        List<ParseResult> expressionArguments = result.getArguments();
        for (int i = 0; i < expressionArguments.size(); i++) {
            ParseResult argument = expressionArguments.get(i);
            arguments[i] = interpreter.evaluate(argument);
        }

        // 获取函数
        Function function;
        if (callee instanceof Function) {
            function = ((Function) callee);
        } else {
            // 获取环境
            Environment environment = interpreter.getEnvironment();
            function = environment.getFunction(callee.toString());
        }
        if (function.isAsync()) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return function.call(arguments);
                } catch (Throwable e) {
                    throw new RuntimeException("Error while executing async function: " + e.getMessage(), e);
                }
            });
        } else {
            return function.call(arguments);
        }
    }
}
