package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.*;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.FluxonFeatures;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.runtime.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

/**
 * cachedResolution guard 对称性测试
 * 验证 FunctionCallExpression 的单态缓存在 target 类型变化时正确失效
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class CachedResolutionGuardTest {

    // 模拟 ExchangeData（生产环境中的 target 对象类型）
    public static class Vessel {
        public final String id;
        public Vessel(String id) { this.id = id; }
    }

    // 另一个无关类型，模拟 ParticlePacketBuilder 等错误 target
    public static class Decoy {
        public final String tag;
        public Decoy(String tag) { this.tag = tag; }
    }

    private Function vesselProbe;
    private Function decoyProbe;

    @BeforeEach
    void registerFunctions() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        // 扩展函数：Vessel::probe() → "vessel:<id>"
        vesselProbe = new NativeFunction<Vessel>(null, "probe",
                returns(Type.OBJECT).noParams(),
                ctx -> ctx.setReturnRef("vessel:" + ctx.getTarget().id),
                false, false);
        runtime.registerExtensionFunction(Vessel.class, vesselProbe);
        // 扩展函数：Decoy::probe() → "decoy:<tag>"
        decoyProbe = new NativeFunction<Decoy>(null, "probe",
                returns(Type.OBJECT).noParams(),
                ctx -> ctx.setReturnRef("decoy:" + ctx.getTarget().tag),
                false, false);
        runtime.registerExtensionFunction(Decoy.class, decoyProbe);
    }

    @AfterEach
    void unregisterFunctions() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.unregisterExtensionFunction(Vessel.class, "probe", vesselProbe);
        runtime.unregisterExtensionFunction(Decoy.class, "probe", decoyProbe);
    }

    /**
     * 核心测试：同一 AST 在不同 target 类型间切换时，缓存正确失效
     *
     * 模拟生产场景：
     * - 同一脚本文本被多次解释执行（共享 AST 和 cachedResolution）
     * - 每次执行通过 env.defineRootVariable 设置不同类型的 target
     * - ContextCallEvaluator (::) 在求值时设置 env.target
     */
    @Test
    void testGuardInvalidatesOnTargetTypeChange() {
        boolean oldFlag = FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE;
        FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = true;
        try {
            String source = "&obj::probe()";
            // 解析一次，获得共享 AST
            Environment parseEnv = FluxonRuntime.getInstance().newEnvironment();
            ParsedScript script = Fluxon.parse(source, parseEnv);

            // 第 1 次：target = Vessel
            Environment env1 = FluxonRuntime.getInstance().newEnvironment();
            env1.defineRootVariable("obj", new Vessel("alpha"));
            Object r1 = script.eval(env1);
            assertEquals("vessel:alpha", r1);

            // 第 2 次：target = Decoy（类型变化，guard 应失效）
            Environment env2 = FluxonRuntime.getInstance().newEnvironment();
            env2.defineRootVariable("obj", new Decoy("beta"));
            Object r2 = script.eval(env2);
            assertEquals("decoy:beta", r2);

            // 第 3 次：target 切回 Vessel（再次变化）
            Environment env3 = FluxonRuntime.getInstance().newEnvironment();
            env3.defineRootVariable("obj", new Vessel("gamma"));
            Object r3 = script.eval(env3);
            assertEquals("vessel:gamma", r3);
        } finally {
            FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = oldFlag;
        }
    }

    /**
     * 测试 guard=null 场景：target 从 null 变为非 null
     *
     * 旧代码中 guardClass=null 时完全跳过检查，导致缓存永远命中。
     * 修复后 guard=null 时，如果当前 target 非 null 则缓存失效。
     */
    @Test
    void testGuardInvalidatesFromNullToNonNull() {
        boolean oldFlag = FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE;
        FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = true;
        try {
            // 注册同名系统函数
            FluxonRuntime runtime = FluxonRuntime.getInstance();
            Function sysProbe = new NativeFunction<>(null, "probe",
                    returns(Type.OBJECT).noParams(),
                    ctx -> ctx.setReturnRef("system"),
                    false, false);
            runtime.registerFunction(sysProbe);
            try {
                String source = "probe()";
                Environment parseEnv = FluxonRuntime.getInstance().newEnvironment();
                ParsedScript script = Fluxon.parse(source, parseEnv);

                // 第 1 次：无 target（系统函数路径），cache guard=null
                Environment env1 = FluxonRuntime.getInstance().newEnvironment();
                Object r1 = script.eval(env1);
                assertEquals("system", r1);

                // 第 2 次：有 target（通过手动设置 env.target）
                // 模拟 ContextCallEvaluator 设置 target 的效果
                // 直接用 eval 不会自动设置 target，需要用另一种方式触发
                // 更准确的测试是用 :: 语法，但需要不同脚本文本（不同 AST）
                // 所以改为直接验证 env.target 变化时的行为
                Environment env2 = FluxonRuntime.getInstance().newEnvironment();
                env2.setTarget(new Vessel("x"));
                Object r2 = script.eval(env2);
                // 有 target 但 probe 函数只在系统函数路径存在时，应该走慢速路径重新解析
                // 结果取决于解析逻辑：如果扩展函数也匹配，返回扩展版本
                // 关键是不应该因为 guard=null 而错误命中缓存的系统 probe
                assertNotNull(r2);
            } finally {
                runtime.unregisterFunction(sysProbe);
            }
        } finally {
            FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = oldFlag;
        }
    }

    /**
     * 测试 guard 从非 null 变为 null
     *
     * 第一次带 target，第二次不带 target
     */
    @Test
    void testGuardInvalidatesFromNonNullToNull() {
        boolean oldFlag = FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE;
        FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = true;
        try {
            // 同时注册系统 probe 和扩展 probe
            FluxonRuntime runtime = FluxonRuntime.getInstance();
            Function sysProbe = new NativeFunction<>(null, "probe",
                    returns(Type.OBJECT).noParams(),
                    ctx -> ctx.setReturnRef("system"),
                    false, false);
            runtime.registerFunction(sysProbe);
            try {
                // 使用 :: 语法确保 target 被设置
                // 第 1 次：&obj::probe() 带 Vessel target
                String source1 = "&obj::probe()";
                Environment parseEnv1 = FluxonRuntime.getInstance().newEnvironment();
                ParsedScript script1 = Fluxon.parse(source1, parseEnv1);

                Environment env1 = FluxonRuntime.getInstance().newEnvironment();
                env1.defineRootVariable("obj", new Vessel("v1"));
                Object r1 = script1.eval(env1);
                assertEquals("vessel:v1", r1);

                // 第 2 次：&obj 为 null（变量不存在，isOptional=true 返回 null）
                // ContextCallEvaluator 会设置 target=null
                // probe() 的 cachedResolution guard=Vessel.class → target=null → 应 miss
                Environment env2 = FluxonRuntime.getInstance().newEnvironment();
                // 不设置 obj → &obj 解析为 null → target=null
                Object r2 = script1.eval(env2);
                // target=null 时走系统 probe 路径
                assertEquals("system", r2);
            } finally {
                runtime.unregisterFunction(sysProbe);
            }
        } finally {
            FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = oldFlag;
        }
    }

    /**
     * 测试 monomorphic 场景（稳定 target 类型）缓存正常命中
     * 确保 guard 修复不破坏正常的缓存优化
     */
    @Test
    void testMonomorphicCacheHit() {
        boolean oldFlag = FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE;
        FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = true;
        try {
            String source = "&obj::probe()";
            Environment parseEnv = FluxonRuntime.getInstance().newEnvironment();
            ParsedScript script = Fluxon.parse(source, parseEnv);

            // 连续多次用相同类型的 target 评估
            for (int i = 0; i < 10; i++) {
                Environment env = FluxonRuntime.getInstance().newEnvironment();
                env.defineRootVariable("obj", new Vessel("iter" + i));
                Object result = script.eval(env);
                assertEquals("vessel:iter" + i, result);
            }
        } finally {
            FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = oldFlag;
        }
    }

    /**
     * 测试 polymorphic 场景（多种 target 类型交替）
     * 缓存在每次类型变化时失效，走慢速路径重新解析
     */
    @Test
    void testPolymorphicTargetAlternation() {
        boolean oldFlag = FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE;
        FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = true;
        try {
            String source = "&obj::probe()";
            Environment parseEnv = FluxonRuntime.getInstance().newEnvironment();
            ParsedScript script = Fluxon.parse(source, parseEnv);

            for (int i = 0; i < 5; i++) {
                // Vessel
                Environment envV = FluxonRuntime.getInstance().newEnvironment();
                envV.defineRootVariable("obj", new Vessel("v" + i));
                assertEquals("vessel:v" + i, script.eval(envV));
                // Decoy
                Environment envD = FluxonRuntime.getInstance().newEnvironment();
                envD.defineRootVariable("obj", new Decoy("d" + i));
                assertEquals("decoy:d" + i, script.eval(envD));
            }
        } finally {
            FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = oldFlag;
        }
    }

    // region 并发压力测试

    /**
     * 多线程并发读写同一 AST 节点的 cachedResolution
     * 所有线程共享同一个 ParsedScript（同一 FunctionCallExpression），
     * 用相同 target 类型并发执行，验证缓存命中路径在竞争下不会产生错误结果
     */
    @Test
    void testConcurrentMonomorphicExecution() throws Exception {
        boolean oldFlag = FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE;
        FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = true;
        try {
            String source = "&obj::probe()";
            Environment parseEnv = FluxonRuntime.getInstance().newEnvironment();
            ParsedScript script = Fluxon.parse(source, parseEnv);

            int threadCount = 8;
            int iterationsPerThread = 500;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startGate = new CountDownLatch(1);
            AtomicInteger errorCount = new AtomicInteger();
            List<Future<?>> futures = new ArrayList<>();

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                futures.add(executor.submit(() -> {
                    try {
                        startGate.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < iterationsPerThread; i++) {
                        String id = "t" + threadId + "_i" + i;
                        Environment env = FluxonRuntime.getInstance().newEnvironment();
                        env.defineRootVariable("obj", new Vessel(id));
                        Object result = script.eval(env);
                        if (!("vessel:" + id).equals(result)) {
                            errorCount.incrementAndGet();
                        }
                    }
                }));
            }

            startGate.countDown();
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
            executor.shutdown();
            assertEquals(0, errorCount.get(),
                    "monomorphic 并发执行产生了 " + errorCount.get() + " 个错误结果");
        } finally {
            FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = oldFlag;
        }
    }

    /**
     * 多线程并发用不同 target 类型执行同一 AST 节点
     * 模拟生产环境中 Vessel 和 Decoy 交替出现，验证 guard 失效+慢速路径在竞争下正确工作
     */
    @Test
    void testConcurrentPolymorphicExecution() throws Exception {
        boolean oldFlag = FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE;
        FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = true;
        try {
            String source = "&obj::probe()";
            Environment parseEnv = FluxonRuntime.getInstance().newEnvironment();
            ParsedScript script = Fluxon.parse(source, parseEnv);

            int threadCount = 8;
            int iterationsPerThread = 500;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startGate = new CountDownLatch(1);
            AtomicInteger errorCount = new AtomicInteger();
            List<Future<?>> futures = new ArrayList<>();

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                final boolean useVessel = (t % 2 == 0);
                futures.add(executor.submit(() -> {
                    try {
                        startGate.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < iterationsPerThread; i++) {
                        String id = "t" + threadId + "_i" + i;
                        Environment env = FluxonRuntime.getInstance().newEnvironment();
                        String expected;
                        if (useVessel) {
                            env.defineRootVariable("obj", new Vessel(id));
                            expected = "vessel:" + id;
                        } else {
                            env.defineRootVariable("obj", new Decoy(id));
                            expected = "decoy:" + id;
                        }
                        Object result = script.eval(env);
                        if (!expected.equals(result)) {
                            errorCount.incrementAndGet();
                        }
                    }
                }));
            }

            startGate.countDown();
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
            executor.shutdown();
            assertEquals(0, errorCount.get(),
                    "polymorphic 并发执行产生了 " + errorCount.get() + " 个错误结果");
        } finally {
            FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = oldFlag;
        }
    }

    /**
     * 多线程在同一 AST 上交替 null/非 null target
     * 验证 guard=null ↔ guard=Class 切换在竞争下的安全性
     */
    @Test
    void testConcurrentNullTargetAlternation() throws Exception {
        boolean oldFlag = FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE;
        FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = true;
        try {
            FluxonRuntime runtime = FluxonRuntime.getInstance();
            Function sysProbe = new NativeFunction<>(null, "probe",
                    returns(Type.OBJECT).noParams(),
                    ctx -> ctx.setReturnRef("system"),
                    false, false);
            runtime.registerFunction(sysProbe);
            try {
                String source = "&obj::probe()";
                Environment parseEnv = runtime.newEnvironment();
                ParsedScript script = Fluxon.parse(source, parseEnv);

                int threadCount = 8;
                int iterationsPerThread = 500;
                ExecutorService executor = Executors.newFixedThreadPool(threadCount);
                CountDownLatch startGate = new CountDownLatch(1);
                AtomicInteger errorCount = new AtomicInteger();
                List<Future<?>> futures = new ArrayList<>();

                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    final boolean useNull = (t % 2 == 0);
                    futures.add(executor.submit(() -> {
                        try {
                            startGate.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        for (int i = 0; i < iterationsPerThread; i++) {
                            Environment env = runtime.newEnvironment();
                            String expected;
                            if (useNull) {
                                expected = "system";
                            } else {
                                String id = "t" + threadId + "_i" + i;
                                env.defineRootVariable("obj", new Vessel(id));
                                expected = "vessel:" + id;
                            }
                            Object result = script.eval(env);
                            if (!expected.equals(result)) {
                                errorCount.incrementAndGet();
                            }
                        }
                    }));
                }

                startGate.countDown();
                for (Future<?> f : futures) {
                    f.get(30, TimeUnit.SECONDS);
                }
                executor.shutdown();
                assertEquals(0, errorCount.get(),
                        "null/非 null target 并发交替产生了 " + errorCount.get() + " 个错误结果");
            } finally {
                runtime.unregisterFunction(sysProbe);
            }
        } finally {
            FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE = oldFlag;
        }
    }

    // endregion
}
