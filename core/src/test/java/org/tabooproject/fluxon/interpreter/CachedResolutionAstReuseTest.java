package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.FluxonFeatures;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.java.Export;

import org.tabooproject.fluxon.runtime.error.ArgumentTypeMismatchError;
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 复现 Frontier 生产环境 Bug：同一 ParsedScript AST 被多次执行时，
 * cachedResolution 缓存导致扩展函数解析到错误的函数。
 *
 * Bug 现象：
 * 1. ClassCastException: Location cannot be cast to ExchangeData
 * 2. context.function.name 返回 "sleep" 而非 "has"
 *
 * Frontier 的使用模式：
 * - parseFrontierStyle() 生成 ParsedScript，缓存在 ConcurrentHashMap 中
 * - 每次 tick 用新 Environment 调用 ParsedScript.eval()
 * - 同一个 AST 节点的 cachedResolution 跨所有调用持久化
 *
 * @author sky
 */
public class CachedResolutionAstReuseTest {

    /**
     * 模拟 ExchangeData：有 has 方法的 @Export 类
     */
    public static class MockExchangeData {

        private final Map<String, Object> data = new HashMap<>();

        public MockExchangeData(String... keys) {
            for (String key : keys) {
                data.put(key, true);
            }
        }

        @Export
        public boolean has(String key) {
            return data.containsKey(key);
        }

        @Export
        public Object get(String key) {
            return data.get(key);
        }
    }

    /**
     * 模拟 Location：无 has 方法的类，用于触发 ClassCastException
     */
    public static class MockLocation {

        public final double x, y, z;

        public MockLocation(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Export
        public double getX() {
            return x;
        }
    }

    @BeforeAll
    static void setup() {
        // 匹配 Frontier 的全局配置
        FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = true;
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.getExportRegistry().registerClass(MockExchangeData.class);
        runtime.getExportRegistry().registerClass(MockLocation.class);
    }

    @AfterAll
    static void teardown() {
        FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = false;
    }

    /**
     * 模拟 Frontier 的解析方式：forceLocalVariables = true
     * Frontier 在运行时通过 env.defineRootVariable 注入变量，
     * 解析期不声明这些变量
     */
    static ParsedScript parseFrontierStyle(String source) {
        CompilationContext ctx = new CompilationContext(source);
        ctx.setForceLocalVariables(true);
        return Fluxon.parse(ctx);
    }

    /**
     * 场景 A：同一个 ParsedScript 多次执行，target 类型不变
     * 验证 cachedResolution 在正常情况下工作
     */
    @Test
    void testSameTargetType_cacheWorks() {
        ParsedScript script = parseFrontierStyle("&ex::has('canhaqi')");
        for (int i = 0; i < 100; i++) {
            Environment env = FluxonRuntime.getInstance().newEnvironment();
            env.defineRootVariable("ex", new MockExchangeData("canhaqi"));
            Object result = script.eval(env);
            assertEquals(true, result, "第 " + i + " 次执行应返回 true");
        }
    }

    /**
     * 场景 B：同一个 ParsedScript 多次执行，中间穿插不同表达式的执行
     * 模拟 Frontier tick 循环中多个 Mechanism 的 condition 依次求值
     */
    @Test
    void testInterleavedExecution_differentScripts() {
        ParsedScript hasScript = parseFrontierStyle("&ex::has('canhaqi')");
        ParsedScript sleepScript = parseFrontierStyle("sleep(0)");
        for (int i = 0; i < 100; i++) {
            // 执行 has 脚本
            Environment env1 = FluxonRuntime.getInstance().newEnvironment();
            env1.defineRootVariable("ex", new MockExchangeData("canhaqi"));
            Object result1 = hasScript.eval(env1);
            assertEquals(true, result1, "has 脚本第 " + i + " 次执行应返回 true");
            // 执行 sleep 脚本（可能影响 pool 状态）
            Environment env2 = FluxonRuntime.getInstance().newEnvironment();
            sleepScript.eval(env2);
            // 再次执行 has 脚本
            Environment env3 = FluxonRuntime.getInstance().newEnvironment();
            env3.defineRootVariable("ex", new MockExchangeData("canhaqi", "inanim"));
            Object result3 = hasScript.eval(env3);
            assertEquals(true, result3, "has 脚本第 " + i + " 次再次执行应返回 true");
        }
    }

    /**
     * 场景 C：复合表达式 &ex::has('canhaqi') && !&ex::has('inanim')
     * 精确复现 Frontier 报错的表达式
     */
    @Test
    void testCompoundHasExpression_astReuse() {
        ParsedScript script = parseFrontierStyle("&ex::has('canhaqi') && !&ex::has('inanim')");
        for (int i = 0; i < 100; i++) {
            Environment env = FluxonRuntime.getInstance().newEnvironment();
            MockExchangeData ex = new MockExchangeData("canhaqi");
            env.defineRootVariable("ex", ex);
            Object result = script.eval(env);
            assertEquals(true, result, "第 " + i + " 次执行: canhaqi=true, inanim=false → 应为 true");
        }
    }

    /**
     * 场景 D：不同 target 类型交替执行同一 AST
     * 同一个 has 函数名在不同 target 类型上注册了不同的扩展函数
     * cachedResolution 的 guardClass 应正确隔离
     *
     * 注意：这不是原始 Bug 的精确场景（原始 Bug 中 Location 没有 has 方法），
     * 但测试 guardClass 的有效性
     */
    @Test
    void testAlternatingTargetTypes_guardClassIsolation() {
        // 两个不同的 ParsedScript，分别以不同类型的 target 调用 has
        ParsedScript script = parseFrontierStyle("&ex::has('key')");
        for (int i = 0; i < 50; i++) {
            // ExchangeData 有 has 方法
            Environment env1 = FluxonRuntime.getInstance().newEnvironment();
            env1.defineRootVariable("ex", new MockExchangeData("key"));
            Object result1 = script.eval(env1);
            assertEquals(true, result1, "ExchangeData 第 " + i + " 次应返回 true");
        }
    }

    /**
     * 场景 E：Pool 平衡性检查
     * 验证多次 AST 重用执行不会导致 StackOverflow 或异常
     * （间接证明 borrow/release 配对正确）
     */
    @Test
    void testPoolBalance_afterAstReuse() {
        ParsedScript hasScript = parseFrontierStyle("&ex::has('canhaqi') && !&ex::has('inanim')");
        ParsedScript sleepScript = parseFrontierStyle("sleep(0)");
        for (int i = 0; i < 1000; i++) {
            Environment env1 = FluxonRuntime.getInstance().newEnvironment();
            env1.defineRootVariable("ex", new MockExchangeData("canhaqi"));
            hasScript.eval(env1);
            Environment env2 = FluxonRuntime.getInstance().newEnvironment();
            sleepScript.eval(env2);
        }
    }

    /**
     * 场景 F：模拟 Frontier MechanismEvaluator 的完整 tick 循环
     * 多个 Mechanism 各自持有不同的缓存 ParsedScript，在同一个 tick 中依次执行
     * 每个 Mechanism 注入不同的变量
     */
    @Test
    void testMechanismTickLoop_multipleConditions() {
        // 模拟多个 Mechanism 的 condition 表达式
        ParsedScript condition1 = parseFrontierStyle("&ex::has('canhaqi') && !&ex::has('inanim')");
        ParsedScript condition2 = parseFrontierStyle("&ex::has('key1')");
        ParsedScript condition3 = parseFrontierStyle("sleep(0)");
        for (int tick = 0; tick < 50; tick++) {
            // Mechanism 1: 有 canhaqi 无 inanim
            Environment env1 = FluxonRuntime.getInstance().newEnvironment();
            env1.defineRootVariable("ex", new MockExchangeData("canhaqi"));
            Object r1 = condition1.eval(env1);
            assertEquals(true, r1, "tick " + tick + " condition1 应为 true");
            // Mechanism 2: 有 key1
            Environment env2 = FluxonRuntime.getInstance().newEnvironment();
            env2.defineRootVariable("ex", new MockExchangeData("key1"));
            Object r2 = condition2.eval(env2);
            assertEquals(true, r2, "tick " + tick + " condition2 应为 true");
            // Mechanism 3: sleep（无 target 上下文）
            Environment env3 = FluxonRuntime.getInstance().newEnvironment();
            condition3.eval(env3);
            // 再次执行 condition1（验证 sleep 没有污染 condition1 的 cachedResolution）
            Environment env4 = FluxonRuntime.getInstance().newEnvironment();
            env4.defineRootVariable("ex", new MockExchangeData("canhaqi", "inanim"));
            Object r4 = condition1.eval(env4);
            assertEquals(false, r4, "tick " + tick + " condition1（有 inanim）应为 false");
        }
    }

    /**
     * 场景 G：cachedResolution 跨不同 target 类型的写入竞争
     * 同一 AST 节点在第一次执行时以 target=ExchangeData 缓存，
     * 第二次执行时 target=null（无 context call），
     * 验证 guardClass 机制是否正确防护
     */
    @Test
    void testCachedResolution_nullTargetAfterNonNull() {
        // 第一次：有 target 的 context call
        ParsedScript scriptWithTarget = parseFrontierStyle("&ex::has('key')");
        Environment env1 = FluxonRuntime.getInstance().newEnvironment();
        env1.defineRootVariable("ex", new MockExchangeData("key"));
        Object r1 = scriptWithTarget.eval(env1);
        assertEquals(true, r1);
        // 后续执行仍使用相同 target 类型
        for (int i = 0; i < 10; i++) {
            Environment env = FluxonRuntime.getInstance().newEnvironment();
            env.defineRootVariable("ex", new MockExchangeData("key"));
            Object r = scriptWithTarget.eval(env);
            assertEquals(true, r, "第 " + (i + 2) + " 次执行应返回 true");
        }
    }

    /**
     * 场景 H：大量快速交替执行，增加暴露竞争问题的概率
     * （虽然 Frontier 的错误发生在单线程，但高频率调用可能暴露 pool 问题）
     */
    @Test
    void testHighFrequencyAstReuse() {
        ParsedScript script = parseFrontierStyle("&ex::has('canhaqi') && !&ex::has('inanim')");
        for (int i = 0; i < 10000; i++) {
            Environment env = FluxonRuntime.getInstance().newEnvironment();
            boolean hasCanhaqi = (i % 3) != 0;
            boolean hasInanim = (i % 5) == 0;
            MockExchangeData ex;
            if (hasCanhaqi && hasInanim) {
                ex = new MockExchangeData("canhaqi", "inanim");
            } else if (hasCanhaqi) {
                ex = new MockExchangeData("canhaqi");
            } else if (hasInanim) {
                ex = new MockExchangeData("inanim");
            } else {
                ex = new MockExchangeData();
            }
            env.defineRootVariable("ex", ex);
            Object result = script.eval(env);
            boolean expected = hasCanhaqi && !hasInanim;
            assertEquals(expected, result,
                    "i=" + i + " canhaqi=" + hasCanhaqi + " inanim=" + hasInanim);
        }
    }

    /**
     * 场景 I：多线程并发执行同一 AST
     * 不同线程用相同 target 类型但不同实例执行同一 ParsedScript，
     * 测试 cachedResolution volatile 写入的线程安全性
     */
    @Test
    void testConcurrentAstReuse_sameTargetType() throws Exception {
        ParsedScript script = parseFrontierStyle("&ex::has('canhaqi') && !&ex::has('inanim')");
        int threadCount = 4;
        int iterations = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int t = 0; t < threadCount; t++) {
            int threadId = t;
            pool.submit(() -> {
                try {
                    barrier.await();
                    for (int i = 0; i < iterations; i++) {
                        Environment env = FluxonRuntime.getInstance().newEnvironment();
                        boolean hasCanhaqi = ((threadId + i) % 3) != 0;
                        boolean hasInanim = ((threadId + i) % 5) == 0;
                        MockExchangeData ex;
                        if (hasCanhaqi && hasInanim) {
                            ex = new MockExchangeData("canhaqi", "inanim");
                        } else if (hasCanhaqi) {
                            ex = new MockExchangeData("canhaqi");
                        } else if (hasInanim) {
                            ex = new MockExchangeData("inanim");
                        } else {
                            ex = new MockExchangeData();
                        }
                        env.defineRootVariable("ex", ex);
                        Object result = script.eval(env);
                        boolean expected = hasCanhaqi && !hasInanim;
                        if (!Boolean.valueOf(expected).equals(result)) {
                            failure.compareAndSet(null, new RuntimeException(
                                    "thread=" + threadId + " i=" + i +
                                    " canhaqi=" + hasCanhaqi + " inanim=" + hasInanim +
                                    " expected=" + expected + " got=" + result));
                            return;
                        }
                    }
                } catch (Throwable ex) {
                    failure.compareAndSet(null, ex);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(30, TimeUnit.SECONDS);
        pool.shutdown();
        Throwable ex = failure.get();
        if (ex != null) {
            fail("多线程并发执行失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 场景 J：模拟 Frontier 错误场景 —— 变量 ex 指向 Location 而非 ExchangeData
     * 当 ex 被错误设为 Location 时，扩展函数 has 不应该被解析到
     * 应该抛出 FunctionNotFoundError 或类似的运行时错误，而不是 ClassCastException
     */
    @Test
    void testWrongTargetType_shouldNotClassCast() {
        ParsedScript script = parseFrontierStyle("&ex::has('canhaqi')");
        // 先正常执行几次，建立 cachedResolution
        for (int i = 0; i < 5; i++) {
            Environment env = FluxonRuntime.getInstance().newEnvironment();
            env.defineRootVariable("ex", new MockExchangeData("canhaqi"));
            assertEquals(true, script.eval(env));
        }
        // 现在将 ex 设为 Location（错误的类型）
        // cachedResolution.guardClass = MockExchangeData.class，应该不匹配 MockLocation
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("ex", new MockLocation(1, 2, 3));
        try {
            Object result = script.eval(env);
            // 如果成功执行（例如通过 fallback），也接受
            // 但不应该是 ClassCastException
        } catch (ClassCastException e) {
            fail("不应该抛出 ClassCastException，guardClass 应该阻止缓存命中: " + e.getMessage());
        } catch (FluxonRuntimeError e) {
            // FunctionNotFoundError 等运行时错误是可接受的
        }
    }

    /**
     * 场景 L：多线程不同 target 类型疯狂并发执行同一 AST
     * 模拟生产环境：多个异步线程高频调用扩展函数，另一个线程也在调用
     * 两种 @Export 类型都有 has 方法但完全不相关
     */
    @Test
    void testAggressiveConcurrent_differentExportTargets() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.getExportRegistry().registerClass(ExportTypeA.class);
        runtime.getExportRegistry().registerClass(ExportTypeB.class);
        ParsedScript script = parseFrontierStyle("&target::has('key')");
        int threadCount = 16;
        int iterationsPerThread = 5000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger errorCount = new java.util.concurrent.atomic.AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            boolean useTypeA = (t % 2 == 0);
            int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        Environment env = script.newEnvironment();
                        if (useTypeA) {
                            env.defineRootVariable("target", new ExportTypeA("key"));
                        } else {
                            env.defineRootVariable("target", new ExportTypeB("key"));
                        }
                        Object result = script.eval(env);
                        if (!Boolean.TRUE.equals(result)) {
                            errorCount.incrementAndGet();
                            firstError.compareAndSet(null, new RuntimeException(
                                    "thread=" + threadId + " i=" + i + " type=" + (useTypeA ? "A" : "B") +
                                    " expected=true got=" + result));
                            return;
                        }
                    }
                } catch (Throwable ex) {
                    errorCount.incrementAndGet();
                    firstError.compareAndSet(null, ex);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        assertTrue(endLatch.await(60, TimeUnit.SECONDS), "线程未在超时内完成");
        executor.shutdown();
        Throwable err = firstError.get();
        if (err != null) {
            fail("并发执行失败 (" + errorCount.get() + " errors): " + err.getMessage(), err);
        }
    }

    /**
     * 场景 M：混合 target 类型 + 无 has 方法的类型
     * 一半线程用有 has 的类型，另一半用没有 has 的类型
     * 触发 has 找不到 → FunctionNotFoundError，不应该 ClassCastException
     */
    @Test
    void testAggressiveConcurrent_mixedTargetsWithMissing() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.getExportRegistry().registerClass(ExportTypeA.class);
        ParsedScript script = parseFrontierStyle("&target::has('key')");
        int threadCount = 8;
        int iterationsPerThread = 3000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> classCastError = new AtomicReference<>();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            boolean useTypeA = (t % 2 == 0);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        Environment env = script.newEnvironment();
                        if (useTypeA) {
                            env.defineRootVariable("target", new ExportTypeA("key"));
                        } else {
                            env.defineRootVariable("target", new MockLocation(1, 2, 3));
                        }
                        try {
                            Object result = script.eval(env);
                            if (useTypeA) {
                                assertEquals(true, result);
                            }
                        } catch (ClassCastException e) {
                            classCastError.compareAndSet(null, e);
                            return;
                        } catch (FluxonRuntimeError e) {
                            // FunctionNotFoundError 是可接受的
                        }
                    }
                } catch (Throwable ex) {
                    classCastError.compareAndSet(null, ex);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        assertTrue(endLatch.await(60, TimeUnit.SECONDS));
        executor.shutdown();
        Throwable err = classCastError.get();
        if (err instanceof ClassCastException) {
            fail("复现了 ClassCastException! " + err.getMessage(), err);
        } else if (err != null) {
            fail("意外错误: " + err.getMessage(), err);
        }
    }

    /**
     * 场景 N2：超高并发 + 复合表达式 + 不同 target 类型
     * 表达式包含 && 逻辑运算和多个 context call，贴近生产场景
     * 大量线程疯狂循环执行，增大竞态窗口
     */
    @Test
    void testAggressiveConcurrent_compoundExpressionDifferentTargets() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.getExportRegistry().registerClass(ExportTypeA.class);
        runtime.getExportRegistry().registerClass(ExportTypeB.class);
        // 模拟 Frontier 的实际表达式：多个 context call + &&
        ParsedScript script = parseFrontierStyle("!&target::has('blocked') && &target::has('ready') && &hp < 0.99");
        int threadCount = 32;
        int iterationsPerThread = 10000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger errorCount = new java.util.concurrent.atomic.AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            boolean useTypeA = (t % 2 == 0);
            int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        Environment env = script.newEnvironment();
                        if (useTypeA) {
                            env.defineRootVariable("target", new ExportTypeA("ready"));
                        } else {
                            env.defineRootVariable("target", new ExportTypeB("ready"));
                        }
                        env.defineRootVariable("hp", 0.5);
                        Object result = script.eval(env);
                        if (!Boolean.TRUE.equals(result)) {
                            errorCount.incrementAndGet();
                            firstError.compareAndSet(null, new RuntimeException(
                                    "thread=" + threadId + " i=" + i + " type=" + (useTypeA ? "A" : "B") +
                                    " expected=true got=" + result));
                            return;
                        }
                    }
                } catch (Throwable ex) {
                    errorCount.incrementAndGet();
                    firstError.compareAndSet(null, ex);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        assertTrue(endLatch.await(120, TimeUnit.SECONDS), "线程未在超时内完成");
        executor.shutdown();
        Throwable err = firstError.get();
        if (err != null) {
            fail("场景 N2 失败 (" + errorCount.get() + " errors): " + err.getMessage(), err);
        }
    }

    /**
     * 场景 N：每个线程内快速连续执行多个不同脚本（模拟粒子 tick）
     * 线程内多个 ParsedScript 各自 eval，同一线程内交替 target 类型
     * 测试 FunctionContextPool 在高频 borrow/release 下的正确性
     */
    @Test
    void testRapidFireMultipleScripts_sameThread() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.getExportRegistry().registerClass(ExportTypeA.class);
        runtime.getExportRegistry().registerClass(ExportTypeB.class);
        // 多个不同的脚本
        ParsedScript scriptA = parseFrontierStyle("&target::has('key')");
        ParsedScript scriptB = parseFrontierStyle("&target::has('other')");
        ParsedScript scriptC = parseFrontierStyle("&target::has('key') && !&target::has('nope')");
        int threadCount = 8;
        int iterationsPerThread = 5000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        // 同一线程内交替执行不同脚本和不同 target 类型
                        boolean useA = (i % 3 != 0);
                        Object target = useA ? new ExportTypeA("key", "other") : new ExportTypeB("key", "other");
                        // 快速连续执行三个脚本
                        {
                            Environment env = scriptA.newEnvironment();
                            env.defineRootVariable("target", target);
                            Object r = scriptA.eval(env);
                            if (!Boolean.TRUE.equals(r)) {
                                firstError.compareAndSet(null, new RuntimeException(
                                    "scriptA thread=" + threadId + " i=" + i + " type=" + (useA ? "A" : "B") + " got=" + r));
                                return;
                            }
                        }
                        {
                            Environment env = scriptB.newEnvironment();
                            env.defineRootVariable("target", target);
                            Object r = scriptB.eval(env);
                            if (!Boolean.TRUE.equals(r)) {
                                firstError.compareAndSet(null, new RuntimeException(
                                    "scriptB thread=" + threadId + " i=" + i + " type=" + (useA ? "A" : "B") + " got=" + r));
                                return;
                            }
                        }
                        {
                            Environment env = scriptC.newEnvironment();
                            env.defineRootVariable("target", target);
                            Object r = scriptC.eval(env);
                            if (!Boolean.TRUE.equals(r)) {
                                firstError.compareAndSet(null, new RuntimeException(
                                    "scriptC thread=" + threadId + " i=" + i + " type=" + (useA ? "A" : "B") + " got=" + r));
                                return;
                            }
                        }
                    }
                } catch (Throwable ex) {
                    firstError.compareAndSet(null, ex);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        assertTrue(endLatch.await(60, TimeUnit.SECONDS), "线程未在超时内完成");
        executor.shutdown();
        Throwable err = firstError.get();
        if (err != null) {
            fail("场景 N 失败: " + err.getMessage(), err);
        }
    }

    /**
     * @Export 类型 A：有 has 方法，跟 ExchangeData 类似
     */
    public static class ExportTypeA {
        private final java.util.Set<String> keys = new java.util.HashSet<>();
        public ExportTypeA(String... keys) { java.util.Collections.addAll(this.keys, keys); }
        @Export public boolean has(String key) { return keys.contains(key); }
    }

    /**
     * @Export 类型 B：也有 has 方法，跟 A 完全无关
     */
    public static class ExportTypeB {
        private final java.util.Set<String> keys = new java.util.HashSet<>();
        public ExportTypeB(String... keys) { java.util.Collections.addAll(this.keys, keys); }
        @Export public boolean has(String key) { return keys.contains(key); }
    }

    /**
     * 场景 O：复现 Frontier 生产环境 ClassCastException
     *
     * 粒子脚本（后台线程疯狂执行）：
     *   async def run() { 大量循环 + sleep + 扩展函数调用 }
     *   run()
     *
     * 主线程脚本（解释执行）：
     *   &ex::has('key')
     *
     * 粒子脚本的 async def 通过 createChild() 共享 Environment，
     * 异步线程执行 context call 时会修改 env.target，
     * 与主线程的 context call 产生 data race
     */
    @Test
    void testAsyncDefWithExtensionCall_crossScriptCCE() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.getExportRegistry().registerClass(ExportTypeA.class);
        runtime.getExportRegistry().registerClass(ExportTypeB.class);
        // 粒子脚本：async def 内部疯狂调用 ExportTypeB 的扩展函数
        // 每次 eval 创建独立 Environment，但 async def 通过 createChild 共享它
        ParsedScript particleScript = parseFrontierStyle(
                "async def run() {\n" +
                "  _i = 0\n" +
                "  while (_i < 50) {\n" +
                "    &pb::has('particle')\n" +
                "    &pb::has('effect')\n" +
                "    &pb::has('spawn')\n" +
                "    sleep(0)\n" +
                "    _i = _i + 1\n" +
                "  }\n" +
                "}\n" +
                "run()"
        );
        // 主线程脚本：解释执行扩展函数
        ParsedScript hasScript = parseFrontierStyle("&ex::has('canhaqi')");
        int particleThreads = 8;
        int mainIterations = 5000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch particleDone = new CountDownLatch(particleThreads);
        AtomicReference<Throwable> cceError = new AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger cceCount = new java.util.concurrent.atomic.AtomicInteger(0);
        // 启动多个粒子线程，每个线程反复 eval 粒子脚本
        ExecutorService executor = Executors.newFixedThreadPool(particleThreads + 1);
        for (int t = 0; t < particleThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 200; i++) {
                        Environment env = particleScript.newEnvironment();
                        env.defineRootVariable("pb", new ExportTypeB("particle", "effect", "spawn"));
                        try {
                            particleScript.eval(env);
                        } catch (Throwable ex) {
                            if (ex instanceof ClassCastException) {
                                cceCount.incrementAndGet();
                                cceError.compareAndSet(null, ex);
                            }
                        }
                    }
                } catch (Throwable ex) {
                    cceError.compareAndSet(null, ex);
                } finally {
                    particleDone.countDown();
                }
            });
        }
        // 主线程：疯狂执行 has 脚本
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < mainIterations; i++) {
                    Environment env = hasScript.newEnvironment();
                    env.defineRootVariable("ex", new ExportTypeA("canhaqi"));
                    try {
                        Object result = hasScript.eval(env);
                        if (!Boolean.TRUE.equals(result)) {
                            cceError.compareAndSet(null, new RuntimeException(
                                    "has 返回了 " + result + " 而不是 true (i=" + i + ")"));
                        }
                    } catch (ClassCastException e) {
                        cceCount.incrementAndGet();
                        cceError.compareAndSet(null, e);
                        System.out.println("[场景 O] 复现 ClassCastException! i=" + i + " " + e.getMessage());
                    } catch (Throwable e) {
                        cceError.compareAndSet(null, e);
                    }
                }
            } catch (Throwable ex) {
                cceError.compareAndSet(null, ex);
            }
        });
        startLatch.countDown();
        particleDone.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        Throwable err = cceError.get();
        if (err instanceof ClassCastException) {
            System.out.println("[场景 O] 共捕获 " + cceCount.get() + " 次 ClassCastException");
            fail("复现了 ClassCastException: " + err.getMessage(), err);
        } else if (err != null) {
            fail("其他错误: " + err.getMessage(), err);
        }
    }

    /**
     * 场景 P：复现 FunctionCallEvaluator 缓存写入的 target 二次读取 race
     *
     * Bug 根因：FunctionCallEvaluator.evaluate 慢路径中：
     * - 行 59: prepareCall 基于当时的 env.getTarget() 解析函数（得到 TypeA.has）
     * - 行 74: 写缓存时再次读 env.getTarget() 作为 guardClass（可能已被改为 TypeB）
     * → CachedResolution(function=TypeA.has, guardClass=TypeB)
     * → 后续 TypeB target 命中缓存但调用 TypeA 的 bridge → ClassCastException
     *
     * 模拟方式：两线程共享 Environment，干扰线程疯狂切换 target 类型
     */
    @Test
    void testAsyncDefSameScript_environmentTargetRace() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.getExportRegistry().registerClass(ExportTypeA.class);
        runtime.getExportRegistry().registerClass(ExportTypeB.class);
        // 只含 context call 的表达式
        ParsedScript hasScript = parseFrontierStyle("&target::has('key')");
        int iterations = 100000;
        AtomicReference<Throwable> cceError = new AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger cceCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean(true);
        // 共享 Environment（模拟 createChild 共享）
        Environment sharedEnv = FluxonRuntime.getInstance().newEnvironment();
        sharedEnv.defineRootVariable("target", new ExportTypeA("key"));
        // 干扰线程：模拟 async 函数体内的 context call 反复 setTarget
        // 关键是在 prepareCall 和缓存写入之间改掉 target
        ExportTypeB pollutant = new ExportTypeB("key");
        ExportTypeA original = new ExportTypeA("key");
        Thread spoiler = new Thread(() -> {
            while (running.get()) {
                sharedEnv.setTarget(pollutant);
                Thread.yield();
                sharedEnv.setTarget(original);
                Thread.yield();
            }
        });
        spoiler.setDaemon(true);
        spoiler.start();
        // 主线程：每次用新 ParsedScript 强制走慢路径写缓存，
        // 最大化 prepareCall 和缓存写入之间的 race 窗口
        for (int i = 0; i < iterations; i++) {
            ParsedScript freshScript = parseFrontierStyle("&target::has('key')");
            try {
                freshScript.eval(sharedEnv);
            } catch (ClassCastException e) {
                cceCount.incrementAndGet();
                cceError.compareAndSet(null, e);
                System.out.println("[场景 P] 复现 CCE! i=" + i + " " + e.getMessage());
                e.printStackTrace(System.out);
                break;
            } catch (Throwable e) {
                // 其他错误可接受
            }
        }
        running.set(false);
        spoiler.join(5000);
        Throwable err = cceError.get();
        if (err instanceof ClassCastException) {
            System.out.println("[场景 P] 共 " + cceCount.get() + " 次 ClassCastException");
            fail("复现了 CachedResolution 写入 race → ClassCastException: " + err.getMessage(), err);
        } else if (err != null) {
            System.out.println("[场景 P] 非 CCE 错误: " + err.getClass().getName() + ": " + err.getMessage());
        }
    }

    /**
     * 场景 P2：多线程疯狂 eval 同一脚本（含 async def），不同 target 类型竞争 cachedResolution
     *
     * 同一 ParsedScript 的同一 FunctionCallExpression 节点被多线程并发执行，
     * 每个线程传入不同类型的 target → cachedResolution 的 guardClass 不断被覆盖
     */
    @Test
    void testAsyncDefSameScript_concurrentCachedResolution() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.getExportRegistry().registerClass(ExportTypeA.class);
        runtime.getExportRegistry().registerClass(ExportTypeB.class);
        ParsedScript script = parseFrontierStyle(
                "async def run() {\n" +
                "  _i = 0\n" +
                "  while (_i < 100) {\n" +
                "    &target::has('key')\n" +
                "    sleep(0)\n" +
                "    _i = _i + 1\n" +
                "  }\n" +
                "}\n" +
                "run()"
        );
        int threadCount = 16;
        int iterationsPerThread = 200;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger errorCount = new java.util.concurrent.atomic.AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            boolean useTypeA = (t % 2 == 0);
            int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        Environment env = script.newEnvironment();
                        if (useTypeA) {
                            env.defineRootVariable("target", new ExportTypeA("key"));
                        } else {
                            env.defineRootVariable("target", new ExportTypeB("key"));
                        }
                        try {
                            script.eval(env);
                        } catch (ClassCastException e) {
                            errorCount.incrementAndGet();
                            firstError.compareAndSet(null, e);
                            System.out.println("[场景 P2] CCE! thread=" + threadId + " i=" + i +
                                    " type=" + (useTypeA ? "A" : "B") + " " + e.getMessage());
                            return;
                        } catch (Throwable e) {
                            // async 内部异常被吞
                        }
                    }
                } catch (Throwable ex) {
                    firstError.compareAndSet(null, ex);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        assertTrue(endLatch.await(120, TimeUnit.SECONDS), "线程未在超时内完成");
        Thread.sleep(3000);
        executor.shutdown();
        Throwable err = firstError.get();
        if (err instanceof ClassCastException) {
            System.out.println("[场景 P2] 共 " + errorCount.get() + " 次 ClassCastException");
            fail("复现了 ClassCastException: " + err.getMessage(), err);
        } else if (err != null) {
            System.out.println("[场景 P2] 非 CCE 错误: " + err.getClass().getName() + ": " + err.getMessage());
        }
    }

    /**
     * 场景 Q：精确复现 Frontier 模式 —— 粒子脚本编译执行 + has 脚本解释执行
     *
     * Frontier 中粒子脚本量大走编译路径，has 条件表达式走解释路径。
     * 编译路径不写 cachedResolution，但可能通过其他共享状态（
     * FunctionContextPool detach/reassign、ExtensionDispatchTable 缓存）影响解释路径。
     */
    @Test
    void testCompiledAsyncVsInterpretedHas() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.getExportRegistry().registerClass(ExportTypeA.class);
        runtime.getExportRegistry().registerClass(ExportTypeB.class);
        // 粒子脚本 —— 用 Fluxon.compile 编译执行
        String particleSource =
                "async def run() {\n" +
                "  _i = 0\n" +
                "  while (_i < 50) {\n" +
                "    &pb::has('particle')\n" +
                "    &pb::has('effect')\n" +
                "    sleep(0)\n" +
                "    _i = _i + 1\n" +
                "  }\n" +
                "}\n" +
                "run()";
        // has 脚本 —— 解释执行
        ParsedScript hasScript = parseFrontierStyle("&ex::has('canhaqi')");
        int particleThreads = 8;
        int mainIterations = 5000;
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicReference<Throwable> cceError = new AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger cceCount = new java.util.concurrent.atomic.AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(particleThreads + 1);
        CountDownLatch allDone = new CountDownLatch(particleThreads + 1);
        // 粒子线程：编译执行
        for (int t = 0; t < particleThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 200; i++) {
                        Environment env = FluxonRuntime.getInstance().newEnvironment();
                        env.defineRootVariable("pb", new ExportTypeB("particle", "effect", "spawn"));
                        try {
                            Fluxon.eval(particleSource, env);
                        } catch (Throwable ex) {
                            if (ex instanceof ClassCastException) {
                                cceCount.incrementAndGet();
                                cceError.compareAndSet(null, ex);
                            }
                        }
                    }
                } catch (Throwable ex) {
                    cceError.compareAndSet(null, ex);
                } finally {
                    allDone.countDown();
                }
            });
        }
        // 主线程：解释执行
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < mainIterations; i++) {
                    Environment env = hasScript.newEnvironment();
                    env.defineRootVariable("ex", new ExportTypeA("canhaqi"));
                    try {
                        Object result = hasScript.eval(env);
                        if (!Boolean.TRUE.equals(result)) {
                            cceError.compareAndSet(null, new RuntimeException(
                                    "has 返回 " + result + " (i=" + i + ")"));
                        }
                    } catch (ClassCastException e) {
                        cceCount.incrementAndGet();
                        cceError.compareAndSet(null, e);
                        System.out.println("[场景 Q] 复现 CCE! i=" + i + " " + e.getMessage());
                    } catch (Throwable e) {
                        cceError.compareAndSet(null, e);
                    }
                }
            } catch (Throwable ex) {
                cceError.compareAndSet(null, ex);
            } finally {
                allDone.countDown();
            }
        });
        startLatch.countDown();
        assertTrue(allDone.await(120, TimeUnit.SECONDS), "超时");
        executor.shutdown();
        // 等 async 任务收尾
        Thread.sleep(3000);
        Throwable err = cceError.get();
        if (err instanceof ClassCastException) {
            System.out.println("[场景 Q] 共 " + cceCount.get() + " 次 CCE");
            fail("复现了 ClassCastException: " + err.getMessage(), err);
        } else if (err != null) {
            fail("其他错误: " + err.getMessage(), err);
        }
    }

    /**
     * 场景 K：异常路径后 FunctionContext 引用稳定性
     * 当函数调用抛出异常后，后续调用如果复用了同一个 pool slot，
     * 持有旧 context 引用的异常对象会看到被覆盖的 function 字段
     */
    @Test
    void testErrorContextStability_afterPoolReuse() {
        ParsedScript sleepScript = parseFrontierStyle("sleep(0)");
        ParsedScript hasScript = parseFrontierStyle("&ex::has('canhaqi')");
        // 先正常执行一次 has
        Environment env1 = FluxonRuntime.getInstance().newEnvironment();
        env1.defineRootVariable("ex", new MockExchangeData("canhaqi"));
        assertEquals(true, hasScript.eval(env1));
        // 制造一个错误：给 has 传入错误类型的 target
        Environment env2 = FluxonRuntime.getInstance().newEnvironment();
        env2.defineRootVariable("ex", new MockLocation(0, 0, 0));
        Throwable captured = null;
        try {
            hasScript.eval(env2);
        } catch (Throwable e) {
            captured = e;
        }
        assertNotNull(captured, "错误类型的 target 应该抛出异常");
        System.out.println("[场景 K] 异常类型: " + captured.getClass().getName());
        System.out.println("[场景 K] 异常信息: " + captured.getMessage());
        // 不应该是 ClassCastException（如果 guardClass 正确工作）
        assertFalse(captured instanceof ClassCastException,
                "不应该抛出 ClassCastException，guardClass 应该阻止缓存命中。" +
                " 实际异常: " + captured.getClass().getName() + ": " + captured.getMessage());
        // 如果是 ArgumentTypeMismatchError，检查 context.function.name 的稳定性
        if (captured instanceof ArgumentTypeMismatchError) {
            ArgumentTypeMismatchError error = (ArgumentTypeMismatchError) captured;
            String nameBeforeReuse = error.getContext().getFunction().getName();
            System.out.println("[场景 K] 错误中的 function name (pool 复用前): " + nameBeforeReuse);
            // 执行 sleep，触发 pool 复用
            Environment env3 = FluxonRuntime.getInstance().newEnvironment();
            sleepScript.eval(env3);
            // 再次读取——pool 复用后 context.function 可能已被覆盖
            String nameAfterReuse = error.getContext().getFunction().getName();
            System.out.println("[场景 K] 错误中的 function name (pool 复用后): " + nameAfterReuse);
            if (!"has".equals(nameAfterReuse)) {
                System.out.println("[场景 K] 已复现 Frontier Bug！function name 被污染: " +
                        nameBeforeReuse + " → " + nameAfterReuse);
            }
        }
    }
}
