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

    private static final int MAX_POOL_SIZE = 64;
    private static final ThreadLocal<FunctionContextPool> LOCAL = ThreadLocal.withInitial(FunctionContextPool::new);
    private static final Object[] EMPTY_REFS = new Object[0];

    private final FunctionContext<?>[] pool = new FunctionContext<?>[MAX_POOL_SIZE];
    private final ConcurrentLinkedQueue<FunctionContext<?>> pendingReturns = new ConcurrentLinkedQueue<>();
    private final Thread ownerThread;
    private int size;

    private FunctionContextPool() {
        this.ownerThread = Thread.currentThread();
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
        FunctionContext<?> context = pollOrCreate();
        if (context.dirty) {
            context.clearRefs();
            context.dirty = false;
        }
        context.reset(function, target, refs, environment);
        return context;
    }

    /**
     * 从线程本地池借用一个 FunctionContext 实例（按参数数量，复用内部数组）
     */
    public FunctionContext<?> borrow(@NotNull Function function, @Nullable Object target, int argCount, @NotNull Environment environment) {
        FunctionContext<?> context = pollOrCreate();
        if (context.dirty) {
            context.clearRefs();
            context.dirty = false;
        }
        context.reset(function, target, argCount, environment);
        return context;
    }

    /**
     * 从池中获取或创建新的 context
     */
    private FunctionContext<?> pollOrCreate() {
        if (size > 0) {
            FunctionContext<?> context = pool[--size];
            pool[size] = null; // 帮助 GC，防止悬挂引用
            return context;
        }
        FunctionContext<?> context = pendingReturns.poll();
        if (context != null) {
            return context;
        }
        return new FunctionContext<>(this);
    }

    /**
     * 从线程本地池借用一个 FunctionContext 实例（复制已有 context 的函数/目标/环境）
     */
    public FunctionContext<?> borrowCopy(@NotNull FunctionContext<?> context, @Nullable Object[] refs) {
        return borrow(context.getFunction(), context.getTarget(), refs != null ? refs : EMPTY_REFS, context.getEnvironment());
    }

    /**
     * 快速归还，由 FunctionContext.close() 调用
     * 自动检测线程归属，跨线程归还走安全通道
     */
    void releaseUnchecked(FunctionContext<?> context) {
        if (Thread.currentThread() == ownerThread) {
            if (size < MAX_POOL_SIZE) {
                pool[size++] = context;
            }
        } else {
            returnFromOtherThread(context);
        }
    }

    /**
     * 从其他线程归还 context 到此池（线程安全）
     * async 任务完成后调用此方法将 context 归还到原借出线程
     */
    public void returnFromOtherThread(FunctionContext<?> context) {
        if (context == null) {
            return;
        }
        context.dirty = true;
        pendingReturns.offer(context);
    }
}
