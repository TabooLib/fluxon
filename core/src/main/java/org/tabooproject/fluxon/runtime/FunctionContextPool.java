package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 线程本地的 FunctionContext 简单池，避免在高频调用路径上重复分配。
 * 仅在单线程借用/归还的情况下使用，不涉及跨线程共享。
 */
public final class FunctionContextPool {

    private static final int MAX_POOL_SIZE = 32;
    private static final ThreadLocal<FunctionContextPool> LOCAL = ThreadLocal.withInitial(FunctionContextPool::new);

    private final FunctionContext<?>[] pool = new FunctionContext<?>[MAX_POOL_SIZE];
    private int size;

    private FunctionContextPool() {
    }

    /**
     * 从线程本地池借用一个 FunctionContext 实例
     */
    public static Lease borrow(@NotNull Function function, @Nullable Object target, Object[] arguments, @NotNull Environment environment) {
        return LOCAL.get().doBorrow(function, target, arguments, environment);
    }

    /**
     * 仅供内部池化使用的重置方法，用于避免重复创建 FunctionContext 实例
     */
    private Lease doBorrow(@NotNull Function function, @Nullable Object target, Object[] arguments, @NotNull Environment environment) {
        FunctionContext<?> context;
        if (size > 0) {
            context = pool[--size];
            pool[size] = null;
            context.reset(function, target, arguments, environment);
        } else {
            context = new FunctionContext<>(function, target, arguments, environment);
        }
        return new Lease(this, context);
    }

    /**
     * 归还一个 FunctionContext 实例到线程本地池
     */
    private void release(FunctionContext<?> context) {
        context.clearArguments();
        if (size >= MAX_POOL_SIZE) {
            return;
        }
        pool[size++] = context;
    }

    /**
     * 线程本地池的借用/归还句柄，用于自动管理资源释放
     */
    public static final class Lease implements AutoCloseable {

        private FunctionContextPool owner;
        private FunctionContext<?> context;
        private final Thread ownerThread;

        private Lease(FunctionContextPool owner, FunctionContext<?> context) {
            this.owner = owner;
            this.context = context;
            this.ownerThread = Thread.currentThread();
        }

        public FunctionContext<?> get() {
            return context;
        }

        @Override
        public void close() {
            if (context == null || owner == null) {
                return;
            }
            if (ownerThread != Thread.currentThread()) {
                // 避免跨线程归还引起竞态，直接丢弃由 GC 回收
                context = null;
                owner = null;
                return;
            }
            owner.release(context);
            context = null;
            owner = null;
        }
    }
}
