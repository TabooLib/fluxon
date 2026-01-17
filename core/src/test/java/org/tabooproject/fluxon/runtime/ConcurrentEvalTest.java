package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 并发执行测试 - 验证脚本和函数支持多线程并发调用
 */
@SuppressWarnings("deprecation")
public class ConcurrentEvalTest {

    /**
     * 创建允许无效引用的脚本实例（用于运行时变量）
     */
    private RuntimeScriptBase createScriptWithDynamicVars(String source, String className) throws Exception {
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowInvalidReference(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        CompileResult compileResult = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        return (RuntimeScriptBase) scriptClass.newInstance();
    }

    /**
     * 测试1: 基本并发 - 同一个脚本实例被多个线程并发调用
     */
    @Test
    void testBasicConcurrency() throws Exception {
        String source = "&x + 10";
        RuntimeScriptBase script = createScriptWithDynamicVars(source, "ConcurrentTest1");

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Integer>> futures = new ArrayList<>();

        // 并发执行，每个线程使用不同的变量值
        for (int i = 0; i < threadCount; i++) {
            final int value = i;
            Future<Integer> future = executor.submit(() -> {
                Environment env = FluxonRuntime.getInstance().newEnvironment();
                env.defineRootVariable("x", value);
                Object result = script.eval(env);
                return ((Number) result).intValue();
            });
            futures.add(future);
        }

        // 验证所有结果正确
        for (int i = 0; i < threadCount; i++) {
            int expected = i + 10;
            int actual = futures.get(i).get(5, TimeUnit.SECONDS);
            assertEquals(expected, actual, "Thread " + i + " result mismatch");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    /**
     * 测试2: 变量隔离 - 验证不同线程的变量互不影响
     */
    @Test
    void testVariableIsolation() throws Exception {
        String source = "&x * 2 + &y";
        RuntimeScriptBase script = createScriptWithDynamicVars(source, "ConcurrentTest2");

        int iterations = 30;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Integer>> futures = new ArrayList<>();

        // 并发执行，每个线程使用不同的 x 和 y 值
        for (int i = 0; i < iterations; i++) {
            final int x = i;
            final int y = i * 10;
            Future<Integer> future = executor.submit(() -> {
                Environment env = FluxonRuntime.getInstance().newEnvironment();
                env.defineRootVariable("x", x);
                env.defineRootVariable("y", y);
                Object result = script.eval(env);
                return ((Number) result).intValue();
            });
            futures.add(future);
        }

        // 验证每个线程的计算结果正确
        for (int i = 0; i < iterations; i++) {
            int expected = i * 2 + i * 10; // x * 2 + y
            int actual = futures.get(i).get(5, TimeUnit.SECONDS);
            assertEquals(expected, actual, "Thread " + i + " variable isolation failed");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    /**
     * 测试3: Lambda 函数并发
     */
    @Test
    void testLambdaConcurrency() throws Exception {
        String source = "x = &value; run { &x * 2 }";
        RuntimeScriptBase script = createScriptWithDynamicVars(source, "ConcurrentTest3");

        int threadCount = 40;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int value = i;
            Future<Integer> future = executor.submit(() -> {
                Environment env = FluxonRuntime.getInstance().newEnvironment();
                env.defineRootVariable("value", value);
                Object result = script.eval(env);
                return ((Number) result).intValue();
            });
            futures.add(future);
        }

        // 验证结果
        for (int i = 0; i < threadCount; i++) {
            int expected = i * 2;
            int actual = futures.get(i).get(5, TimeUnit.SECONDS);
            assertEquals(expected, actual, "Thread " + i + " lambda result mismatch");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    /**
     * 测试4: 高并发压力测试
     */
    @Test
    void testHighConcurrencyStress() throws Exception {
        String source = "&a + &b * &c";
        RuntimeScriptBase script = createScriptWithDynamicVars(source, "ConcurrentTest4");

        int threadCount = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        // 创建大量线程同时启动
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final int value = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // 等待统一启动信号
                    Environment env = FluxonRuntime.getInstance().newEnvironment();
                    env.defineRootVariable("a", value);
                    env.defineRootVariable("b", value + 1);
                    env.defineRootVariable("c", 2);
                    Object result = script.eval(env);
                    int expected = value + (value + 1) * 2; // a + b * c
                    int actual = ((Number) result).intValue();
                    assertEquals(expected, actual);
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 统一启动所有线程
        startLatch.countDown();
        assertTrue(endLatch.await(10, TimeUnit.SECONDS), "Threads did not complete in time");

        // 验证无错误
        if (!errors.isEmpty()) {
            fail("Concurrent execution errors: " + errors.size() + " errors occurred. First error: " + errors.get(0).getMessage());
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    /**
     * 测试5: 多个不同脚本并发执行
     */
    @Test
    void testMultipleScriptsConcurrency() throws Exception {
        RuntimeScriptBase script1 = createScriptWithDynamicVars("&x * 2", "ConcurrentTest5_1");
        RuntimeScriptBase script2 = createScriptWithDynamicVars("&x * 3", "ConcurrentTest5_2");
        RuntimeScriptBase script3 = createScriptWithDynamicVars("&x * 5", "ConcurrentTest5_3");

        int iterations = 20;
        ExecutorService executor = Executors.newFixedThreadPool(15);
        List<Future<Integer>> futures = new ArrayList<>();

        // 并发执行不同脚本
        for (int i = 0; i < iterations; i++) {
            final int value = i;
            futures.add(executor.submit(() -> {
                Environment env = FluxonRuntime.getInstance().newEnvironment();
                env.defineRootVariable("x", value);
                return ((Number) script1.eval(env)).intValue();
            }));
            futures.add(executor.submit(() -> {
                Environment env = FluxonRuntime.getInstance().newEnvironment();
                env.defineRootVariable("x", value);
                return ((Number) script2.eval(env)).intValue();
            }));
            futures.add(executor.submit(() -> {
                Environment env = FluxonRuntime.getInstance().newEnvironment();
                env.defineRootVariable("x", value);
                return ((Number) script3.eval(env)).intValue();
            }));
        }

        // 验证结果
        for (int i = 0; i < iterations; i++) {
            assertEquals(i * 2, futures.get(i * 3).get(5, TimeUnit.SECONDS));
            assertEquals(i * 3, futures.get(i * 3 + 1).get(5, TimeUnit.SECONDS));
            assertEquals(i * 5, futures.get(i * 3 + 2).get(5, TimeUnit.SECONDS));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
}
