package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 Environment 的数组缓存优化和脏标记策略
 */
public class EnvironmentCacheTest {

    @Test
    public void testSystemFunctionsArrayCached() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();

        // 创建两个环境
        Environment env1 = runtime.newEnvironment();
        Environment env2 = runtime.newEnvironment();

        // 获取它们的 systemFunctions 数组
        Function[] systemFunctions1 = env1.getRootSystemFunctions();
        Function[] systemFunctions2 = env2.getRootSystemFunctions();

        // 验证它们引用的是同一个数组对象（而不是拷贝）
        assertSame(systemFunctions1, systemFunctions2,
                "systemFunctions 数组应该被缓存，所有环境共享同一个实例");

        System.out.println("✓ systemFunctions 数组缓存验证通过");
        System.out.println("  数组大小: " + systemFunctions1.length);
    }

    @Test
    public void testSystemExtensionFunctionsArrayCached() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();

        // 创建两个环境
        Environment env1 = runtime.newEnvironment();
        Environment env2 = runtime.newEnvironment();

        // 获取它们的 systemExtensionFunctions 数组
        Object[][] systemExtensionFunctions1 = env1.getRootSystemExtensionFunctions();
        Object[][] systemExtensionFunctions2 = env2.getRootSystemExtensionFunctions();

        // 验证它们引用的是同一个数组对象（而不是拷贝）
        assertSame(systemExtensionFunctions1, systemExtensionFunctions2,
                "systemExtensionFunctions 数组应该被缓存，所有环境共享同一个实例");

        System.out.println("✓ systemExtensionFunctions 数组缓存验证通过");
        System.out.println("  数组大小: " + systemExtensionFunctions1.length);
    }

    @Test
    public void testDirtyFlagAfterRegisterFunction() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();

        // 创建第一个环境
        Environment env1 = runtime.newEnvironment();
        Function[] functions1 = env1.getRootSystemFunctions();
        int initialCount = functions1.length;

        // 注册一个新函数（会触发 dirty 标记）
        runtime.registerFunction("testFunc", 0, ctx -> "test");

        // 创建第二个环境（应该触发重新构建缓存）
        Environment env2 = runtime.newEnvironment();
        Function[] functions2 = env2.getRootSystemFunctions();

        // 验证数组已经更新
        assertEquals(initialCount + 1, functions2.length,
                "注册新函数后，新环境应该包含更新后的函数数组");

        // 验证新旧数组不是同一个对象（因为重新构建了）
        assertNotSame(functions1, functions2,
                "注册新函数后，缓存数组应该重新构建");

        System.out.println("✓ 脏标记策略验证通过");
        System.out.println("  初始函数数: " + initialCount);
        System.out.println("  注册后函数数: " + functions2.length);
    }

    @Test
    public void testCacheRebuiltOnlyOnce() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();

        // 注册一个新函数
        runtime.registerFunction("testFunc2", 0, ctx -> "test2");

        // 创建第一个环境（会触发缓存重建）
        Environment env1 = runtime.newEnvironment();
        Function[] functions1 = env1.getRootSystemFunctions();

        // 创建第二个环境（不应该触发缓存重建）
        Environment env2 = runtime.newEnvironment();
        Function[] functions2 = env2.getRootSystemFunctions();

        // 验证两个环境共享同一个缓存数组
        assertSame(functions1, functions2,
                "缓存重建后，后续环境应该共享相同的缓存数组");

        System.out.println("✓ 缓存只重建一次验证通过");
    }

    @Test
    public void testExtensionFunctionDirtyFlag() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();

        // 创建第一个环境
        Environment env1 = runtime.newEnvironment();
        Object[][] extensionFunctions1 = env1.getRootSystemExtensionFunctions();
        int initialCount = extensionFunctions1.length;

        // 注册一个新的扩展函数
        runtime.registerExtensionFunction(String.class, "testExt", 0, ctx -> "test");

        // 创建第二个环境
        Environment env2 = runtime.newEnvironment();
        Object[][] extensionFunctions2 = env2.getRootSystemExtensionFunctions();

        // 验证扩展函数数组已更新
        assertTrue(extensionFunctions2.length >= initialCount,
                "注册新扩展函数后，数组应该更新");

        // 验证数组已重新构建
        assertNotSame(extensionFunctions1, extensionFunctions2,
                "注册新扩展函数后，缓存数组应该重新构建");

        System.out.println("✓ 扩展函数脏标记验证通过");
        System.out.println("  初始扩展函数组数: " + initialCount);
        System.out.println("  注册后扩展函数组数: " + extensionFunctions2.length);
    }

    @Test
    public void testRootVariablesNotShared() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();

        // 创建两个环境
        Environment env1 = runtime.newEnvironment();
        Environment env2 = runtime.newEnvironment();

        // 在 env1 中定义一个变量
        env1.defineRootVariable("testVar", "value1");

        // 验证 env2 中没有这个变量
        assertFalse(env2.has("testVar", -1),
                "不同环境的根变量应该是独立的");

        System.out.println("✓ 根变量隔离验证通过");
    }

    @Test
    public void testFunctionMapNotShared() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();

        // 创建两个环境
        Environment env1 = runtime.newEnvironment();
        Environment env2 = runtime.newEnvironment();

        // 在 env1 中定义一个函数
        Function testFunc = new NativeFunction<>(null, null, ctx -> "test");
        env1.defineRootFunction("testFunc11", testFunc);

        // 验证 env2 中没有这个函数
        assertNull(env2.getFunctionOrNull("testFunc11"),
                "不同环境的函数映射应该是独立的");

        System.out.println("✓ 函数映射隔离验证通过");
    }

    @Test
    public void testPerformanceImprovement() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();

        int iterations = 10000;

        // 测试创建环境的性能
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            runtime.newEnvironment();
        }
        long endTime = System.nanoTime();

        long avgTimeNanos = (endTime - startTime) / iterations;
        double avgTimeMicros = avgTimeNanos / 1000.0;

        System.out.println("✓ 性能测试完成");
        System.out.printf("  创建 %d 个环境，平均耗时: %.2f μs%n", iterations, avgTimeMicros);

        // 验证性能合理（应该很快，因为不需要转换数组）
        assertTrue(avgTimeMicros < 100,
                "创建环境应该很快（< 100μs），因为数组已经被缓存");
    }
}

