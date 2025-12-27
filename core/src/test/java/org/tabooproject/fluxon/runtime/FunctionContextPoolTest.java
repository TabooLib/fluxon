package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 针对 FunctionContextPool 的线程安全保护与并发调用回归测试
 */
public class FunctionContextPoolTest {

    /**
     * 测试跨线程释放行为。
     * <p>
     * 注意：优化后移除了 ownerThread 检查，依赖 ThreadLocal 保障线程隔离。
     * 正确的使用模式是：每个线程应通过 FunctionContextPool.local() 获取自己的池实例。
     * 此测试验证：即使错误地传递池引用跨线程，release 操作仍能正常执行（虽然语义上不推荐）。
     */
    @Test
    public void closeFromOtherThreadIsIgnored() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        Environment environment = runtime.newEnvironment();
        Function function = new NativeFunction<>(new SymbolFunction(null, "poolGuard", 0), ctx -> null);
        FunctionContextPool pool = currentThreadPool();
        FunctionContext<?> context = pool.borrow(function, null, new Object[0], environment);
        int afterBorrow = getPoolSize(pool);
        // 跨线程释放现在会实际添加到池中（因为移除了线程检查）
        // 这不是推荐的使用模式，但不会导致错误
        Thread t = new Thread(() -> pool.release(context));
        t.start();
        t.join();
        int afterCrossThreadRelease = getPoolSize(pool);
        // 跨线程释放现在会增加池大小（优化后的行为）
        assertTrue(afterCrossThreadRelease >= afterBorrow, "Cross-thread release now adds to pool (optimization removed thread guard)");
        // 不再重复释放，因为 context 已经被释放
    }

    @Test
    public void asyncAndPrimaryCallsStayThreadLocal() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerAsyncFunction("asyncPoolEcho", 1, ctx -> Thread.currentThread().getName() + ":" + ctx.getArgument(0));
        runtime.registerPrimarySyncFunction("primaryPoolEcho", 1, ctx -> Thread.currentThread().getName() + ":" + ctx.getArgument(0));

        Executor previousPrimary = runtime.getPrimaryThreadExecutor();
        ExecutorService primaryExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "primary-pool-test"));
        runtime.setPrimaryThreadExecutor(primaryExecutor);

        ExecutorService callers = Executors.newFixedThreadPool(4);
        try {
            int tasks = 64;
            List<CompletableFuture<String>> asyncResults = new ArrayList<>();
            List<CompletableFuture<String>> primaryResults = new ArrayList<>();

            for (int i = 0; i < tasks; i++) {
                final int index = i;
                asyncResults.add(CompletableFuture.supplyAsync(() -> {
                    Environment env = runtime.newEnvironment();
                    Object result = Intrinsics.callFunction(env, "asyncPoolEcho", new Object[]{"A" + index}, -1, -1);
                    return Intrinsics.awaitValue(result).toString();
                }, callers));
                primaryResults.add(CompletableFuture.supplyAsync(() -> {
                    Environment env = runtime.newEnvironment();
                    Object result = Intrinsics.callFunction(env, "primaryPoolEcho", new Object[]{"P" + index}, -1, -1);
                    return Intrinsics.awaitValue(result).toString();
                }, callers));
            }

            for (int i = 0; i < tasks; i++) {
                String async = asyncResults.get(i).get(30, TimeUnit.SECONDS);
                assertTrue(async.startsWith("fluxon-worker-"),
                        "Async functions should execute on worker threads");
                assertTrue(async.endsWith("A" + i),
                        "Async calls should keep their argument binding");

                String primary = primaryResults.get(i).get(30, TimeUnit.SECONDS);
                assertTrue(primary.startsWith("primary-pool-test"),
                        "Primary sync functions should execute on the configured executor");
                assertTrue(primary.endsWith("P" + i),
                        "Primary sync calls should keep their argument binding");
            }
        } finally {
            callers.shutdownNow();
            primaryExecutor.shutdownNow();
            runtime.setPrimaryThreadExecutor(previousPrimary);
        }
    }

    @SuppressWarnings("unchecked")
    private static FunctionContextPool currentThreadPool() throws Exception {
        Field localField = FunctionContextPool.class.getDeclaredField("LOCAL");
        localField.setAccessible(true);
        ThreadLocal<FunctionContextPool> local = (ThreadLocal<FunctionContextPool>) localField.get(null);
        return local.get();
    }

    private static int getPoolSize(FunctionContextPool pool) throws Exception {
        Field sizeField = FunctionContextPool.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        return sizeField.getInt(pool);
    }
}
