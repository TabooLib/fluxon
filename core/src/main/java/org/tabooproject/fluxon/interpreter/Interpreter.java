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
import org.tabooproject.fluxon.runtime.FunctionContextPool;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError;

import static org.tabooproject.fluxon.runtime.stdlib.Operations.isTrue;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluxon 解释器
 * 负责执行 AST 节点，线程私有的执行上下文
 */
public class Interpreter {

    // 双槽结果传递
    public long resultPrimitive;
    public Object resultRef;

    // 当前环境
    @NotNull
    private Environment environment;
    // 线程本地 FunctionContextPool 缓存
    @NotNull
    private final FunctionContextPool pool;
    // 缓存 lambda -> UserFunction，避免循环中重复创建实例
    private final Map<LambdaExpression, UserFunction> lambdaCache = new IdentityHashMap<>();
    // root 变量类型
    private Map<String, Type> rootVariableTypes = null;

    public Interpreter(@NotNull Environment environment) {
        this.environment = environment;
        this.pool = FunctionContextPool.local();
    }

    /**
     * 创建子解释器（独立 result slots + lambdaCache，共享 environment）
     * 用于异步执行时隔离线程间的 result 竞争
     */
    public Interpreter createChild() {
        Interpreter child = new Interpreter(this.environment);
        child.rootVariableTypes = this.rootVariableTypes;
        return child;
    }

    /**
     * 根据返回的 Type 读取对应槽位并装箱
     */
    public Object getResultBoxed(Type type) {
        if (type == Type.VOID) return null;
        if (type.isPrimitive()) return Type.box(resultPrimitive, type);
        return resultRef;
    }

    /**
     * 直接判断结果是否为真（避免装箱）
     */
    public boolean isResultTrue(Type type) {
        if (!type.isPrimitive()) return isTrue(resultRef);
        if (type == Type.F) return ((int) resultPrimitive & 0x7FFFFFFF) != 0;
        if (type == Type.D) return (resultPrimitive & 0x7FFFFFFFFFFFFFFFL) != 0;
        return resultPrimitive != 0;
    }


    /**
     * 执行 AST
     * 结果存入 resultRef
     */
    public void execute(List<ParseResult> parseResults) {
        for (ParseResult result : parseResults) {
            if (result instanceof Definition) {
                evaluateDefinition((Definition) result);
            }
        }
        // 第二遍：真正执行表达式和语句；定义节点已经处理过，直接跳过即可
        resultRef = null;
        for (ParseResult result : parseResults) {
            if (!(result instanceof Definition)) {
                Type t = evaluate(result);
                resultRef = getResultBoxed(t);
            }
        }
    }

    /**
     * 使用指定环境执行单个节点
     * 结果存入双槽
     */
    public Type executeWithEnvironment(ParseResult result, Environment env) {
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
     * 使用 instanceof 进行类型判断，避免 getType() 的虚方法调用开销
     */
    public Type evaluate(ParseResult result) {
        try {
            consumeCostIfNeeded(result);
            if (result instanceof Expression) {
                return evaluateExpression((Expression) result);
            } else if (result instanceof Statement) {
                return evaluateStatement((Statement) result);
            } else if (result instanceof Definition) {
                evaluateDefinition((Definition) result);
                return Type.OBJECT;
            }
            resultRef = null;
            return Type.VOID;
        } catch (FluxonRuntimeError ex) {
            attachSource(ex, result);
            throw ex;
        }
    }

    /**
     * 直接评估表达式，使用缓存的 evaluator 引用避免 getExpressionType() 调用
     */
    public Type evaluateExpression(Expression expression) {
        try {
            return expression.getEvaluator().evaluate(this, expression);
        } catch (FluxonRuntimeError ex) {
            attachSource(ex, expression);
            throw ex;
        }
    }

    /**
     * 直接评估语句，使用缓存的 evaluator 引用避免 getStatementType() 调用
     */
    public Type evaluateStatement(Statement statement) {
        try {
            return statement.getEvaluator().evaluate(this, statement);
        } catch (FluxonRuntimeError ex) {
            attachSource(ex, statement);
            throw ex;
        }
    }

    /**
     * 直接评估定义
     */
    public void evaluateDefinition(Definition definition) {
        try {
            if (definition instanceof FunctionDefinition) {
                FunctionDefinition funcDef = (FunctionDefinition) definition;
                UserFunction function = new UserFunction(funcDef, this);
                environment.defineRootFunction(funcDef.getName(), function);
                resultRef = function;
                return;
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

    /**
     * 获取 FunctionContextPool
     */
    @NotNull
    public FunctionContextPool getPool() {
        return pool;
    }

    /**
     * 为错误添加源信息（如果不存在）
     */
    private void attachSource(FluxonRuntimeError error, ParseResult result) {
        if (error.getSourceExcerpt() == null) {
            error.attachSource(SourceTrace.get(result));
        }
    }

    /**
     * 消耗执行成本（如果启用且结果为语句）
     */
    private void consumeCostIfNeeded(ParseResult result) {
        if (result instanceof Statement) {
            environment.consumeCostStep();
        }
    }

    /**
     * 消耗执行成本一步（委托给 environment）
     */
    public void consumeCostStep() {
        environment.consumeCostStep();
    }

    /**
     * 设置执行成本限制（委托给 environment）
     */
    public void setCostLimit(long costLimit) {
        environment.setCostLimit(costLimit);
    }

    /**
     * 禁用执行成本限制（委托给 environment）
     */
    public void disableCostLimit() {
        environment.disableCostLimit();
    }

    /**
     * 设置每次执行成本消耗（委托给 environment）
     */
    public void setCostPerStep(long costPerStep) {
        environment.setCostPerStep(costPerStep);
    }

    /**
     * 获取执行成本限制
     */
    public long getCostLimit() {
        return environment.getCostLimit();
    }

    /**
     * 获取当前执行成本剩余
     */
    public long getCostRemaining() {
        return environment.getCostRemaining();
    }

    /**
     * 获取每次执行成本消耗
     */
    public long getCostPerStep() {
        return environment.getCostPerStep();
    }

    /**
     * 获取是否启用执行成本限制
     */
    public boolean isCostLimitEnabled() {
        return environment.isCostLimitEnabled();
    }

    /**
     * 设置变量类型映射（转换为数组并设置到当前 Environment）
     */
    public void setVariableTypes(Map<Integer, Type> types) {
        if (types == null || types.isEmpty()) {
            this.environment.setVariableTypes(null);
            return;
        }
        int maxPos = types.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
        if (maxPos >= 0) {
            Type[] arr = new Type[maxPos + 1];
            types.forEach((pos, type) -> {
                if (pos >= 0) arr[pos] = type;
            });
            this.environment.setVariableTypes(arr);
        } else {
            this.environment.setVariableTypes(null);
        }
    }

    /**
     * 设置 root 变量类型
     */
    public void setRootVariableTypes(Map<String, Type> types) {
        this.rootVariableTypes = types;
    }

    /**
     * 获取 root 变量类型
     */
    public Type getRootVariableType(String name) {
        if (rootVariableTypes != null) {
            Type type = rootVariableTypes.get(name);
            if (type != null) return type;
        }
        return Type.OBJECT;
    }
}
