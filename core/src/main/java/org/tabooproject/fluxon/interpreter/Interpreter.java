package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.interpreter.visitors.DefinitionEvaluator;
import org.tabooproject.fluxon.interpreter.visitors.ExpressionEvaluator;
import org.tabooproject.fluxon.interpreter.visitors.StatementEvaluator;
import org.tabooproject.fluxon.parser.definitions.Definition;
import org.tabooproject.fluxon.parser.expressions.Expression;
import org.tabooproject.fluxon.parser.statements.Statement;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.List;

/**
 * Fluxon 解释器
 * 负责执行 AST 节点
 * <p>
 * 优点
 * 清晰的架构设计：解释器采用了经典的访问者模式，通过不同的评估方法处理不同类型的AST节点，结构清晰。
 * 良好的作用域管理：通过环境类(Environment)实现了作用域链，支持变量的嵌套作用域查找和更新，这是解释器的基础功能。
 * 完整的功能实现：支持了各种表达式求值、语句执行、函数定义和调用，包括递归函数，基本覆盖了脚本语言的核心功能。
 * 异常处理机制：使用ReturnValue异常实现函数返回，这是一种高效处理非局部流程控制的常见模式。
 * 灵活的函数实现：支持原生函数和用户定义函数，原生函数通过函数式接口实现，用户函数捕获定义时环境形成闭包。
 * <p>
 * 不足
 * 错误处理不够完善：缺乏详细的错误报告机制，如行号和位置信息，对用户调试不够友好。
 * 性能考虑不足：没有对常见操作进行优化，如数字运算的装箱/拆箱操作可能影响性能。
 * 类型系统简单：没有实现类型检查和自动转换规则，可能导致运行时类型错误。
 * 缺少高级语言特性：没有实现如异常处理、模块系统、类和对象等现代语言特性。
 * 并发支持有限：虽然有async/await表达式的框架，但实际实现是同步的，没有真正的异步执行能力。
 */
public class Interpreter {

    // 当前环境
    private Environment environment;

    // 求值器
    private final ExpressionEvaluator expressionEvaluator;
    private final StatementEvaluator statementEvaluator;
    private final DefinitionEvaluator definitionEvaluator;

    /**
     * 创建解释器
     */
    public Interpreter() {
        // 初始化全局环境
        this.environment = new Environment();

        // 使用注册中心初始化环境
        FluxonRuntime.getInstance().initializeEnvironment(environment);

        // 初始化求值器
        this.expressionEvaluator = new ExpressionEvaluator(this, environment);
        this.statementEvaluator = new StatementEvaluator(this, environment);
        this.definitionEvaluator = new DefinitionEvaluator(this, environment);
    }

    /**
     * 执行 AST
     *
     * @param parseResults 解析结果列表
     * @return 最后一个表达式的执行结果
     */
    public Object execute(List<ParseResult> parseResults) {
        Object lastValue = null;
        for (ParseResult result : parseResults) {
            lastValue = evaluate(result);
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

        // 更新所有求值器的环境
        updateEvaluatorsEnvironment(env);
        try {
            return evaluate(result);
        } finally {
            // 恢复环境
            this.environment = previous;
            updateEvaluatorsEnvironment(previous);
        }
    }

    /**
     * 更新所有求值器的环境
     *
     * @param env 新环境
     */
    private void updateEvaluatorsEnvironment(Environment env) {
        expressionEvaluator.setEnvironment(env);
        statementEvaluator.setEnvironment(env);
        definitionEvaluator.setEnvironment(env);
    }

    /**
     * 评估单个解析结果
     *
     * @param result 解析结果
     * @return 执行结果
     */
    public Object evaluate(ParseResult result) {
        // 根据不同的结果类型分派到不同的求值器
        switch (result.getType()) {
            case EXPRESSION:
                return expressionEvaluator.visitExpression((Expression) result);
            case STATEMENT:
                return statementEvaluator.visitStatement((Statement) result);
            case DEFINITION:
                return definitionEvaluator.visitDefinition((Definition) result);
            default:
                throw new RuntimeException("Unknown parse result type: " + result.getType());
        }
    }

    /**
     * 获取当前环境
     *
     * @return 当前环境
     */
    public Environment getEnvironment() {
        return environment;
    }
} 