package org.tabooproject.fluxon.interpreter;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.SourceTrace;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.expression.LambdaExpression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntimeError;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluxon 解释器
 * 负责执行 AST 节点
 */
public class Interpreter {

    // 当前环境
    @NotNull
    private Environment environment;
    // 缓存 lambda -> UserFunction，避免循环中重复创建实例
    private final Map<LambdaExpression, UserFunction> lambdaCache = new IdentityHashMap<>();

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
        Environment previous = this.environment;
        this.environment = env;
        try {
            return evaluate(result);
        } finally {
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
        try {
            switch (result.getType()) {
                case EXPRESSION:
                    return evaluateExpression((Expression) result);
                case STATEMENT:
                    return evaluateStatement((Statement) result);
                case DEFINITION:
                    return evaluateDefinition((Definition) result);
                default:
                    return null;
            }
        } catch (FluxonRuntimeError ex) {
            attachSource(ex, result);
            throw ex;
        }
    }

    /**
     * 直接评估表达式，跳过 ResultType 分派开销
     */
    public Object evaluateExpression(Expression expression) {
        try {
            return expression.getExpressionType().evaluator.evaluate(this, expression);
        } catch (FluxonRuntimeError ex) {
            attachSource(ex, expression);
            throw ex;
        }
    }

    /**
     * 直接评估语句
     */
    public Object evaluateStatement(Statement statement) {
        try {
            return statement.getStatementType().evaluator.evaluate(this, statement);
        } catch (FluxonRuntimeError ex) {
            attachSource(ex, statement);
            throw ex;
        }
    }

    /**
     * 直接评估定义
     */
    public Object evaluateDefinition(Definition definition) {
        try {
            if (definition instanceof FunctionDefinition) {
                FunctionDefinition funcDef = (FunctionDefinition) definition;
                UserFunction function = new UserFunction(funcDef, this);
                environment.defineRootFunction(funcDef.getName(), function);
                return function;
            }
            throw new RuntimeException("Unknown definition type: " + definition.getClass().getName());
        } catch (FluxonRuntimeError ex) {
            attachSource(ex, definition);
            throw ex;
        }
    }

    /**
     * 获取或创建 lambda 对应的 UserFunction，重复求值时复用实例
     */
    @NotNull
    public UserFunction getOrCreateLambda(@NotNull LambdaExpression expr) {
        return lambdaCache.computeIfAbsent(expr, e -> new UserFunction(e.toFunctionDefinition("main"), this));
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

    private void attachSource(FluxonRuntimeError error, ParseResult result) {
        if (error.getSourceExcerpt() == null) {
            error.attachSource(SourceTrace.get(result));
        }
    }
}
