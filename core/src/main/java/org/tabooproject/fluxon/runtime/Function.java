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
     * 获取函数签名
     */
    @Nullable
    FunctionSignature getSignature();

    /**
     * 获取函数参数数量
     */
    default int getParameterCount() {
        FunctionSignature sig = getSignature();
        return sig != null ? sig.getParameterCount() : 0;
    }

    /**
     * 获取函数返回类型
     */
    @NotNull
    default Type getReturnType() {
        FunctionSignature sig = getSignature();
        return sig != null ? sig.getReturnType() : Type.OBJECT;
    }

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
     * 获取直接绑定信息
     * 若非空，编译器可直接生成 INVOKESTATIC 调用目标方法，跳过函数调用框架
     */
    @Nullable
    default DirectBinding getDirectBinding() {
        return null;
    }

    /**
     * 执行函数
     *
     * @param context 函数上下文
     */
    void call(@NotNull final FunctionContext<?> context);
}
