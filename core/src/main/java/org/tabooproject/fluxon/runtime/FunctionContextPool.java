package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 线程本地的 FunctionContext 简单池，避免在高频调用路径上重复分配。
 * 支持跨线程归还：async 任务完成后可将 context 归还到原借出线程的池。
 */
public final class FunctionContextPool {

    public static final Type TYPE = new Type(FunctionContextPool.class);

    private static final int MAX_POOL_SIZE = 32;
    private static final ThreadLocal<FunctionContextPool> LOCAL = ThreadLocal.withInitial(FunctionContextPool::new);
    private static final Object[] EMPTY_REFS = new Object[0];

    private final FunctionContext<?>[] pool = new FunctionContext<?>[MAX_POOL_SIZE];
    private final ConcurrentLinkedQueue<FunctionContext<?>> pendingReturns = new ConcurrentLinkedQueue<>();
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
        FunctionContext<?> context = pollOrCreate(function, target, refs, environment);
        context.reset(function, target, refs, environment);
        return context;
    }

    /**
     * 从线程本地池借用一个 FunctionContext 实例（按参数数量，不分配 Object[] 参数数组）
     */
    public FunctionContext<?> borrow(@NotNull Function function, @Nullable Object target, int argCount, @NotNull Environment environment) {
        Object[] refs = argCount > 0 ? new Object[argCount] : EMPTY_REFS;
        FunctionContext<?> context = pollOrCreate(function, target, refs, environment);
        context.reset(function, target, argCount, environment);
        return context;
    }

    /**
     * 从池中获取或创建新的 context
     */
    private FunctionContext<?> pollOrCreate(@NotNull Function function, @Nullable Object target, @NotNull Object[] refs, @NotNull Environment environment) {
        // 优先从本地池获取（无锁）
        if (size > 0) {
            FunctionContext<?> context = pool[--size];
            pool[size] = null;
            return context;
        }
        // 本地池为空，尝试从跨线程归还队列回收
        FunctionContext<?> context = pendingReturns.poll();
        if (context != null) {
            return context;
        }
        // 都没有，新建
        return new FunctionContext<>(function, target, refs, environment, this);
    }

    /**
     * 从线程本地池借用一个 FunctionContext 实例（复制已有 context 的函数/目标/环境）
     */
    public FunctionContext<?> borrowCopy(@NotNull FunctionContext<?> context, @Nullable Object[] refs) {
        return borrow(context.getFunction(), context.getTarget(), refs != null ? refs : EMPTY_REFS, context.getEnvironment());
    }

    /**
     * 归还一个 FunctionContext 实例到线程本地池（仅限同线程调用）
     */
    public void release(FunctionContext<?> context) {
        if (context == null) {
            return;
        }
        // 如果满了直接丢弃对象
        // 不需要做任何清理（GC 会回收它）
        if (size >= MAX_POOL_SIZE) {
            return;
        }
        // 只有确定要入池，才进行清理
        context.clearForPooling();
        pool[size++] = context;
    }

    /**
     * 从其他线程归还 context 到此池（线程安全）
     * async 任务完成后调用此方法将 context 归还到原借出线程
     */
    public void returnFromOtherThread(FunctionContext<?> context) {
        if (context == null) {
            return;
        }
        context.clearForPooling();
        pendingReturns.offer(context);
    }
}
