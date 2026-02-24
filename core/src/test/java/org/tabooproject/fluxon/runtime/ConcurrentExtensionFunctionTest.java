package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.parser.ParsedScript;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.runtime.FunctionSignature.returnsObject;

/**
 * 并发扩展函数测试 - 验证 AST 缓存字段在多线程间的竞争问题
 *
 * 复现场景：
 * FunctionCallExpression 上的 cachedHandlerTag / resolvedExtensionFunction 等字段
 * 在 AST 被多线程共享时发生 data race，导致参数类型被污染。
 *
 * @author sky
 */
public class ConcurrentExtensionFunctionTest {

    /**
     * 两个不同的 target 类型
     */
    public static class TypeA {
        public final String name;
        TypeA(String name) { this.name = name; }
    }

    public static class TypeB {
        public final int value;
        TypeB(int value) { this.value = value; }
    }

    @BeforeAll
    static void registerExtensions() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        // TypeA.check(String) -> 返回字符串本身
        runtime.registerExtension(TypeA.class)
                .function("check", returnsObject().params(Type.STRING), ctx -> {
                    String arg = ctx.getString(0);
                    ctx.setReturnRef("A:" + arg);
                });
        // TypeB.check(String) -> 返回字符串本身
        runtime.registerExtension(TypeB.class)
                .function("check", returnsObject().params(Type.STRING), ctx -> {
                    String arg = ctx.getString(0);
                    ctx.setReturnRef("B:" + arg);
                });
    }

    /**
     * 核心测试：同一份 AST，不同 target 类型并发执行扩展函数
     *
     * 如果 FunctionCallExpression 的缓存字段存在竞争：
     * - 线程 A (target=TypeA) 可能读到线程 B 缓存的 resolvedExtensionFunction
     * - 导致类型不匹配或返回错误结果
     */
    @RepeatedTest(10)
    void testConcurrentExtensionFunctionWithDifferentTargets() throws Exception {
        // 解析一次，共享 AST
        String source = "&target::check('hello')";
        Environment parseEnv = FluxonRuntime.getInstance().newEnvironment();
        parseEnv.defineRootVariable("target", new TypeA("dummy"));
        ParsedScript script = Fluxon.parse(source, parseEnv);

        int threadCount = 64;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            boolean useTypeA = (i % 2 == 0);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Environment env = script.newEnvironment();
                    if (useTypeA) {
                        env.defineRootVariable("target", new TypeA("test"));
                    } else {
                        env.defineRootVariable("target", new TypeB(42));
                    }
                    Object result = script.eval(env);
                    // 验证结果
                    String expected = useTypeA ? "A:hello" : "B:hello";
                    if (!expected.equals(result)) {
                        errorCount.incrementAndGet();
                        errors.add(new AssertionError(
                                "Expected " + expected + " but got " + result
                                        + " (type=" + (result == null ? "null" : result.getClass().getSimpleName()) + ")"
                        ));
                    }
                } catch (Throwable e) {
                    errorCount.incrementAndGet();
                    errors.add(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(endLatch.await(10, TimeUnit.SECONDS), "Threads did not complete in time");
        executor.shutdown();

        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(errorCount.get()).append(" errors out of ").append(threadCount).append(" threads:\n");
            for (int i = 0; i < Math.min(5, errors.size()); i++) {
                Throwable e = errors.get(i);
                sb.append("  - ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append("\n");
            }
            fail(sb.toString());
        }
    }

    /**
     * 补充测试：同一 target 类型但大量并发，验证参数不会被其他线程的值污染
     */
    @RepeatedTest(10)
    void testConcurrentSameTargetDifferentArgs() throws Exception {
        String source = "&target::check(&arg)";
        Environment parseEnv = FluxonRuntime.getInstance().newEnvironment();
        parseEnv.defineRootVariable("target", new TypeA("dummy"));
        parseEnv.defineRootVariable("arg", "dummy");
        ParsedScript script = Fluxon.parse(source, parseEnv);

        int threadCount = 64;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Environment env = script.newEnvironment();
                    env.defineRootVariable("target", new TypeA("t" + idx));
                    env.defineRootVariable("arg", "v" + idx);
                    Object result = script.eval(env);
                    String expected = "A:v" + idx;
                    if (!expected.equals(result)) {
                        errorCount.incrementAndGet();
                        errors.add(new AssertionError(
                                "Thread " + idx + ": expected " + expected + " but got " + result
                        ));
                    }
                } catch (Throwable e) {
                    errorCount.incrementAndGet();
                    errors.add(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(endLatch.await(10, TimeUnit.SECONDS), "Threads did not complete in time");
        executor.shutdown();

        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(errorCount.get()).append(" errors out of ").append(threadCount).append(" threads:\n");
            for (int i = 0; i < Math.min(5, errors.size()); i++) {
                Throwable e = errors.get(i);
                sb.append("  - ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append("\n");
            }
            fail(sb.toString());
        }
    }

    /**
     * 压力测试：长时间、大量迭代的并发执行
     * 增加竞态窗口被命中的概率
     */
    @RepeatedTest(5)
    void testHighVolumeStress() throws Exception {
        String source = "&target::check('stress')";
        Environment parseEnv = FluxonRuntime.getInstance().newEnvironment();
        parseEnv.defineRootVariable("target", new TypeA("dummy"));
        ParsedScript script = Fluxon.parse(source, parseEnv);

        int threadCount = 16;
        int iterationsPerThread = 500;
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            boolean useTypeA = (t % 2 == 0);
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterationsPerThread; j++) {
                        Environment env = script.newEnvironment();
                        if (useTypeA) {
                            env.defineRootVariable("target", new TypeA("s"));
                        } else {
                            env.defineRootVariable("target", new TypeB(j));
                        }
                        Object result = script.eval(env);
                        String expected = useTypeA ? "A:stress" : "B:stress";
                        if (!expected.equals(result)) {
                            errorCount.incrementAndGet();
                            if (errors.size() < 10) {
                                errors.add(new AssertionError("Expected " + expected + " but got " + result));
                            }
                        }
                    }
                } catch (Throwable e) {
                    errorCount.incrementAndGet();
                    if (errors.size() < 10) {
                        errors.add(e);
                    }
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(errorCount.get()).append(" errors in ").append(threadCount * iterationsPerThread).append(" total executions:\n");
            for (int i = 0; i < Math.min(5, errors.size()); i++) {
                Throwable e = errors.get(i);
                sb.append("  - ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append("\n");
            }
            fail(sb.toString());
        }
    }
}
