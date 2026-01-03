package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.runtime.error.ArgumentTypeMismatchError;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

/**
 * 函数调用上下文
 * 封装函数调用所需的所有信息：目标对象、参数列表和环境
 */
public class FunctionContext<Target> implements AutoCloseable {

    public static final Type TYPE = new Type(FunctionContext.class);

    @NotNull
    private Function function;
    @Nullable
    private Target target;
    @NotNull
    private Environment environment;
    @Nullable
    private FunctionContextPool pool;

    // ====================== 参数数组模式 ======================
    private Object[] arguments;
    private int argumentCount;

    // ====================== inline 参数槽位（fast-args）======================
    // 当 inlineMode 为 true 时，参数存储在 inlineArg0..inlineArg3 中，arguments 为 null
    private boolean inlineMode;
    private int inlineCount;
    private Object inlineArg0;
    private Object inlineArg1;
    private Object inlineArg2;
    private Object inlineArg3;

    private static final Object[] EMPTY_ARGUMENTS = new Object[0];

    public FunctionContext(
            @NotNull Function function,
            @Nullable Target target,
            Object[] arguments,
            @NotNull Environment environment
    ) {
        this(function, target, arguments, environment, null);
    }

    public FunctionContext(
            @NotNull Function function,
            @Nullable Target target,
            Object[] arguments,
            @NotNull Environment environment,
            @Nullable FunctionContextPool pool
    ) {
        this.function = function;
        this.target = target;
        this.arguments = arguments;
        this.argumentCount = arguments.length;
        this.environment = environment;
        this.pool = pool;
    }

    /**
     * 判断指定索引的参数是否为指定类型
     *
     * @param index 参数索引
     * @param type  目标类型
     * @return 如果索引有效且参数类型兼容则返回true，否则返回false
     */
    public boolean isType(int index, @NotNull Class<?> type) {
        Object argument = getArgument(index);
        return argument != null && Intrinsics.isCompatibleType(type, argument);
    }

    /**
     * 判断是否存在指定索引的参数
     *
     * @param index 参数索引
     * @return 如果索引有效则返回true，否则返回false
     */
    public boolean hasArgument(int index) {
        return index < getArgumentCount();
    }

    /**
     * 获取指定索引的参数值
     *
     * @param index 参数索引
     * @return 如果索引有效则返回参数值，否则返回null
     */
    @Nullable
    public Object getArgument(int index) {
        if (inlineMode) {
            if (index >= inlineCount) {
                return null;
            }
            // @formatter:off
            switch (index) {
                case 0: return inlineArg0;
                case 1: return inlineArg1;
                case 2: return inlineArg2;
                case 3: return inlineArg3;
                default: return null;
            }
            // @formatter:on
        }
        if (index >= argumentCount) {
            return null;
        }
        return arguments[index];
    }

    /**
     * 获取指定索引的参数值，并尝试将其转换为指定类型
     *
     * @param index 参数索引
     * @param type  目标类型
     * @param <T>   目标类型参数
     * @return 如果索引有效且参数类型兼容则返回转换后的参数值，否则抛出异常
     */
    @Nullable
    public <T> T getArgumentByType(int index, @NotNull Class<T> type) {
        Object argument = getArgument(index);
        if (argument == null) return null;
        // 修复：传入对象本身，而不是 Class 对象
        if (Intrinsics.isCompatibleType(type, argument)) {
            return type.cast(argument);
        }
        throw new ArgumentTypeMismatchError(this, index, type, argument);
    }

    /**
     * 获取指定索引的参数值，并尝试将其转换为 Number 类型
     *
     * @param index 参数索引
     * @return 如果索引有效且参数类型兼容则返回转换后的 Number 值，否则抛出异常
     */
    @NotNull
    public Number getNumber(int index) {
        Object argument = getArgument(index);
        if (argument instanceof Number) {
            return ((Number) argument);
        }
        throw new ArgumentTypeMismatchError(this, index, Number.class, argument);
    }

    /**
     * 获取指定索引的参数值，并尝试将其转换为 Number 类型，若失败则返回 null
     *
     * @param index 参数索引
     * @return 如果索引有效且参数类型兼容则返回转换后的 Number 值，否则返回 null
     */
    @Nullable
    public Number getNumberOrNull(int index) {
        return getArgumentByType(index, Number.class);
    }

    /**
     * 获取指定索引的参数值，并尝试将其转换为 Boolean 类型，若失败则返回 false
     *
     * @param index 参数索引
     * @return 如果索引有效且参数类型兼容则返回转换后的 Boolean 值，否则返回 false
     */
    public boolean getBoolean(int index) {
        Object argument = getArgument(index);
        return Coerce.asBoolean(argument).orElse(false);
    }

    /**
     * 获取指定索引的参数值，并尝试将其转换为 String 类型，若失败则返回 null
     *
     * @param index 参数索引
     * @return 如果索引有效且参数类型兼容则返回转换后的 String 值，否则返回 null
     */
    @Nullable
    public String getString(int index) {
        Object argument = getArgument(index);
        if (argument == null) return null;
        return argument.toString();
    }

    /**
     * 获取指定索引的参数值，并尝试将其转换为 Function 类型
     *
     * @param index 参数索引
     * @return 如果索引有效且参数类型兼容则返回转换后的 Function 值，否则抛出异常
     */
    @NotNull
    public Function getFunction(int index) {
        Object argument = getArgument(index);
        if (argument instanceof Function) {
            return (Function) argument;
        }
        throw new ArgumentTypeMismatchError(this, index, Function.class, argument);
    }

    /**
     * 获取指定索引的参数值，并尝试将其转换为 Function 类型，若失败则返回 null
     *
     * @param index 参数索引
     * @return 如果索引有效且参数类型兼容则返回转换后的 Function 名称，否则返回 null
     */
    @Nullable
    public String getFunctionOrNull(int index) {
        Object argument = getArgument(index);
        if (argument instanceof Function) {
            return ((Function) argument).getName();
        }
        return null;
    }

    /**
     * 获取当前函数上下文关联的函数实例
     *
     * @return 函数实例
     */
    @NotNull
    public Function getFunction() {
        return function;
    }

    /**
     * 获取当前函数上下文关联的目标实例
     *
     * @return 目标实例
     */
    @Nullable
    public Target getTarget() {
        return target;
    }

    /**
     * 获取当前函数上下文关联的参数数组
     * 在 inline 模式下，会惰性创建数组并缓存
     *
     * @return 参数数组
     */
    public Object[] getArguments() {
        if (inlineMode) {
            // 惰性物化：创建数组并缓存
            Object[] materialized = Intrinsics.materializeArgs(inlineCount, inlineArg0, inlineArg1, inlineArg2, inlineArg3);
            // 切换到数组模式，避免重复物化
            this.arguments = materialized;
            this.argumentCount = inlineCount;
            this.inlineMode = false;
            return materialized;
        }
        return arguments;
    }

    /**
     * 获取当前函数上下文关联的参数数量
     *
     * @return 参数数量
     */
    public int getArgumentCount() {
        return inlineMode ? inlineCount : argumentCount;
    }

    /**
     * 获取当前函数上下文关联的环境实例
     *
     * @return 环境实例
     */
    @NotNull
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * 获取当前函数上下文所属的池实例（如果有的话）
     */
    @Nullable
    public FunctionContextPool getPool() {
        return pool;
    }

    /**
     * 更新当前函数上下文关联的参数数组和参数数量
     * 用于在高频调用时避免重复创建函数上下文实例
     * 注意：此方法会清除 inline 模式
     */
    public FunctionContext<Target> updateArguments(Object[] arguments) {
        this.arguments = arguments;
        this.argumentCount = arguments.length;
        this.inlineMode = false;
        clearInlineSlots();
        return this;
    }

    /**
     * 更新当前函数上下文关联的 inline 参数槽位
     * 用于在高频调用时避免重复创建函数上下文实例和参数数组
     * 注意：此方法会启用 inline 模式
     */
    public FunctionContext<Target> updateArguments(
            int count,
            @Nullable Object arg0,
            @Nullable Object arg1,
            @Nullable Object arg2,
            @Nullable Object arg3
    ) {
        this.arguments = null;
        this.argumentCount = 0;
        this.inlineMode = true;
        this.inlineCount = count;
        this.inlineArg0 = arg0;
        this.inlineArg1 = arg1;
        this.inlineArg2 = arg2;
        this.inlineArg3 = arg3;
        return this;
    }

    /**
     * 仅供内部池化使用的重置方法，用于避免重复创建 FunctionContext 实例
     */
    @SuppressWarnings("unchecked")
    void reset(
            @NotNull Function function,
            @Nullable Object target,
            @NotNull Object[] arguments,
            @NotNull Environment environment) {
        this.function = function;
        this.target = (Target) target;
        this.arguments = arguments;
        this.argumentCount = arguments.length;
        this.environment = environment;
        this.inlineMode = false;
        clearInlineSlots();
    }

    /**
     * 仅供内部池化使用的 inline 模式重置方法
     */
    @SuppressWarnings("unchecked")
    void resetInline(
            @NotNull Function function,
            @Nullable Object target,
            int count,
            @Nullable Object arg0,
            @Nullable Object arg1,
            @Nullable Object arg2,
            @Nullable Object arg3,
            @NotNull Environment environment) {
        this.function = function;
        this.target = (Target) target;
        this.arguments = null;
        this.argumentCount = 0;
        this.environment = environment;
        this.inlineMode = true;
        this.inlineCount = count;
        this.inlineArg0 = arg0;
        this.inlineArg1 = arg1;
        this.inlineArg2 = arg2;
        this.inlineArg3 = arg3;
    }

    /**
     * 池化归还时清理对参数、目标和环境的引用，便于 GC 及时回收
     */
    @SuppressWarnings("DataFlowIssue")
    void clearForPooling() {
        this.arguments = EMPTY_ARGUMENTS;
        this.argumentCount = 0;
        this.target = null;
        this.environment = null;
        this.inlineMode = false;
        clearInlineSlots();
    }

    /**
     * 清理 inline 槽位引用，便于 GC
     */
    private void clearInlineSlots() {
        this.inlineCount = 0;
        this.inlineArg0 = null;
        this.inlineArg1 = null;
        this.inlineArg2 = null;
        this.inlineArg3 = null;
    }

    @Override
    public String toString() {
        return "FunctionContext{" +
                "function=" + function.getName() +
                ", target=" + target +
                ", arguments=" + getArgumentCount() +
                ", inline=" + inlineMode +
                '}';
    }

    @Override
    public void close() {
        if (pool != null) {
            pool.release(this);
        }
    }
}
