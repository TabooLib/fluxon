package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.CommandRegistry;
import org.tabooproject.fluxon.parser.DomainRegistry;
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError;
import org.tabooproject.fluxon.runtime.error.FunctionNotFoundError;
import org.tabooproject.fluxon.runtime.java.Export;
import org.tabooproject.fluxon.util.KV;

import java.io.PrintStream;
import java.util.*;

/**
 * 运行时环境
 * 用于管理运行时期间的函数和变量
 */
@SuppressWarnings("DataFlowIssue")
public class Environment {

    // 类型
    public static final Type TYPE = new Type(Environment.class);

    // 根环境
    @NotNull
    protected final Environment root;
    @Nullable
    protected final Environment parent;
    @Nullable
    protected final EnvironmentState rootState; // non-null only for root

    // 局部变量 - 原始类型
    protected long[] localPrimitives;
    // 局部变量 - 引用类型
    @Nullable
    protected Object[] localRefs;
    // 局部变量对照表
    @Nullable
    protected String[] localVariableNames;
    // 上下文目标
    @Nullable
    protected Object target;

    /**
     * 创建顶层环境（全局环境）
     */
    public Environment(@NotNull Map<String, OverloadSet> functions, @NotNull Map<String, Object> values) {
        this(functions, values, 0);
    }

    /**
     * 创建顶层环境（全局环境）- 指定局部变量数量
     */
    public Environment(@NotNull Map<String, OverloadSet> functions, @NotNull Map<String, Object> values, int localVariableCount) {
        this.root = this;
        this.parent = null;
        this.rootState = new EnvironmentState(functions, values);
        this.localPrimitives = localVariableCount > 0 ? new long[localVariableCount] : null;
        this.localRefs = localVariableCount > 0 ? new Object[localVariableCount] : null;
        this.localVariableNames = localVariableCount > 0 ? new String[localVariableCount] : null;
    }

    /**
     * 创建子环境（函数环境）
     *
     * @param parentEnv 父环境
     */
    public Environment(@NotNull Environment parentEnv, int localVariables) {
        this.root = parentEnv.root;
        this.parent = parentEnv;
        this.rootState = null;
        this.localPrimitives = localVariables > 0 ? new long[localVariables] : null;
        this.localRefs = localVariables > 0 ? new Object[localVariables] : null;
        this.localVariableNames = localVariables > 0 ? new String[localVariables] : null;
        this.target = parentEnv.target;
    }

    /**
     * 获取根环境
     * 如果自己是根环境，则返回自己
     */
    @Export
    @NotNull
    public Environment getRoot() {
        return root;
    }

    /**
     * 获取父环境
     */
    @Export
    @Nullable
    public Environment getParent() {
        return parent;
    }

    // region 函数定义与获取

    /**
     * 在根环境中定义函数
     *
     * @param name  函数名
     * @param value 函数对象
     */
    public void defineRootFunction(String name, Function value) {
        EnvironmentState m = root.rootState;
        m.functions.computeIfAbsent(name, OverloadSet::new).add(value);
        if (m.userFunctionNames == null) {
            m.userFunctionNames = new HashSet<>();
        }
        m.userFunctionNames.add(name);
    }

    /**
     * 在根环境中定义扩展函数
     *
     * @param extensionClass 扩展类
     * @param name           函数名
     * @param value          函数对象
     */
    public void defineRootExtensionFunction(Class<?> extensionClass, String name, Function value) {
        FluxonRuntime.getInstance().getExtensionFunctions().computeIfAbsent(name, k -> new LinkedHashMap<>()).put(extensionClass, value);
    }

    /**
     * 获取函数（只查找根环境）
     *
     * @param name 函数名
     * @return 函数值
     * @throws FluxonRuntimeError 如果函数不存在
     */
    @Export
    @NotNull
    public Function getFunction(String name) {
        OverloadSet set = root.rootState.functions.get(name);
        if (set != null) {
            Function function = set.first();
            if (function != null) {
                return function;
            }
        }
        throw new FunctionNotFoundError(this, null, name, 0, -1, -1);
    }

    /**
     * 获取函数（只查找根环境，返回第一个重载）
     *
     * @param name 函数名
     * @return 函数值
     */
    @Export
    @Nullable
    public Function getFunctionOrNull(String name) {
        OverloadSet set = root.rootState.functions.get(name);
        return set != null ? set.first() : null;
    }

    /**
     * 获取扩展函数（只查找根环境）
     *
     * @param extensionClass 扩展类
     * @param name           函数名
     * @return 函数值
     * @throws FluxonRuntimeError 如果函数不存在
     */
    @NotNull
    public Function getExtensionFunction(Class<?> extensionClass, String name, int index) {
        Function function = getExtensionFunctionOrNull(extensionClass, name, index);
        if (function != null) {
            return function;
        }
        throw new FunctionNotFoundError(this, extensionClass, name, 0, -1, index);
    }

    /**
     * 获取扩展函数（只查找根环境）
     *
     * @param extensionClass 扩展类
     * @param name           函数名
     * @return 函数值
     */
    @Nullable
    public Function getExtensionFunctionOrNull(Class<?> extensionClass, String name, int index) {
        if (index != -1) {
            // 使用派发表进行优化解析
            ExtensionDispatchTable dispatchTable = FluxonRuntime.getInstance().getCachedDispatchTables()[index];
            return dispatchTable.resolve(extensionClass);
        }
        // 回退逻辑，使用名称检索
        // 需要进行线性扫描（用于动态注册的扩展函数）
        else {
            Map<Class<?>, Function> classFunctionMap = FluxonRuntime.getInstance().getExtensionFunctions().get(name);
            if (classFunctionMap != null) {
                // 查找精确匹配
                Function exact = classFunctionMap.get(extensionClass);
                if (exact != null) {
                    return exact;
                }
                // 查找可赋值匹配
                for (Map.Entry<Class<?>, Function> entry : classFunctionMap.entrySet()) {
                    if (entry.getKey().isAssignableFrom(extensionClass)) {
                        return entry.getValue();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取根环境中的所有函数重载集合
     */
    @Export
    public Map<String, OverloadSet> getRootFunctions() {
        return root.rootState.functions;
    }

    /**
     * 获取用户动态定义的函数（不包含系统函数）
     * 用于解析器识别运行时定义的函数
     *
     * @return 用户定义的函数映射，如果没有则返回空 map
     */
    public Map<String, OverloadSet> getUserFunctions() {
        EnvironmentState m = root.rootState;
        if (m.userFunctionNames == null || m.userFunctionNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, OverloadSet> result = new HashMap<>();
        for (String name : m.userFunctionNames) {
            OverloadSet set = m.functions.get(name);
            if (set != null) {
                result.put(name, set);
            }
        }
        return result;
    }

    /**
     * 获取根环境中的所有系统函数
     */
    public Function[] getRootSystemFunctions() {
        return FluxonRuntime.getInstance().getCachedSystemFunctions();
    }

    /**
     * 获取根环境中的所有扩展函数
     */
    @Export
    public Map<String, Map<Class<?>, Function>> getRootExtensionFunctions() {
        return FluxonRuntime.getInstance().getExtensionFunctions();
    }

    /**
     * 获取根环境中的所有系统扩展函数
     */
    public KV<Class<?>, Function>[][] getRootSystemExtensionFunctions() {
        return FluxonRuntime.getInstance().getCachedSystemExtensionFunctions();
    }

    /**
     * 获取根环境中的所有扩展函数派发表
     */
    public ExtensionDispatchTable[] getRootDispatchTables() {
        return FluxonRuntime.getInstance().getCachedDispatchTables();
    }

    // endregion

    // region 变量定义与获取

    /**
     * 初始化根层级局部变量数组（用于 _ 前缀变量）
     * 仅在根环境上调用有效，且仅在未初始化时生效
     *
     * @param count 局部变量数量
     */
    public void initializeRootLocalVariables(int count) {
        if (this == root && count > 0 && localRefs == null) {
            this.localPrimitives = new long[count];
            this.localRefs = new Object[count];
            this.localVariableNames = new String[count];
        }
    }

    /**
     * 在根环境中定义变量
     *
     * @param name  变量名
     * @param value 变量值
     */
    public void defineRootVariable(@NotNull String name, @Nullable Object value) {
        root.rootState.rootVariables.put(name, value);
    }

    /**
     * 获取根变量值
     *
     * @param name 变量名
     * @return 变量值
     */
    @Nullable
    public Object getRootVariable(@NotNull String name) {
        return root.rootState.rootVariables.get(name);
    }

    /**
     * 设置根变量值
     *
     * @param name  变量名
     * @param value 新的变量值
     */
    public void setRootVariable(@NotNull String name, @Nullable Object value) {
        root.rootState.rootVariables.put(name, value);
    }

    /**
     * 判断根变量是否存在
     *
     * @param name 变量名
     * @return 存在与否
     */
    public boolean hasRootVariable(@NotNull String name) {
        return root.rootState.rootVariables.containsKey(name);
    }

    // region 局部变量 - 引用类型

    /**
     * 获取局部引用变量（支持闭包穿透：当索引超出当前环境时走父链）
     *
     * @param index 局部变量索引
     * @return 变量值
     */
    @Nullable
    public Object getLocalRef(int index) {
        if (localRefs != null && index < localRefs.length) {
            return localRefs[index];
        }
        if (parent != null) {
            return parent.getLocalRef(index);
        }
        return null;
    }

    /**
     * 设置局部引用变量（支持闭包穿透：当索引超出当前环境时走父链）
     *
     * @param index 局部变量索引
     * @param value 变量值
     */
    public void setLocalRef(int index, @Nullable Object value) {
        if (localRefs != null && index < localRefs.length) {
            localRefs[index] = value;
        } else if (parent != null) {
            parent.setLocalRef(index, value);
        }
    }

    // endregion

    // region 局部变量 - 原始类型

    public int getLocalInt(int index) {
        return (int) localPrimitives[index];
    }

    public void setLocalInt(int index, int v) {
        localPrimitives[index] = v;
    }

    public long getLocalLong(int index) {
        return localPrimitives[index];
    }

    public void setLocalLong(int index, long v) {
        localPrimitives[index] = v;
    }

    public double getLocalDouble(int index) {
        return Double.longBitsToDouble(localPrimitives[index]);
    }

    public void setLocalDouble(int index, double v) {
        localPrimitives[index] = Double.doubleToRawLongBits(v);
    }

    public float getLocalFloat(int index) {
        return Float.intBitsToFloat((int) localPrimitives[index]);
    }

    public void setLocalFloat(int index, float v) {
        localPrimitives[index] = Float.floatToRawIntBits(v);
    }

    /**
     * 获取原始类型变量（通用方法）
     * 返回 long 位模式，调用者需根据类型转换
     */
    public long getLocalPrimitive(int index) {
        return localPrimitives[index];
    }

    /**
     * 设置原始类型变量（通用方法）
     * 存储 long 位模式，调用者需先转换
     */
    public void setLocalPrimitive(int index, long bits) {
        localPrimitives[index] = bits;
    }

    // endregion

    /**
     * 获取根环境中的所有变量
     */
    @Export
    public Map<String, Object> getRootVariables() {
        return root.rootState.rootVariables;
    }

    /**
     * 获取当前环境中的局部引用变量数组
     */
    @Nullable
    public Object[] getLocalRefs() {
        return localRefs;
    }

    /**
     * 获取当前环境中的所有局部变量名
     */
    public String[] getLocalVariableNames() {
        return localVariableNames;
    }

    // endregion

    // region 目标对象

    /**
     * 获取当前环境中的目标对象
     */
    public @Nullable Object getTarget() {
        return target;
    }

    /**
     * 设置当前环境中的目标对象
     */
    public void setTarget(@Nullable Object target) {
        this.target = target;
    }

    // endregion

    // region 输入输出

    /**
     * 获取输出流（来自根环境）
     */
    public PrintStream getOut() {
        return root.rootState.out;
    }

    /**
     * 设置输出流（写入根环境）
     */
    public void setOut(@NotNull PrintStream out) {
        root.rootState.out = Objects.requireNonNull(out, "out");
    }

    /**
     * 获取错误输出流（来自根环境）
     */
    public PrintStream getErr() {
        return root.rootState.err;
    }

    /**
     * 设置错误输出流（写入根环境）
     */
    public void setErr(@NotNull PrintStream err) {
        root.rootState.err = Objects.requireNonNull(err, "err");
    }

    // endregion

    // region 注册表

    /**
     * 获取 Command 注册表
     */
    public CommandRegistry getCommandRegistry() {
        CommandRegistry registry = root.rootState.commandRegistry;
        return registry != null ? registry : CommandRegistry.primary();
    }

    /**
     * 设置 Command 注册表
     */
    public void setCommandRegistry(CommandRegistry commandRegistry) {
        root.rootState.commandRegistry = commandRegistry;
    }

    /**
     * 获取 Domain 注册表
     */
    public DomainRegistry getDomainRegistry() {
        DomainRegistry registry = root.rootState.domainRegistry;
        return registry != null ? registry : DomainRegistry.primary();
    }

    /**
     * 设置 Domain 注册表
     */
    public void setDomainRegistry(DomainRegistry domainRegistry) {
        root.rootState.domainRegistry = domainRegistry;
    }

    // endregion

    // region 执行成本控制

    /**
     * 消耗执行成本一步（线程安全）
     */
    public void consumeCostStep() {
        EnvironmentState m = this.root.rootState;
        if (m.costLimitEnabled) {
            long step = m.costPerStep;
            long remaining = m.costRemaining.addAndGet(-step);
            if (remaining < 0) {
                m.costRemaining.addAndGet(step); // 回滚
                throw new org.tabooproject.fluxon.runtime.error.ExecutionCostExceededError(m.costLimit, remaining + step, step);
            }
        }
    }

    public void setCostLimit(long costLimit) {
        if (costLimit <= 0) {
            throw new IllegalArgumentException("costLimit must be positive");
        }
        EnvironmentState m = this.root.rootState;
        m.costLimitEnabled = true;
        m.costLimit = costLimit;
        m.costRemaining.set(costLimit);
    }

    public void disableCostLimit() {
        EnvironmentState m = this.root.rootState;
        m.costLimitEnabled = false;
        m.costLimit = Long.MAX_VALUE;
        m.costRemaining.set(Long.MAX_VALUE);
    }

    public void setCostPerStep(long costPerStep) {
        if (costPerStep <= 0) {
            throw new IllegalArgumentException("costPerStep must be positive");
        }
        this.root.rootState.costPerStep = costPerStep;
    }

    public long getCostLimit() {
        return root.rootState.costLimit;
    }

    public long getCostRemaining() {
        return root.rootState.costRemaining.get();
    }

    public long getCostPerStep() {
        return root.rootState.costPerStep;
    }

    public boolean isCostLimitEnabled() {
        return root.rootState.costLimitEnabled;
    }

    // endregion

    @Override
    public String toString() {
        return "Environment{" +
                "rootVariables=" + root.rootState.rootVariables +
                ", target=" + target +
                ", parent=" + parent +
                '}';
    }
}
