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
import org.tabooproject.fluxon.runtime.FunctionContext;
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

    // 单字段结果传递
    public long resultPrimitive;
    public Object resultRef;

    // return 信号（替代 ReturnValue 异常，避免堆分配和异常分发开销）
    public boolean hasReturn;
    public Object returnValue;

    // 缓存 costLimitEnabled，避免每次通过 environment.root.rootState 间接读取
    boolean costLimitEnabled;

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
    // env-free 模式下的活跃 FunctionContext（null 表示使用传统 Environment 路径）
    public FunctionContext<?> activeFunctionContext = null;

    public Interpreter(@NotNull Environment environment) {
        this.environment = environment;
        this.pool = FunctionContextPool.local();
        this.costLimitEnabled = environment.isCostLimitEnabled();
    }

    /**
     * 创建子解释器（独立结果字段，独立 Environment）
     * 子 Environment 通过 root 共享函数定义和根变量，
     * 但 target 是值拷贝，避免并发线程间 save/restore target 竞态
     */
    public Interpreter createChild() {
        Environment childEnv = new Environment(this.environment, 0);
        Interpreter child = new Interpreter(childEnv);
        child.rootVariableTypes = this.rootVariableTypes;
        return child;
    }

    /**
     * 读取单字段结果并装箱
     */
    public Object getResultBoxed(Type type) {
        if (type == Type.VOID) return null;
        if (type.isPrimitive()) return Type.box(resultPrimitive, type);
        return resultRef;
    }

    /**
     * 直接判断单字段结果是否为真（避免装箱）
     */
    public boolean isResultTrue(Type type) {
        if (!type.isPrimitive()) return isTrue(resultRef);
        if (type == Type.F) return ((int) resultPrimitive & 0x7FFFFFFF) != 0;
        if (type == Type.D) return (resultPrimitive & 0x7FFFFFFFFFFFFFFFL) != 0;
        return resultPrimitive != 0;
    }

    /**
     * 执行 AST，返回最终结果
     */
    public Object execute(List<ParseResult> parseResults) {
        for (ParseResult result : parseResults) {
            if (result instanceof Definition) {
                evaluateDefinition((Definition) result);
            }
        }
        Object finalResult = null;
        for (ParseResult result : parseResults) {
            if (!(result instanceof Definition)) {
                Type t = evaluate(result);
                finalResult = getResultBoxed(t);
            }
        }
        return finalResult;
    }

    /**
     * 使用指定环境执行单个节点
     */
    public Type executeWithEnvironment(ParseResult result, Environment env) {
        Environment previous = this.environment;
        FunctionContext<?> previousContext = this.activeFunctionContext;
        this.environment = env;
        // Environment 路径必须隔离调用方的 env-free 局部槽位，避免 Lambda 参数被外层 FunctionContext 串读。
        this.activeFunctionContext = null;
        try {
            return evaluate(result);
        } finally {
            this.environment = previous;
            this.activeFunctionContext = previousContext;
        }
    }

    /**
     * 使用 FunctionContext 作为变量存储执行单个节点（env-free 模式）
     * 局部变量通过 FunctionContext 的数组读写，跳过 Environment 分配
     */
    public Type executeWithFunctionContext(ParseResult result, FunctionContext<?> ctx) {
        FunctionContext<?> previous = this.activeFunctionContext;
        this.activeFunctionContext = ctx;
        try {
            return evaluate(result);
        } finally {
            this.activeFunctionContext = previous;
        }
    }

    /**
     * 评估单个解析结果
     * Expression 路径不包裹 try-catch（evaluateExpression 已有独立的异常处理）
     */
    public Type evaluate(ParseResult result) {
        if (result instanceof Expression) {
            return evaluateExpression((Expression) result);
        }
        try {
            if (result instanceof Statement) {
                if (costLimitEnabled) environment.consumeCostStep();
                return evaluateStatement((Statement) result);
            }
            if (result instanceof Definition) {
                evaluateDefinition((Definition) result);
                return Type.OBJECT;
            }
            return Type.VOID;
        } catch (FluxonRuntimeError ex) {
            attachSource(ex, result);
            throw ex;
        }
    }

    /**
     * 评估表达式
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
     * 评估语句
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
     * 评估定义
     */
    public void evaluateDefinition(Definition definition) {
        try {
            if (definition instanceof FunctionDefinition) {
                FunctionDefinition funcDef = (FunctionDefinition) definition;
                UserFunction function = new UserFunction(funcDef, this);
                environment.defineRootFunction(funcDef.getName(), function);
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
     * 为错误添加源信息
     */
    private void attachSource(FluxonRuntimeError error, ParseResult result) {
        if (error.getSourceExcerpt() == null) {
            error.attachSource(SourceTrace.get(result));
        }
    }

    /**
     * 消耗执行成本一步（costLimitEnabled 为 false 时直接跳过）
     */
    public void consumeCostStep() {
        if (costLimitEnabled) environment.consumeCostStep();
    }

    /**
     * 设置执行成本限制
     */
    public void setCostLimit(long costLimit) {
        environment.setCostLimit(costLimit);
        this.costLimitEnabled = true;
    }

    /**
     * 禁用执行成本限制
     */
    public void disableCostLimit() {
        environment.disableCostLimit();
        this.costLimitEnabled = false;
    }

    /**
     * 设置每次执行成本消耗
     */
    public void setCostPerStep(long costPerStep) {
        environment.setCostPerStep(costPerStep);
    }

    public long getCostLimit() {
        return environment.getCostLimit();
    }

    public long getCostRemaining() {
        return environment.getCostRemaining();
    }

    public long getCostPerStep() {
        return environment.getCostPerStep();
    }

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
