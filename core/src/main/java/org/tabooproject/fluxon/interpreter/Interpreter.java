package org.tabooproject.fluxon.interpreter;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;

import java.util.List;

/**
 * Fluxon 解释器
 * 负责执行 AST 节点
 */
public class Interpreter {

    // 当前环境
    @NotNull
    private Environment environment;

    public Interpreter(@NotNull Environment environment) {
        this.environment = environment;
    }

    /**
     * 执行 AST
     *
     * @param parseResults 解析结果列表
     * @return 最后一个表达式的执行结果
     */
    public Object execute(List<ParseResult> parseResults) {
        // 第一遍：提前注册所有顶层函数定义，支持前向引用
        for (ParseResult result : parseResults) {
            if (result.getType() == ParseResult.ResultType.DEFINITION) {
                evaluateDefinition((Definition) result);
            }
        }
        // 第二遍：真正执行表达式和语句；定义节点已经处理过，直接跳过即可
        Object lastValue = null;
        for (ParseResult result : parseResults) {
            if (result.getType() != ParseResult.ResultType.DEFINITION) {
                lastValue = evaluate(result);
            }
        }
        return lastValue;
    }

    /**
     * 使用指定环境执行单个节点
     *
     * @param result 解析结果
     * @param env    执行环境
     * @return 执行结果
     */
    public Object executeWithEnvironment(ParseResult result, Environment env) {
        // 保存当前环境
        Environment previous = this.environment;
        this.environment = env;
        try {
            return evaluate(result);
        } finally {
            // 恢复环境
            this.environment = previous;
        }
    }

    /**
     * 评估单个解析结果
     *
     * @param result 解析结果
     * @return 执行结果
     */
    public Object evaluate(ParseResult result) {
        switch (result.getType()) {
            case EXPRESSION:
                return evaluateExpression((Expression) result);
            case STATEMENT:
                return evaluateStatement((Statement) result);
            case DEFINITION:
                return evaluateDefinition((Definition) result);
        }
        return null;
    }

    /**
     * 直接评估表达式，跳过 ResultType 分派开销
     */
    public Object evaluateExpression(Expression expression) {
        return expression.getExpressionType().evaluator.evaluate(this, expression);
    }

    /**
     * 直接评估语句
     */
    public Object evaluateStatement(Statement statement) {
        return statement.getStatementType().evaluator.evaluate(this, statement);
    }

    /**
     * 直接评估定义
     */
    public Object evaluateDefinition(Definition definition) {
        if (definition instanceof FunctionDefinition) {
            FunctionDefinition funcDef = (FunctionDefinition) definition;
            // 创建函数对象，捕获当前环境
            UserFunction function = new UserFunction(funcDef, this);
            // 在当前环境中定义函数
            environment.defineRootFunction(funcDef.getName(), function);
            return function;
        }
        throw new RuntimeException("Unknown definition type: " + definition.getClass().getName());
    }

    /**
     * 设置当前环境
     */
    public void setEnvironment(@NotNull Environment environment) {
        this.environment = environment;
    }

    /**
     * 获取当前环境
     */
    @NotNull
    public Environment getEnvironment() {
        return environment;
    }
}