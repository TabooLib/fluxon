package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;
import org.tabooproject.fluxon.util.ThreadLocalObjectPool;

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

    @Test
    public void closeFromOtherThreadIsIgnored() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        Environment environment = runtime.newEnvironment();
        Function function = new NativeFunction<>(new SymbolFunction(null, "poolGuard", 0), ctx -> null);

        FunctionContextPool pool = currentThreadPool();
        resetPoolCounters(pool);
        int beforeBorrow = getPoolSize(pool);

        FunctionContextPool.Lease lease = FunctionContextPool.borrow(function, null, new Object[0], environment);
        int afterBorrow = getPoolSize(pool);

        Thread t = new Thread(lease::close);
        t.start();
        t.join();

        assertEquals(afterBorrow, getPoolSize(pool),
                "Cross-thread close should not mutate the borrower thread's pool (size before borrow=" + beforeBorrow + ")");
    }

    @Test
    public void leaseAndContextAreReusedAndReset() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        Environment environment = runtime.newEnvironment();
        Function fn1 = new NativeFunction<>(new SymbolFunction(null, "fn1", 0), ctx -> ctx.getFunction().getName());
        Function fn2 = new NativeFunction<>(new SymbolFunction(null, "fn2", 0), ctx -> ctx.getFunction().getName());

        FunctionContextPool pool = currentThreadPool();
        resetPoolCounters(pool);

        FunctionContextPool.Lease lease1 = FunctionContextPool.borrow(fn1, "t1", new Object[]{"a"}, environment);
        FunctionContext<?> ctx1 = lease1.get();
        assertSame(fn1, ctx1.getFunction());
        assertArrayEquals(new Object[]{"a"}, ctx1.getArguments());
        lease1.close();

        FunctionContextPool.Lease lease2 = FunctionContextPool.borrow(fn2, "t2", new Object[]{"b"}, environment);
        FunctionContext<?> ctx2 = lease2.get();
        assertSame(lease1, lease2, "Lease should be recycled");
        assertSame(fn2, ctx2.getFunction(), "Context should be reset with new function");
        assertArrayEquals(new Object[]{"b"}, ctx2.getArguments(), "Context should be reset with new arguments");
        lease2.close();
    }

    @Test
    public void detachSkipsReturnToPool() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        Environment environment = runtime.newEnvironment();
        Function function = new NativeFunction<>(new SymbolFunction(null, "poolDetach", 0), ctx -> null);

        FunctionContextPool pool = currentThreadPool();
        resetPoolCounters(pool);
        int before = getPoolSize(pool);

        FunctionContextPool.Lease lease = FunctionContextPool.borrow(function, null, new Object[]{"x"}, environment);
        FunctionContext<?> ctx = lease.detach();
        assertNotNull(ctx);
        lease.close();

        assertEquals(before, getPoolSize(pool), "Detached context should not be returned to pool");
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
        Field sizeField = ThreadLocalObjectPool.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        return sizeField.getInt(pool);
    }

    private static void resetPoolCounters(FunctionContextPool pool) throws Exception {
        Field sizeField = ThreadLocalObjectPool.class.getDeclaredField("size");
        Field leaseSizeField = ThreadLocalObjectPool.class.getDeclaredField("leaseSize");
        sizeField.setAccessible(true);
        leaseSizeField.setAccessible(true);
        sizeField.setInt(pool, 0);
        leaseSizeField.setInt(pool, 0);
    }
}
