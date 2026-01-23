package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 线程本地的 FunctionContext 简单池，避免在高频调用路径上重复分配。
 * 仅在单线程借用/归还的情况下使用，不涉及跨线程共享。
 * 由于使用 ThreadLocal 存储，每个线程自动获得独立的池实例，无需额外的线程检查。
 */
public final class FunctionContextPool {

    public static final Type TYPE = new Type(FunctionContextPool.class);

    private static final int MAX_POOL_SIZE = 32;
    private static final ThreadLocal<FunctionContextPool> LOCAL = ThreadLocal.withInitial(FunctionContextPool::new);
    private static final Object[] EMPTY_REFS = new Object[0];

    private final FunctionContext<?>[] pool = new FunctionContext<?>[MAX_POOL_SIZE];
    private int size;

    private FunctionContextPool() {
    }

    /**
     * 获取当前线程的池实例
     */
    @NotNull
    public static FunctionContextPool local() {
        return LOCAL.get();
    }

    /**
     * 从线程本地池借用一个 FunctionContext 实例
     */
    public FunctionContext<?> borrow(@NotNull Function function, @Nullable Object target, @NotNull Object[] refs, @NotNull Environment environment) {
        FunctionContext<?> context;
        if (size > 0) {
            context = pool[--size];
            pool[size] = null;
            context.reset(function, target, refs, environment);
        } else {
            context = new FunctionContext<>(function, target, refs, environment, this);
        }
        return context;
    }

    /**
     * 从线程本地池借用一个 FunctionContext 实例（复制已有 context 的函数/目标/环境）
     */
    public FunctionContext<?> borrowCopy(@NotNull FunctionContext<?> context, @Nullable Object[] refs) {
        return borrow(context.getFunction(), context.getTarget(), refs != null ? refs : EMPTY_REFS, context.getEnvironment());
    }

    /**
     * 归还一个 FunctionContext 实例到线程本地池
     */
    public void release(FunctionContext<?> context) {
        if (context == null) {
            return;
        }
        context.clearForPooling();
        if (size >= MAX_POOL_SIZE) {
            return;
        }
        pool[size++] = context;
    }
}
