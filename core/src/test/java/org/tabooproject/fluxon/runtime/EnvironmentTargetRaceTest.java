package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.RepeatedTest;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Environment.target 竞态回归测试
 *
 * 验证多线程通过同一个 RuntimeScriptBase 实例的 callFunction 并发调用
 * 包含 :: 上下文调用的编译后函数时，不会出现 target 互相覆盖
 *
 * @author sky
 */
public class EnvironmentTargetRaceTest {

    // 模拟 Frontier 的 staminaConsume 场景：
    // 函数内同时有 :: 上下文调用（读取 target）和 for 循环遍历 Map（修改 target）
    private static final String SCRIPT = "" +
            "def process(items, label) {\n" +
            "    result = [:]\n" +
            "    for entry in &items::entrySet() {\n" +
            "        key = &entry::key()\n" +
            "        value = &entry::value()\n" +
            "        len = &key::length()\n" +
            "        &result::put(&key, &value * &len)\n" +
            "    }\n" +
            "    &result::put(\"_label\", &label::uppercase())\n" +
            "    &result\n" +
            "}\n";

    /**
     * 多线程并发调用 callFunction，验证 :: 上下文调用的 target 隔离
     * 修复前：Environment.target 被多线程互相覆盖，导致 FunctionNotFoundError 或参数错位
     */
    @RepeatedTest(20)
    void testConcurrentCallFunctionTargetIsolation() throws Exception {
        // 编译脚本
        CompileResult compileResult = Fluxon.compile(SCRIPT, "TargetRaceTest");
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base = (RuntimeScriptBase) scriptClass.newInstance();
        // 共享同一个 Environment（模拟 Frontier 的 scriptFunctions 共享模式）
        Environment sharedEnv = FluxonRuntime.getInstance().newEnvironment();
        base.eval(sharedEnv);
        // 并发调用
        int threadCount = 8;
        int iterationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            int threadId = t;
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < iterationsPerThread; i++) {
                    try {
                        // 每个线程用不同的参数调用，方便验证结果正确性
                        Map<String, Integer> items = new LinkedHashMap<>();
                        items.put("ab", threadId + 1);
                        items.put("cde", (threadId + 1) * 10);
                        String label = "thread" + threadId;
                        Object result = base.callFunction("process", null, new Object[]{items, label});
                        // 验证结果
                        assertTrue(result instanceof Map, "Result should be Map, got: " + (result != null ? result.getClass() : "null"));
                        @SuppressWarnings("unchecked")
                        Map<String, Object> resultMap = (Map<String, Object>) result;
                        // "ab" 长度 2，值 (threadId+1) * 2
                        assertEquals((threadId + 1) * 2, resultMap.get("ab"), "ab value mismatch, thread=" + threadId + " iter=" + i);
                        // "cde" 长度 3，值 (threadId+1)*10 * 3
                        assertEquals((threadId + 1) * 10 * 3, resultMap.get("cde"), "cde value mismatch, thread=" + threadId + " iter=" + i);
                        // label 应该是大写
                        assertEquals("THREAD" + threadId, resultMap.get("_label"), "_label mismatch, thread=" + threadId + " iter=" + i);
                    } catch (Throwable e) {
                        errorCount.incrementAndGet();
                        System.err.println("Thread " + threadId + " iter " + i + " error: " + e.getMessage());
                    }
                }
            }));
        }
        // 同时释放所有线程
        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();
        assertEquals(0, errorCount.get(), "Expected zero errors from concurrent callFunction");
    }
}
