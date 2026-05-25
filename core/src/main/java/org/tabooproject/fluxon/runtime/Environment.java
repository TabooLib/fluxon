package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.compiler.ParameterInfo;
import org.tabooproject.fluxon.parser.CommandRegistry;
import org.tabooproject.fluxon.parser.DomainRegistry;
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError;
import org.tabooproject.fluxon.runtime.error.FunctionNotFoundError;
import org.tabooproject.fluxon.runtime.java.Export;
import org.tabooproject.fluxon.util.KV;

import java.io.PrintStream;
import java.util.*;

import static org.tabooproject.fluxon.runtime.sharing.SharedFunctionAdapter.*;
import static org.tabooproject.fluxon.runtime.sharing.SharedFunctionRegistry.*;

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
    // 局部变量对照表（按需分配，仅调试/反射场景使用）
    @Nullable
    protected String[] localVariableNames;
    // 局部变量数量（用于按需分配 localVariableNames）
    protected int localVariableCount;
    // 局部变量类型（解释模式下使用，作用域隔离）
    @Nullable
    protected Type[] variableTypes;
    // 闭包捕获偏移量：索引 < captureOffset 的变量走父环境链
    protected int captureOffset;
    // 捕获型 Lambda 包装缓存，同一个定义时环境内重复求值同一个函数时复用包装对象
    @Nullable
    protected Map<Function, CapturedFunction> capturedFunctionCache;
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
        this.localVariableCount = localVariableCount;
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
        this.localVariableCount = localVariables;
        this.target = parentEnv.target;
    }

    /**
     * 绑定捕获型 Lambda 的定义时环境。
     * 包装对象只保存函数实例和当前环境引用，复用不会改变捕获变量的可变语义。
     */
    @NotNull
    public Function captureFunction(@NotNull Function function) {
        if (capturedFunctionCache == null) {
            capturedFunctionCache = new IdentityHashMap<>();
        }
        CapturedFunction captured = capturedFunctionCache.get(function);
        if (captured != null) return captured;
        captured = new CapturedFunction(function, this);
        capturedFunctionCache.put(function, captured);
        return captured;
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
     * 用户定义的函数会添加到重载列表开头，以覆盖同名的系统函数
     *
     * @param name  函数名
     * @param value 函数对象
     */
    public void defineRootFunction(String name, Function value) {
        EnvironmentState m = root.rootState;
        if (m.userFunctions == null) {
            m.userFunctions = new HashMap<>();
        }
        OverloadSet existing = m.userFunctions.get(name);
        if (existing != null) {
            existing.addFirst(value);
        } else {
            OverloadSet set = new OverloadSet(name);
            set.add(value);
            m.userFunctions.put(name, set);
        }
    }

    /**
     * 在根环境中定义扩展函数
     *
     * @param extensionClass 扩展类
     * @param name           函数名
     * @param value          函数对象
     */
    public void defineRootExtensionFunction(Class<?> extensionClass, String name, Function value) {
        FluxonRuntime.getInstance().getExtensionFunctions()
                .computeIfAbsent(name, k -> new LinkedHashMap<>())
                .computeIfAbsent(extensionClass, c -> new OverloadSet(name))
                .add(value);
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
        EnvironmentState m = root.rootState;
        // 先查用户函数 overlay
        if (m.userFunctions != null) {
            OverloadSet set = m.userFunctions.get(name);
            if (set != null) {
                Function function = set.first();
                if (function != null) {
                    return function;
                }
            }
        }
        // 再查系统函数
        OverloadSet set = m.systemFunctions.get(name);
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
        EnvironmentState m = root.rootState;
        if (m.userFunctions != null) {
            OverloadSet set = m.userFunctions.get(name);
            if (set != null) {
                return set.first();
            }
        }
        OverloadSet set = m.systemFunctions.get(name);
        return set != null ? set.first() : null;
    }

    /**
     * 从全局共享注册表查找函数（不自动注册到本地运行时）
     *
     * @param owner 函数所有者
     * @param name  函数名
     * @return 适配后的本地 NativeFunction，未找到返回 null
     */
    @Nullable
    public Function getSharedFunction(String owner, String name) {
        Object[] entry = find(owner, name);
        if (entry == null) return null;
        return adapt(entry);
    }

    /**
     * 获取扩展函数（只查找根环境）
     *
     * @param extensionClass 扩展类
     * @param name           函数名（用于错误提示）
     * @param index          派发表索引
     * @param argCount       参数数量
     * @return 函数值
     * @throws FluxonRuntimeError 如果函数不存在
     */
    @NotNull
    public Function getExtensionFunction(Class<?> extensionClass, String name, int index, int argCount) {
        Function function = getExtensionFunctionOrNull(extensionClass, index, argCount);
        if (function != null) {
            return function;
        }
        throw new FunctionNotFoundError(this, extensionClass, name, 0, -1, index);
    }

    /**
     * 获取扩展函数（只查找根环境）
     *
     * @param extensionClass 扩展类
     * @param index          派发表索引
     * @param argCount       参数数量
     * @return 函数值
     */
    @Nullable
    public Function getExtensionFunctionOrNull(Class<?> extensionClass, int index, int argCount) {
        ExtensionDispatchTable dispatchTable = FluxonRuntime.getInstance().getCachedDispatchTables()[index];
        return dispatchTable.resolve(extensionClass, argCount);
    }

    /**
     * 获取根环境中的所有函数重载集合
     */
    @Export
    public Map<String, OverloadSet> getRootFunctions() {
        EnvironmentState m = root.rootState;
        if (m.userFunctions == null || m.userFunctions.isEmpty()) {
            return m.systemFunctions;
        }
        // 合并视图：用户函数覆盖同名系统函数
        Map<String, OverloadSet> merged = new HashMap<>(m.systemFunctions);
        merged.putAll(m.userFunctions);
        return merged;
    }

    /**
     * 获取用户动态定义的函数（不包含系统函数）
     * 用于解析器识别运行时定义的函数
     *
     * @return 用户定义的函数映射，如果没有则返回空 map
     */
    public Map<String, OverloadSet> getUserFunctions() {
        EnvironmentState m = root.rootState;
        if (m.userFunctions == null || m.userFunctions.isEmpty()) {
            return Collections.emptyMap();
        }
        return m.userFunctions;
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
    public Map<String, Map<Class<?>, OverloadSet>> getRootExtensionFunctions() {
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
            this.localVariableCount = count;
            this.localPrimitives = new long[count];
            this.localRefs = new Object[count];
            this.localVariableNames = new String[count];
        }
    }

    /**
     * 设置参数信息（由 ParsedScript.newEnvironment() 调用）
     *
     * @param parameters 参数映射
     */
    public void setParameters(@Nullable Map<String, ParameterInfo> parameters) {
        root.rootState.parameters = parameters;
    }

    /**
     * 获取参数信息（内部方法）
     */
    @NotNull
    private ParameterInfo getParameterInfo(@NotNull String name) {
        Map<String, ParameterInfo> params = root.rootState.parameters;
        if (params == null) {
            throw new IllegalStateException("No parameters defined");
        }
        ParameterInfo info = params.get(name);
        if (info == null) {
            throw new IllegalArgumentException("Unknown parameter: " + name);
        }
        return info;
    }

    /**
     * 设置参数值（通用方法）
     *
     * @param name  参数名
     * @param value 参数值
     */
    public void setParameter(@NotNull String name, @Nullable Object value) {
        ParameterInfo info = getParameterInfo(name);
        int index = info.getIndex();
        Type type = info.getType();
        if (type == Type.DOUBLE) {
            setLocalDouble(index, ((Number) value).doubleValue());
        } else if (type == Type.LONG) {
            setLocalLong(index, ((Number) value).longValue());
        } else {
            setLocalRef(index, value);
        }
    }

    /**
     * 设置 double 类型参数值
     */
    public void setParameter(@NotNull String name, double value) {
        setLocalDouble(getParameterInfo(name).getIndex(), value);
    }

    /**
     * 设置 long 类型参数值
     */
    public void setParameter(@NotNull String name, long value) {
        setLocalLong(getParameterInfo(name).getIndex(), value);
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
        if (index < captureOffset && parent != null) {
            return parent.getLocalRef(index);
        }
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
        if (index < captureOffset && parent != null) {
            parent.setLocalRef(index, value);
            return;
        }
        if (localRefs != null && index < localRefs.length) {
            localRefs[index] = value;
        } else if (parent != null) {
            parent.setLocalRef(index, value);
        }
    }

    /**
     * 设置闭包捕获偏移量
     * 索引 < captureOffset 的局部变量访问将被委托到父环境
     */
    public void setCaptureOffset(int captureOffset) {
        this.captureOffset = captureOffset;
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
     * 获取当前环境中的所有局部变量名（按需分配）
     */
    public String[] getLocalVariableNames() {
        if (localVariableNames == null && localVariableCount > 0) {
            localVariableNames = new String[localVariableCount];
        }
        return localVariableNames;
    }

    /**
     * 获取变量类型（作用域隔离）
     *
     * @param pos 变量位置
     * @return 类型，默认返回 OBJECT
     */
    public Type getVariableType(int pos) {
        if (variableTypes != null && pos >= 0 && pos < variableTypes.length) {
            Type t = variableTypes[pos];
            if (t != null) return t;
        }
        return Type.OBJECT;
    }

    /**
     * 设置变量类型数组
     *
     * @param variableTypes 变量类型数组
     */
    public void setVariableTypes(Type[] variableTypes) {
        this.variableTypes = variableTypes;
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
