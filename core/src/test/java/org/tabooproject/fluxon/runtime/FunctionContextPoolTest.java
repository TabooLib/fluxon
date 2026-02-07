package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

/**
 * 针对 FunctionContextPool 的线程安全保护与并发调用回归测试
 */
public class FunctionContextPoolTest {

    /**
     * 测试跨线程释放行为。
     * <p>
     * 栈式分配器依赖 LIFO 顺序和 stackIndex 校验，
     * 跨线程 close() 会通过 stackIndex 匹配成功释放。
     * 此测试验证跨线程 close 不会破坏池状态。
     */
    @Test
    public void closeFromOtherThreadIsIgnored() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        Environment environment = runtime.newEnvironment();
        Function function = new NativeFunction<>("poolGuard", returns(Type.VOID).noParams(), ctx -> {});
        FunctionContextPool pool = FunctionContextPool.local();
        // 先借后还，确保栈内有空间
        FunctionContext<?> warmup = pool.borrow(function, null, new Object[0], environment);
        warmup.close();
        int baseline = pool.getDepth();
        FunctionContext<?> context = pool.borrow(function, null, new Object[0], environment);
        assertEquals(baseline + 1, pool.getDepth(), "Borrow should increment depth");
        // 跨线程 close
        Thread t = new Thread(context::close);
        t.start();
        t.join();
        // 栈式池中跨线程 close 通过 stackIndex 匹配释放
        assertEquals(baseline, pool.getDepth(), "Cross-thread close should release via stackIndex match");
    }

    @Test
    public void asyncAndPrimaryCallsStayThreadLocal() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerAsyncFunction("asyncPoolEcho", returns(Type.OBJECT).params(Type.OBJECT), ctx -> ctx.setReturnRef(Thread.currentThread().getName() + ":" + ctx.getRef(0)));
        runtime.registerPrimarySyncFunction("primaryPoolEcho", returns(Type.OBJECT).params(Type.OBJECT), ctx -> ctx.setReturnRef(Thread.currentThread().getName() + ":" + ctx.getRef(0)));

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
                    Function function = env.getFunction("asyncPoolEcho");
                    FunctionContextPool pool = FunctionContextPool.local();
                    FunctionContext<?> ctx = pool.borrow(function, null, new Object[]{"A" + index}, env);
                    Object result = Intrinsics.finishCall(ctx);
                    return Intrinsics.awaitValue(result).toString();
                }, callers));
                primaryResults.add(CompletableFuture.supplyAsync(() -> {
                    Environment env = runtime.newEnvironment();
                    Function function = env.getFunction("primaryPoolEcho");
                    FunctionContextPool pool = FunctionContextPool.local();
                    FunctionContext<?> ctx = pool.borrow(function, null, new Object[]{"P" + index}, env);
                    Object result = Intrinsics.finishCall(ctx);
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
}
