package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.runtime.concurrent.ThreadPoolManager;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.FluxonTestUtil.assertBothEqual;
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
                if (!ThreadPoolManager.getInstance().isVirtualThreadExecutor()) {
                    assertTrue(async.startsWith("fluxon-worker-"),
                            "Async functions should execute on worker threads");
                }
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

    /**
     * 验证 async 函数的 reassignPool：
     * detach 后 ctx.getPool() 应指向 worker 线程的 pool，而非调用方线程的 pool
     */
    @Test
    public void asyncReassignPoolToWorkerThread() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        // 注册 async 函数，内部做嵌套调用验证 pool 归属
        runtime.registerAsyncFunction("asyncPoolCheck", returns(Type.OBJECT).params(Type.OBJECT), ctx -> {
            FunctionContextPool poolInsideAsync = ctx.getPool();
            FunctionContextPool workerLocalPool = FunctionContextPool.local();
            // reassignPool 后两者应相同
            ctx.setReturnRef(poolInsideAsync == workerLocalPool);
        });
        Environment env = runtime.newEnvironment();
        Function function = env.getFunction("asyncPoolCheck");
        FunctionContextPool callerPool = FunctionContextPool.local();
        FunctionContext<?> ctx = callerPool.borrow(function, null, new Object[]{"test"}, env);
        Object result = Intrinsics.finishCall(ctx);
        Boolean poolMatch = (Boolean) Intrinsics.awaitValue(result);
        assertTrue(poolMatch, "After reassignPool, ctx.getPool() should return worker thread's local pool");
    }

    /**
     * 脚本顶层执行结束后清理空闲池槽，避免线程复用时保留上一轮 Environment.rootVariables
     */
    @Test
    public void scriptEvalClearsIdleContextReferences() throws Exception {
        Object payload = new Object();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def hold(x) = &x\n" +
                        "hold(&payload)",
                ctx -> {},
                env -> env.defineRootVariable("payload", payload)
        );
        assertBothEqual(payload, result);

        FunctionContextPool pool = FunctionContextPool.local();
        FunctionContext<?> firstContext = firstStackContext(pool);
        assertNull(field(firstContext, "function"), "Idle context should not retain function after script eval");
        assertNull(field(firstContext, "environment"), "Idle context should not retain Environment after script eval");
        assertFalse(refsContain(firstContext, payload), "Idle context should not retain script argument values");
    }

    /**
     * 并发压力测试：async 函数内嵌套调用不应出现参数交叉
     * 模拟 Frontier 场景：多线程同时触发 async 函数，每个 async 函数内做嵌套调用
     */
    @RepeatedTest(20)
    public void asyncNestedCallNoArgCrossContamination() throws Exception {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def identity(x) = &x\n" +
                "async def compute(id) = {\n" +
                "  r = identity(&id)\n" +
                "  return &r\n" +
                "}\n" +
                "f1 = compute(100)\n" +
                "f2 = compute(200)\n" +
                "f3 = compute(300)\n" +
                "f4 = compute(400)\n" +
                "(await &f1) + (await &f2) + (await &f3) + (await &f4)"
        );
        assertBothEqual(1000, result);
    }

    /**
     * 并发压力测试：async 函数内多层嵌套调用
     */
    @RepeatedTest(20)
    public void asyncDeeplyNestedCallsNoCrossTalk() throws Exception {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def add(a, b) = &a + &b\n" +
                "def mul(a, b) = &a * &b\n" +
                "async def calc(x) = {\n" +
                "  s = add(&x, &x)\n" +
                "  p = mul(&s, &x)\n" +
                "  return &p\n" +
                "}\n" +
                "f1 = calc(3)\n" +
                "f2 = calc(5)\n" +
                "f3 = calc(7)\n" +
                "(await &f1) + (await &f2) + (await &f3)"
        );
        // calc(3) = 3+3=6, 6*3=18; calc(5) = 5+5=10, 10*5=50; calc(7) = 7+7=14, 14*7=98
        // 18 + 50 + 98 = 166
        assertBothEqual(166, result);
    }

    private static FunctionContext<?> firstStackContext(FunctionContextPool pool) throws Exception {
        Field stackField = FunctionContextPool.class.getDeclaredField("stack");
        stackField.setAccessible(true);
        FunctionContext<?>[] stack = (FunctionContext<?>[]) stackField.get(pool);
        return stack[0];
    }

    private static Object field(FunctionContext<?> context, String name) throws Exception {
        Field field = FunctionContext.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(context);
    }

    private static Object frameField(FunctionContext<?> context, String name) throws Exception {
        Object arguments = field(context, "arguments");
        Field field = FunctionArgumentFrame.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(arguments);
    }

    private static boolean refsContain(FunctionContext<?> context, Object value) throws Exception {
        Object[] refs = (Object[]) frameField(context, "refs");
        for (Object ref : refs) {
            if (ref == value) {
                return true;
            }
        }
        return false;
    }
}
