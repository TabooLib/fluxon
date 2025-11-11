package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.runtime.error.ArgumentTypeMismatchError;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.Arrays;

/**
 * 函数调用上下文
 * 封装函数调用所需的所有信息：目标对象、参数列表和环境
 */
public class FunctionContext<Target> {

    public static final Type TYPE = new Type(FunctionContext.class);

    @NotNull
    private final Function function;
    @Nullable
    private final Target target;
    @NotNull
    private final Environment environment;

    private Object[] arguments;
    private int argumentCount;

    public FunctionContext(@NotNull Function function, @Nullable Target target, Object[] arguments, @NotNull Environment environment) {
        this.function = function;
        this.target = target;
        this.arguments = arguments;
        this.argumentCount = arguments.length;
        this.environment = environment;
    }

    /**
     * 判断是否存在指定索引的参数
     *
     * @param index 参数索引
     * @return 如果索引有效则返回true，否则返回false
     */
    public boolean hasArgument(int index) {
        return index < argumentCount;
    }

    /**
     * 获取指定索引的参数值
     *
     * @param index 参数索引
     * @return 如果索引有效则返回参数值，否则返回null
     */
    @Nullable
    public Object getArgument(int index) {
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
     *
     * @return 参数数组
     */
    public Object[] getArguments() {
        return arguments;
    }

    /**
     * 获取当前函数上下文关联的参数数量
     *
     * @return 参数数量
     */
    public int getArgumentCount() {
        return argumentCount;
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
     * 更新当前函数上下文关联的参数数组和参数数量
     * 用于在高频调用时避免重复创建函数上下文实例
     *
     * @param arguments 参数数组
     * @return 当前函数上下文实例
     */
    public FunctionContext<Target> updateArguments(Object[] arguments) {
        this.arguments = arguments;
        this.argumentCount = arguments.length;
        return this;
    }

    /**
     * 复制当前函数上下文实例
     *
     * @return 新的函数上下文实例
     */
    @NotNull
    public FunctionContext<?> copy(Object[] arguments) {
        return new FunctionContext<>(function, target, arguments, environment);
    }

    @Override
    public String toString() {
        return "FunctionContext{" +
                "function=" + function +
                ", target=" + target +
                ", arguments=" + Arrays.toString(arguments) +
                ", environment=" + environment +
                '}';
    }
}
