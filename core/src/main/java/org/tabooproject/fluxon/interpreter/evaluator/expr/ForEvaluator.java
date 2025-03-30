package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.destructure.DestructuringRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.ForExpression;
import org.tabooproject.fluxon.runtime.Environment;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ForEvaluator extends ExpressionEvaluator<ForExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.FOR;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ForExpression result) {
        // 评估集合表达式
        Object collection = interpreter.evaluate(result.getCollection());

        // 创建迭代器，根据集合类型进行不同处理
        Iterator<?> iterator;

        if (collection instanceof List) {
            iterator = ((List<?>) collection).iterator();
        } else if (collection instanceof Map) {
            iterator = ((Map<?, ?>) collection).entrySet().iterator();
        } else if (collection instanceof Iterable) {
            iterator = ((Iterable<?>) collection).iterator();
        } else if (collection instanceof Object[]) {
            iterator = Arrays.asList((Object[]) collection).iterator();
        } else if (collection != null) {
            throw new RuntimeException("Cannot iterate over " + collection.getClass().getName());
        } else {
            throw new RuntimeException("Cannot iterate over null");
        }

        // 获取变量名列表
        List<String> variables = result.getVariables();

        // 创建新环境进行迭代
        interpreter.enterScope();
        Object last = null;
        try {
            Environment environment = interpreter.getEnvironment();
            // 迭代集合元素
            while (iterator.hasNext()) {
                // 使用解构器注册表执行解构
                DestructuringRegistry.getInstance().destructure(environment, variables, iterator.next());
                // 执行循环体
                last = interpreter.evaluate(result.getBody());
            }
        } finally {
            interpreter.exitScope();
        }
        return last;
    }
}
