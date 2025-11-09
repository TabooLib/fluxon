package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.definition.Annotation;

import java.util.List;

/**
 * 函数接口
 * 表示可以被调用的函数
 */
public interface Function {

    // 类型常量
    Type TYPE = new Type(Function.class);

    /**
     * 获取函数的命名空间（包）
     * 若为空则表示默认可用
     */
    @Nullable
    String getNamespace();

    /**
     * 获取函数名称
     */
    @NotNull
    String getName();

    /**
     * 获取函数参数数量
     */
    @NotNull
    List<Integer> getParameterCounts();

    /**
     * 获取函数参数最大数量
     */
    int getMaxParameterCount();

    /**
     * 判断是否为异步函数
     *
     * @return 如果是异步函数返回 true，否则返回 false
     */
    boolean isAsync();

    /**
     * 判断是否为主线程同步函数
     *
     * @return 如果是主线程同步函数返回 true，否则返回 false
     */
    boolean isPrimarySync();

    /**
     * 获取函数的注解列表
     *
     * @return 注解列表
     */
    List<Annotation> getAnnotations();

    /**
     * 执行函数
     *
     * @param context 函数上下文
     * @return 返回值
     */
    @Nullable
    Object call(@NotNull final FunctionContext<?> context);
}