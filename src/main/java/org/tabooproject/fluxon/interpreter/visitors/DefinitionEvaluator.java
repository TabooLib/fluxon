package org.tabooproject.fluxon.interpreter.visitors;

import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.interpreter.UserFunction;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.parser.definitions.Definition;
import org.tabooproject.fluxon.parser.definitions.Definitions;
import org.tabooproject.fluxon.parser.expressions.Expression;
import org.tabooproject.fluxon.parser.statements.Statement;

/**
 * 定义求值器
 * 处理所有定义类型的求值
 */
public class DefinitionEvaluator extends AbstractVisitor {
    /**
     * 构造函数
     *
     * @param interpreter 解释器实例
     * @param environment 当前环境
     */
    public DefinitionEvaluator(Interpreter interpreter, Environment environment) {
        super(interpreter, environment);
    }

    /**
     * 访问并评估定义
     *
     * @param definition 定义对象
     * @return 执行结果
     */
    @Override
    public Object visitDefinition(Definition definition) {
        if (definition instanceof Definitions.FunctionDefinition) {
            return evaluateFunctionDefinition((Definitions.FunctionDefinition) definition);
        }
        throw new RuntimeException("Unknown definition type: " + definition.getClass().getName());
    }

    /**
     * 评估函数定义
     *
     * @param funcDef 函数定义对象
     * @return null
     */
    private UserFunction evaluateFunctionDefinition(Definitions.FunctionDefinition funcDef) {
        // 创建函数对象，捕获当前环境
        UserFunction function = new UserFunction(funcDef, environment, interpreter);
        // 在当前环境中定义函数
        environment.defineFunction(funcDef.getName(), function);
        return function;
    }

    /**
     * 不支持的其他类型的 visit 方法
     */
    @Override
    public Object visitExpression(Expression expression) {
        throw new UnsupportedOperationException("Definition evaluator does not support evaluating expressions.");
    }

    @Override
    public Object visitStatement(Statement statement) {
        throw new UnsupportedOperationException("Definition evaluator does not support evaluating statements.");
    }
} 