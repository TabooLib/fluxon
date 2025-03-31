package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.List;

/**
 * Fluxon 解释器
 * 负责执行 AST 节点
 */
public class Interpreter {

    // 当前环境
    private Environment environment;

    // 求值器
    private final DefinitionVisitor definitionVisitor;
    private final ExpressionVisitor expressionVisitor;
    private final StatementVisitor statementVisitor;

    public Interpreter() {
        this(new Environment());
    }

    public Interpreter(Environment environment) {
        this.environment = environment;
        this.definitionVisitor = new DefinitionVisitor(this, environment);
        this.expressionVisitor = new ExpressionVisitor(this, environment);
        this.statementVisitor = new StatementVisitor(this, environment);
        // 使用注册中心初始化环境
        FluxonRuntime.getInstance().initializeEnvironment(environment);
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
        definitionVisitor.setEnvironment(env);
        expressionVisitor.setEnvironment(env);
        statementVisitor.setEnvironment(env);
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
            case DEFINITION:
                return definitionVisitor.visitDefinition((Definition) result);
            case EXPRESSION:
                return expressionVisitor.visitExpression((Expression) result);
            case STATEMENT:
                return statementVisitor.visitStatement((Statement) result);
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

    /**
     * 进入新的环境
     */
    public void enterScope() {
        environment = new Environment(environment);
    }

    /**
     * 返回上级环境
     */
    public void exitScope() {
        environment = environment.getParent();
    }
} 