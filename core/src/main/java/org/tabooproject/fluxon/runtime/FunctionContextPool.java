package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.util.ThreadLocalObjectPool;

/**
 * 线程本地的 FunctionContext 简单池，避免在高频调用路径上重复分配。
 * 仅在单线程借用/归还的情况下使用，不涉及跨线程共享。
 */
public final class FunctionContextPool extends ThreadLocalObjectPool<FunctionContext<?>, FunctionContextPool.Lease> {

    private static final int MAX_POOL_SIZE = 32;
    private static final ThreadLocal<FunctionContextPool> LOCAL = ThreadLocal.withInitial(FunctionContextPool::new);

    private Function function;
    private Object target;
    private Object[] arguments;
    private Environment environment;

    private FunctionContextPool() {
        super(MAX_POOL_SIZE);
    }

    /**
     * 从线程本地池借用一个 FunctionContext 实例
     */
    public static Lease borrow(@NotNull Function function, @Nullable Object target, Object[] arguments, @NotNull Environment environment) {
        FunctionContextPool pool = LOCAL.get();
        pool.function = function;
        pool.target = target;
        pool.arguments = arguments;
        pool.environment = environment;
        return pool.borrowInternal();
    }

    @Override
    protected FunctionContext<?> create() {
        return new FunctionContext<>(function, target, arguments, environment);
    }

    @Override
    protected void onBorrow(FunctionContext<?> value) {
        value.reset(function, target, arguments, environment);
    }

    @Override
    protected void onRelease(FunctionContext<?> value) {
        value.clearForPooling();
    }

    @Override
    protected Lease newLease() {
        return new Lease();
    }

    /**
     * 线程本地池的借用/归还句柄，用于自动管理资源释放
     */
    public static final class Lease extends ThreadLocalObjectPool.Lease<FunctionContext<?>> {
    }
}
