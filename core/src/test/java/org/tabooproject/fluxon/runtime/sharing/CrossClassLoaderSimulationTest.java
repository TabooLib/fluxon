package org.tabooproject.fluxon.runtime.sharing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.runtime.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CrossClassLoaderSimulationTest {

    private final Set<String> registeredFunctionNames = ConcurrentHashMap.newKeySet();

    @AfterEach
    void cleanup() {
        SharedFunctionRegistry.unregisterAll("pluginA");
        SharedFunctionRegistry.unregisterAll("pluginB");
        SharedFunctionRegistry.unregisterAll("pluginC");
        // 清除并发测试注册的 owner
        for (int i = 0; i < 100; i++) {
            SharedFunctionRegistry.unregisterAll("concurrent-" + i);
        }
        // 清理导入到 runtime 的函数
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        for (String name : registeredFunctionNames) {
            OverloadSet set = runtime.getSystemFunctions().get(name);
            if (set != null) {
                for (Function f : set.getOverloads().toArray(new Function[0])) {
                    runtime.unregisterFunction(f);
                }
            }
        }
        registeredFunctionNames.clear();
    }

    /**
     * 导入共享函数并追踪名称以便 cleanup
     */
    private void importAndTrack(FluxonRuntime runtime, String owner, String name) {
        runtime.importSharedFunction(owner, name);
        registeredFunctionNames.add(name);
    }

    // =================== 同名函数不同 owner 隔离 ===================

    @Test
    void testSameNameDifferentOwnerIsolation() throws Throwable {
        MethodHandle mhDouble = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "calcDouble",
                MethodType.methodType(int.class, int.class));
        MethodHandle mhTriple = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "calcTriple",
                MethodType.methodType(int.class, int.class));
        // pluginA 的 calc 是 *2，pluginB 的 calc 是 *3
        SharedFunctionRegistry.register("pluginA", "calc", mhDouble);
        SharedFunctionRegistry.register("pluginB", "calc", mhTriple);
        // 各自独立查找
        Object[] entryA = SharedFunctionRegistry.find("pluginA", "calc");
        Object[] entryB = SharedFunctionRegistry.find("pluginB", "calc");
        assertNotNull(entryA);
        assertNotNull(entryB);
        assertEquals("pluginA", SharedFunctionEntry.owner(entryA));
        assertEquals("pluginB", SharedFunctionEntry.owner(entryB));
        // 通过 MethodHandle 直接验证行为不同
        assertEquals(10, (int) SharedFunctionEntry.handle(entryA).invoke(5));
        assertEquals(15, (int) SharedFunctionEntry.handle(entryB).invoke(5));
        // unregister pluginA 不影响 pluginB
        SharedFunctionRegistry.unregisterAll("pluginA");
        assertNull(SharedFunctionRegistry.find("pluginA", "calc"));
        assertNotNull(SharedFunctionRegistry.find("pluginB", "calc"));
    }

    // =================== 生命周期完整性 ===================

    @Test
    void testUnexportThenImportFails() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "add",
                MethodType.methodType(int.class, int.class, int.class));
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.setSharingIdentity("pluginA");
        runtime.exportFunction("lifecycle_fn", mh);
        assertNotNull(SharedFunctionRegistry.find("pluginA", "lifecycle_fn"));
        // 模拟插件卸载
        runtime.unexportAll();
        // 另一个插件尝试导入 — 应返回 false
        boolean imported = runtime.importSharedFunction("pluginA", "lifecycle_fn");
        assertFalse(imported);
    }

    @Test
    void testReExportAfterUnexport() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "add",
                MethodType.methodType(int.class, int.class, int.class));
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.setSharingIdentity("pluginA");
        runtime.exportFunction("reexport_fn", mh);
        runtime.unexportAll();
        assertNull(SharedFunctionRegistry.find("pluginA", "reexport_fn"));
        // 重新导出
        runtime.exportFunction("reexport_fn", mh);
        assertNotNull(SharedFunctionRegistry.find("pluginA", "reexport_fn"));
    }

    // =================== 高并发注册和查找 ===================

    @Test
    void testConcurrentRegisterAndFind() throws Exception {
        int threadCount = 20;
        int opsPerThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        MethodHandle mh = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "add",
                MethodType.methodType(int.class, int.class, int.class));
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);
        for (int t = 0; t < threadCount; t++) {
            int threadId = t;
            pool.submit(() -> {
                try {
                    String owner = "concurrent-" + threadId;
                    for (int i = 0; i < opsPerThread; i++) {
                        SharedFunctionRegistry.register(owner, "fn_" + i, mh);
                    }
                    // 验证自己注册的都能找到
                    for (int i = 0; i < opsPerThread; i++) {
                        Object[] entry = SharedFunctionRegistry.find(owner, "fn_" + i);
                        if (entry == null) errors.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(10, TimeUnit.SECONDS);
        pool.shutdown();
        assertEquals(0, errors.get(), "并发注册/查找出现错误");
        // 验证总注册数
        int total = 0;
        for (int t = 0; t < threadCount; t++) {
            total += SharedFunctionRegistry.unregisterAll("concurrent-" + t);
        }
        assertEquals(threadCount * opsPerThread, total);
    }

    // =================== 复杂返回类型 ===================

    @Test
    void testMapReturnType() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "buildConfig",
                MethodType.methodType(Map.class, String.class, int.class));
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.setSharingIdentity("pluginA");
        runtime.exportFunction("build_config", mh);
        importAndTrack(runtime, "pluginA", "build_config");
        Object result = Fluxon.eval("build_config(\"server\", 8080)");
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("server", map.get("name"));
        assertEquals(8080, map.get("port"));
    }

    @Test
    void testListReturnType() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "generateSequence",
                MethodType.methodType(List.class, int.class));
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.setSharingIdentity("pluginB");
        runtime.exportFunction("gen_seq", mh);
        importAndTrack(runtime, "pluginB", "gen_seq");
        Object result = Fluxon.eval("gen_seq(5)");
        assertTrue(result instanceof List);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), result);
    }

    @Test
    void testNullReturnValue() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "returnsNull",
                MethodType.methodType(Object.class));
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.setSharingIdentity("pluginA");
        runtime.exportFunction("get_null", mh);
        importAndTrack(runtime, "pluginA", "get_null");
        Object result = Fluxon.eval("get_null()");
        assertNull(result);
    }

    // =================== 多参数类型混合 ===================

    @Test
    void testMixedParameterTypes() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "formatEntry",
                MethodType.methodType(String.class, String.class, int.class, double.class, boolean.class));
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.setSharingIdentity("pluginA");
        runtime.exportFunction("fmt_entry", mh);
        importAndTrack(runtime, "pluginA", "fmt_entry");
        Object result = Fluxon.eval("fmt_entry(\"item\", 42, 3.14, true)");
        assertEquals("item:42:3.14:true", result);
    }

    @Test
    void testLongParameterAndReturn() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "largeLong",
                MethodType.methodType(long.class, long.class));
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.setSharingIdentity("pluginA");
        runtime.exportFunction("large_long", mh);
        importAndTrack(runtime, "pluginA", "large_long");
        Object result = Fluxon.eval("large_long(1000000000000)");
        // 1_000_000_000_000 * 2 = 2_000_000_000_000
        assertEquals(2000000000000L, result);
    }

    @Test
    void testBooleanReturnType() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "isPositive",
                MethodType.methodType(boolean.class, int.class));
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.setSharingIdentity("pluginA");
        runtime.exportFunction("is_positive", mh);
        importAndTrack(runtime, "pluginA", "is_positive");
        // true case
        Object resultTrue = Fluxon.eval("is_positive(42)");
        assertEquals(true, resultTrue instanceof Boolean ? resultTrue : ((Number) resultTrue).intValue() != 0);
        // false case
        Object resultFalse = Fluxon.eval("is_positive(-1)");
        if (resultFalse instanceof Boolean) {
            assertFalse((Boolean) resultFalse);
        } else {
            assertEquals(0, ((Number) resultFalse).intValue());
        }
    }

    // =================== 异常传播 ===================

    @Test
    void testExceptionPropagation() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "throwingFunction",
                MethodType.methodType(void.class, String.class));
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.setSharingIdentity("pluginA");
        runtime.exportFunction("throw_fn", mh);
        importAndTrack(runtime, "pluginA", "throw_fn");
        // 异常应该传播到调用方
        try {
            Fluxon.eval("throw_fn(\"boom\")");
            fail("Expected exception to propagate");
        } catch (Exception e) {
            // 验证异常链中包含原始消息
            String fullMessage = getFullExceptionChain(e);
            assertTrue(fullMessage.contains("boom"), "异常消息应包含原始信息: " + fullMessage);
        }
    }

    @Test
    void testCheckedExceptionWrappedAsRuntime() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "throwsChecked",
                MethodType.methodType(void.class));
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.setSharingIdentity("pluginA");
        runtime.exportFunction("throw_checked", mh);
        importAndTrack(runtime, "pluginA", "throw_checked");
        try {
            Fluxon.eval("throw_checked()");
            fail("Expected exception");
        } catch (Exception e) {
            // checked exception 应被包装为 RuntimeException
            String chain = getFullExceptionChain(e);
            assertTrue(chain.contains("checked exception"), "应包含原始 checked 异常消息: " + chain);
        }
    }

    // =================== exportRegisteredFunction 快照绑定 ===================

    @Test
    void testExportRegisteredFunctionBindsSnapshot() throws Throwable {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.setSharingIdentity("pluginA");
        // 注册初始版本
        AtomicInteger version = new AtomicInteger(1);
        runtime.registerFunction("snapshot_fn",
                FunctionSignature.returns(Type.I).noParams(),
                context -> context.setReturnInt(version.get()));
        registeredFunctionNames.add("snapshot_fn");
        runtime.exportRegisteredFunction("snapshot_fn");
        // 导出后修改版本号
        version.set(999);
        // 通过共享注册表调用 — 应该通过桥接调用原函数，而原函数引用了 version
        Object[] entry = SharedFunctionRegistry.find("pluginA", "snapshot_fn");
        assertNotNull(entry);
        Object result = SharedFunctionEntry.handle(entry).invoke();
        // MethodHandle 绑定的是 Function 实例（lambda 捕获了 version 引用），
        // 所以修改 version 后调用应得到新值
        assertEquals(999, result);
    }

    // =================== requireSharingIdentity ===================

    @Test
    void testExportWithoutIdentityThrows() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "add",
                MethodType.methodType(int.class, int.class, int.class));
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.setSharingIdentity(null); // 清除 identity
        assertThrows(IllegalStateException.class, () -> runtime.exportFunction("fn", mh));
    }

    @Test
    void testExportRegisteredWithoutIdentityThrows() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.setSharingIdentity(null);
        runtime.registerFunction("orphan_fn",
                FunctionSignature.returnsObject().noParams(),
                context -> context.setReturnRef("ok"));
        registeredFunctionNames.add("orphan_fn");
        assertThrows(IllegalStateException.class, () -> runtime.exportRegisteredFunction("orphan_fn"));
    }

    // =================== findAll 跨 owner + 扩展函数混合 ===================

    @Test
    void testFindAllMixedOwnersAndTypes() throws Exception {
        MethodHandle mhAdd = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "add",
                MethodType.methodType(int.class, int.class, int.class));
        MethodHandle mhLen = MethodHandles.lookup().findVirtual(
                String.class, "length", MethodType.methodType(int.class));
        // 同名 "shared_util" 注册为：pluginA 普通函数 + pluginB 普通函数 + pluginC 扩展函数
        SharedFunctionRegistry.register("pluginA", "shared_util", mhAdd);
        SharedFunctionRegistry.register("pluginB", "shared_util", mhAdd);
        SharedFunctionRegistry.registerExtension("pluginC", "shared_util", mhLen, String.class);
        List<Object[]> all = SharedFunctionRegistry.findAll("shared_util");
        assertEquals(3, all.size());
        // 验证 owner 去重
        Set<String> owners = new HashSet<>();
        for (Object[] e : all) owners.add(SharedFunctionEntry.owner(e));
        assertEquals(3, owners.size());
        assertTrue(owners.containsAll(Arrays.asList("pluginA", "pluginB", "pluginC")));
        // 验证有一个是扩展函数
        long extensionCount = all.stream().filter(SharedFunctionEntry::isExtension).count();
        assertEquals(1, extensionCount);
    }

    @Test
    void testGetOwnersReturnsDistinct() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "add",
                MethodType.methodType(int.class, int.class, int.class));
        // pluginA 注册多个函数
        SharedFunctionRegistry.register("pluginA", "f1", mh);
        SharedFunctionRegistry.register("pluginA", "f2", mh);
        SharedFunctionRegistry.register("pluginA", "f3", mh);
        SharedFunctionRegistry.register("pluginB", "f1", mh);
        List<String> owners = SharedFunctionRegistry.getOwners();
        // pluginA 只出现一次
        assertEquals(1, owners.stream().filter(o -> o.equals("pluginA")).count());
        assertEquals(1, owners.stream().filter(o -> o.equals("pluginB")).count());
    }

    // =================== 扩展函数通过 Fluxon.eval 调用 ===================

    @Test
    void testImportExtensionAndCallViaContext() throws Throwable {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "reverseString",
                MethodType.methodType(String.class, String.class));
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.setSharingIdentity("pluginA");
        runtime.exportExtensionFunction("reverse_ext", mh, String.class);
        // 导入扩展函数到本地 runtime
        Object[] entry = SharedFunctionRegistry.findExtension("pluginA", "reverse_ext", String.class);
        assertNotNull(entry);
        NativeFunction<?> adapted = SharedFunctionAdapter.adapt(entry);
        runtime.registerExtensionFunction(String.class, adapted);
        // 验证扩展函数已注册到 runtime 的 extensionFunctions map
        Map<Class<?>, OverloadSet> classMap = runtime.getExtensionFunctions().get("reverse_ext");
        assertNotNull(classMap, "reverse_ext should be in extensionFunctions");
        OverloadSet overloads = classMap.get(String.class);
        assertNotNull(overloads, "reverse_ext should have String.class binding");
        // 通过 FunctionContext 直接调用验证
        FunctionContextPool pool = FunctionContextPool.local();
        Environment env = runtime.newEnvironment();
        try (FunctionContext<?> ctx = pool.borrow(adapted, "hello", new Object[0], env)) {
            adapted.call(ctx);
            assertEquals("olleh", ctx.getReturnRef());
        }
        // 也通过 MethodHandle 直接验证
        assertEquals("olleh", (String) SharedFunctionEntry.handle(entry).invoke("hello"));
        // 清理
        runtime.unregisterExtensionFunction(String.class, "reverse_ext", adapted);
    }

    // =================== importAllSharedFunctions ===================

    @Test
    void testImportAllSharedFunctions() throws Exception {
        MethodHandle mhAdd = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "add",
                MethodType.methodType(int.class, int.class, int.class));
        MethodHandle mhMul = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "multiply",
                MethodType.methodType(double.class, double.class, double.class));
        SharedFunctionRegistry.register("pluginA", "batch_add", mhAdd);
        SharedFunctionRegistry.register("pluginA", "batch_mul", mhMul);
        registeredFunctionNames.add("batch_add");
        registeredFunctionNames.add("batch_mul");
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        int count = runtime.importAllSharedFunctions("pluginA");
        assertEquals(2, count);
        assertEquals(30, Fluxon.eval("batch_add(10, 20)"));
        assertEquals(10.0, ((Number) Fluxon.eval("batch_mul(2.5, 4.0)")).doubleValue(), 0.001);
    }

    @Test
    void testImportAllFromNonExistentOwnerReturnsZero() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        int count = runtime.importAllSharedFunctions("nonexistent_plugin");
        assertEquals(0, count);
    }

    // =================== 版本安全 ===================

    @Test
    void testUnsupportedVersionReturnsStubbedFunction() {
        // 模拟未来版本 entry
        Object[] futureEntry = new Object[7];
        futureEntry[SharedFunctionEntry.IDX_VERSION] = 999;
        futureEntry[SharedFunctionEntry.IDX_NAME] = "future_func";
        futureEntry[SharedFunctionEntry.IDX_OWNER] = "futurePlugin";
        futureEntry[SharedFunctionEntry.IDX_HANDLE] = null;
        futureEntry[SharedFunctionEntry.IDX_IS_EXTENSION] = false;
        futureEntry[SharedFunctionEntry.IDX_EXTENSION_TARGET] = null;
        futureEntry[6] = "unknown_field";
        Function adapted = SharedFunctionAdapter.adapt(futureEntry);
        assertNotNull(adapted);
        assertEquals("future_func", adapted.getName());
        assertThrows(UnsupportedOperationException.class, () -> {
            FunctionContextPool pool = FunctionContextPool.local();
            Environment env = FluxonRuntime.getInstance().newEnvironment();
            try (FunctionContext<?> ctx = pool.borrow(adapted, null, new Object[]{1, 2}, env)) {
                adapted.call(ctx);
            }
        });
    }

    @Test
    void testVersionBoundaryCheck() {
        // v1 是支持的最高版本
        Object[] v1Entry = SharedFunctionEntry.create("f", "o", null, false, null);
        assertTrue(SharedFunctionEntry.isVersionSupported(v1Entry));
        // v0 不支持
        Object[] v0Entry = new Object[6];
        v0Entry[SharedFunctionEntry.IDX_VERSION] = 0;
        assertFalse(SharedFunctionEntry.isVersionSupported(v0Entry));
        // v2 不支持（尚未定义）
        Object[] v2Entry = new Object[6];
        v2Entry[SharedFunctionEntry.IDX_VERSION] = 2;
        assertFalse(SharedFunctionEntry.isVersionSupported(v2Entry));
    }

    // =================== 覆盖注册 ===================

    @Test
    void testOverwriteExistingRegistration() throws Throwable {
        MethodHandle mhAdd = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "add",
                MethodType.methodType(int.class, int.class, int.class));
        MethodHandle mhMul = MethodHandles.lookup().findStatic(
                CrossClassLoaderSimulationTest.class, "multiply",
                MethodType.methodType(double.class, double.class, double.class));
        // 先注册 add，然后用 multiply 覆盖同一个 key
        SharedFunctionRegistry.register("pluginA", "overwrite_fn", mhAdd);
        SharedFunctionRegistry.register("pluginA", "overwrite_fn", mhMul);
        Object[] entry = SharedFunctionRegistry.find("pluginA", "overwrite_fn");
        assertNotNull(entry);
        // 应该是 multiply 的行为
        Object result = SharedFunctionEntry.handle(entry).invoke(3.0, 4.0);
        assertEquals(12.0, (double) result, 0.001);
    }

    // =================== 辅助方法 ===================

    public static int add(int a, int b) {
        return a + b;
    }

    public static double multiply(double a, double b) {
        return a * b;
    }

    public static int calcDouble(int x) {
        return x * 2;
    }

    public static int calcTriple(int x) {
        return x * 3;
    }

    public static Map<String, Object> buildConfig(String name, int port) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("port", port);
        return map;
    }

    public static List<Integer> generateSequence(int n) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(i);
        return list;
    }

    public static Object returnsNull() {
        return null;
    }

    public static String formatEntry(String name, int id, double value, boolean flag) {
        return name + ":" + id + ":" + value + ":" + flag;
    }

    public static long largeLong(long x) {
        return x * 2;
    }

    public static boolean isPositive(int x) {
        return x > 0;
    }

    public static void throwingFunction(String msg) {
        throw new IllegalArgumentException(msg);
    }

    public static void throwsChecked() throws Exception {
        throw new Exception("checked exception");
    }

    public static String reverseString(String s) {
        return new StringBuilder(s).reverse().toString();
    }

    private String getFullExceptionChain(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable current = e;
        while (current != null) {
            sb.append(current.getClass().getSimpleName()).append(": ").append(current.getMessage()).append(" -> ");
            current = current.getCause();
        }
        return sb.toString();
    }
}
