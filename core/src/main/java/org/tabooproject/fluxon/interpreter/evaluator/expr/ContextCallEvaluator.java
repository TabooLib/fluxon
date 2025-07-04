package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ContextCall;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.FunctionCall;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.parser.statement.Block;
import org.tabooproject.fluxon.runtime.ContextEnvironment;
import org.tabooproject.fluxon.runtime.Environment;

/**
 * 上下文调用表达式求值器
 * 处理形如 "text" :: replace("a", "b") 或 "text" :: { replace("a", "b"); length } 的表达式
 */
public class ContextCallEvaluator extends ExpressionEvaluator<ContextCall> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.CONTEXT_CALL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ContextCall expression) {
        // 计算目标值
        Object target = interpreter.evaluate(expression.getTarget());
        // 创建新的环境作用域，将目标值绑定为 'this'
        Environment contextEnv = new ContextEnvironment(interpreter.getEnvironment(), target);
        // 根据上下文类型进行不同的处理
        if (expression.getContext() instanceof Block) {
            // 块表达式：在上下文环境中执行所有语句，返回最后一个表达式的值
            Block block = (Block) expression.getContext();
            Object result = null;
            for (int i = 0; i < block.getStatements().size(); i++) {
                result = interpreter.executeWithEnvironment(block.getStatements().get(i), contextEnv);
            }
            return result;
        } else if (expression.getContext() instanceof FunctionCall) {
            // 函数调用：将目标值作为第一个参数传入
            FunctionCall call = (FunctionCall) expression.getContext();
            return evaluateContextFunctionCall(interpreter, target, call);
        } else if (expression.getContext() instanceof Identifier) {
            // 标识符：作为属性访问
            Identifier identifier = (Identifier) expression.getContext();
            return evaluateContextProperty(interpreter, target, identifier);
        } else {
            // 其他表达式：直接在上下文环境中求值
            return interpreter.executeWithEnvironment(expression.getContext(), contextEnv);
        }
    }

    /**
     * 在上下文中执行函数调用
     * 将目标值作为第一个参数传入函数
     */
    private Object evaluateContextFunctionCall(Interpreter interpreter, Object target, FunctionCall call) {
        // 创建新的参数列表，将目标值作为第一个参数
        java.util.List<org.tabooproject.fluxon.parser.ParseResult> newArgs = new java.util.ArrayList<>();
        newArgs.add(new org.tabooproject.fluxon.parser.expression.literal.StringLiteral(target.toString()));

        // 添加原有的参数
        newArgs.addAll(call.getArguments());

        // 创建新的函数调用
        FunctionCall contextCall = new FunctionCall(call.getCallee(), newArgs);

        return interpreter.evaluate(contextCall);
    }

    /**
     * 在上下文中访问属性
     * 目前简单返回目标值的字符串表示的长度（作为示例）
     */
    private Object evaluateContextProperty(Interpreter interpreter, Object target, Identifier identifier) {
        String propertyName = identifier.getValue();

        // 根据属性名返回相应的值
        switch (propertyName) {
            case "length":
                return target.toString().length();
            case "toString":
                return target.toString();
            default:
                // 尝试在当前环境中查找该标识符
                try {
                    return interpreter.evaluate(identifier);
                } catch (Exception e) {
                    // 如果找不到，返回属性名本身作为字符串
                    return propertyName;
                }
        }
    }
}