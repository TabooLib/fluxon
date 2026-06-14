package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 ExtensionDispatchTable 的扩展函数派发优化
 */
public class ExtensionDispatchTableTest {

    private FluxonRuntime runtime;

    @BeforeEach
    void setUp() {
        runtime = FluxonRuntime.getInstance();
    }

    @Test
    void testExactMatchForString() {
        Environment env = runtime.newEnvironment();

        // String 类型应该精确匹配到 String 的扩展函数
        Function func = env.getExtensionFunctionOrNull(String.class, getExtensionIndex("length"), 0);
        assertNotNull(func, "String.length 扩展函数应该存在");
    }

    @Test
    void testExactMatchForArrayList() {
        Environment env = runtime.newEnvironment();

        // ArrayList 应该匹配到 List 或 Collection 的扩展函数
        Function func = env.getExtensionFunctionOrNull(ArrayList.class, getExtensionIndex("size"), 0);
        assertNotNull(func, "ArrayList.size 扩展函数应该存在");
    }

    @Test
    void testAssignableMatchForSubclass() {
        Environment env = runtime.newEnvironment();

        // 自定义子类应该能匹配到父类的扩展函数
        // LinkedHashMap 是 Map 的实现，应该匹配到 Map 的扩展函数
        Function func = env.getExtensionFunctionOrNull(LinkedHashMap.class, getExtensionIndex("keySet"), 0);
        assertNotNull(func, "LinkedHashMap 应该匹配到 Map.keySet 扩展函数");
    }

    @Test
    void testCacheStabilityForSameClass() {
        Environment env = runtime.newEnvironment();
        int index = getExtensionIndex("length");

        // 对同一目标类型多次调用，应该返回相同的函数实例
        Function func1 = env.getExtensionFunctionOrNull(String.class, index, 0);
        Function func2 = env.getExtensionFunctionOrNull(String.class, index, 0);
        Function func3 = env.getExtensionFunctionOrNull(String.class, index, 0);

        assertSame(func1, func2, "多次调用应该返回相同的函数实例");
        assertSame(func2, func3, "多次调用应该返回相同的函数实例");
    }

    @Test
    void testCacheStabilityForAssignableClass() {
        Environment env = runtime.newEnvironment();
        int index = getExtensionIndex("size");

        // 对可赋值匹配的类型多次调用，应该返回相同的函数实例
        Function func1 = env.getExtensionFunctionOrNull(ArrayList.class, index, 0);
        Function func2 = env.getExtensionFunctionOrNull(ArrayList.class, index, 0);

        assertSame(func1, func2, "可赋值匹配的缓存应该稳定");
    }

    @Test
    void testDispatchTablesCreated() {
        Environment env = runtime.newEnvironment();
        ExtensionDispatchTable[] tables = env.getRootDispatchTables();
        
        assertNotNull(tables, "派发表数组不应该为空");
        assertTrue(tables.length > 0, "派发表数组应该包含至少一个派发表");
    }

    @Test
    void testDispatchTablesCachedAcrossEnvironments() {
        Environment env1 = runtime.newEnvironment();
        Environment env2 = runtime.newEnvironment();
        
        ExtensionDispatchTable[] tables1 = env1.getRootDispatchTables();
        ExtensionDispatchTable[] tables2 = env2.getRootDispatchTables();
        
        assertSame(tables1, tables2, "派发表数组应该在多个环境间共享");
    }

    @Test
    void testSingleCandidatePath() {
        // 测试单候选快速路径
        Environment env = runtime.newEnvironment();
        ExtensionDispatchTable[] tables = env.getRootDispatchTables();
        
        // 找一个单候选的派发表
        for (ExtensionDispatchTable table : tables) {
            if (table.getCandidateCount() == 1) {
                assertFalse(table.usesMapPath(), "单候选应该不使用 Map 路径");
                return;
            }
        }
    }

    @Test
    void testSmallCandidatesPath() {
        // 测试小候选数组路径
        Environment env = runtime.newEnvironment();
        ExtensionDispatchTable[] tables = env.getRootDispatchTables();

        // 找一个小候选（2-6）的派发表
        for (ExtensionDispatchTable table : tables) {
            int count = table.getCandidateCount();
            if (count >= 2 && count <= 6) {
                assertFalse(table.usesMapPath(), "小候选（2-6）应该不使用 Map 路径");
                return;
            }
        }
    }

    @Test
    void testDispatchPerformance() {
        Environment env = runtime.newEnvironment();
        int index = getExtensionIndex("length");
        int iterations = 100000;

        // 预热
        for (int i = 0; i < 1000; i++) {
            env.getExtensionFunctionOrNull(String.class, index, 0);
        }

        // 测试派发表性能
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            env.getExtensionFunctionOrNull(String.class, index, 0);
        }
        long endTime = System.nanoTime();

        long avgTimeNanos = (endTime - startTime) / iterations;
        System.out.printf("派发表查找平均耗时: %d ns%n", avgTimeNanos);

        // 应该非常快（通常 < 100ns，因为有缓存）
        assertTrue(avgTimeNanos < 1000, "派发表查找应该非常快（< 1000ns）");
    }

    @Test
    void testAssignableMatchPerformanceWithCache() {
        Environment env = runtime.newEnvironment();
        int index = getExtensionIndex("size");
        int iterations = 100000;

        // 预热（第一次调用会计算并缓存）
        env.getExtensionFunctionOrNull(ArrayList.class, index, 0);

        // 测试缓存后的性能
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            env.getExtensionFunctionOrNull(ArrayList.class, index, 0);
        }
        long endTime = System.nanoTime();

        long avgTimeNanos = (endTime - startTime) / iterations;
        System.out.printf("可赋值匹配（缓存后）查找平均耗时: %d ns%n", avgTimeNanos);

        // 缓存后应该和精确匹配一样快
        assertTrue(avgTimeNanos < 1000, "缓存后的可赋值匹配查找应该非常快（< 1000ns）");
    }

    /**
     * 获取扩展函数的索引
     * 如果找不到返回 -1
     */
    private int getExtensionIndex(String name) {
        int index = 0;
        for (String key : runtime.getExtensionFunctions().keySet()) {
            if (key.equals(name)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    @Test
    void testMostSpecificMatchInHierarchy() {
        // 构造候选：Iterable（注册在前）, Collection（更具体，注册在后）
        // 修复前 resolveSmallCandidates 会返回第一个可赋值的 Iterable
        // 修复后应返回最具体的 Collection
        OverloadSet iterableSet = new OverloadSet("test");
        iterableSet.add(new NativeFunction<>("test", FunctionSignature.returns(Type.I).noParams(), ctx -> ctx.setReturnInt(-1)));
        OverloadSet collectionSet = new OverloadSet("test");
        collectionSet.add(new NativeFunction<>("test", FunctionSignature.returns(Type.I).noParams(), ctx -> ctx.setReturnInt(1)));
        Map<Class<?>, OverloadSet> exactMap = new LinkedHashMap<>();
        exactMap.put(Iterable.class, iterableSet);
        exactMap.put(java.util.Collection.class, collectionSet);
        Class<?>[] candidateClasses = { Iterable.class, java.util.Collection.class };
        OverloadSet[] candidateOverloadSets = { iterableSet, collectionSet };
        ExtensionDispatchTable table = new ExtensionDispatchTable(exactMap, candidateClasses, candidateOverloadSets);
        // ArrayList implements Collection (which extends Iterable)
        // 应当匹配到更具体的 Collection，而非 Iterable
        OverloadSet resolved = table.resolveOverloadSet(ArrayList.class);
        assertSame(collectionSet, resolved, "Should resolve to Collection (most specific), not Iterable");
    }

    @Test
    void testMostSpecificMatchReversedOrder() {
        // 候选顺序反转：Collection 在前，Iterable 在后
        // 无论注册顺序如何，都应选择最具体的 Collection
        OverloadSet collectionSet = new OverloadSet("test");
        collectionSet.add(new NativeFunction<>("test", FunctionSignature.returns(Type.I).noParams(), ctx -> ctx.setReturnInt(1)));
        OverloadSet iterableSet = new OverloadSet("test");
        iterableSet.add(new NativeFunction<>("test", FunctionSignature.returns(Type.I).noParams(), ctx -> ctx.setReturnInt(-1)));
        Map<Class<?>, OverloadSet> exactMap = new LinkedHashMap<>();
        exactMap.put(java.util.Collection.class, collectionSet);
        exactMap.put(Iterable.class, iterableSet);
        Class<?>[] candidateClasses = { java.util.Collection.class, Iterable.class };
        OverloadSet[] candidateOverloadSets = { collectionSet, iterableSet };
        ExtensionDispatchTable table = new ExtensionDispatchTable(exactMap, candidateClasses, candidateOverloadSets);
        OverloadSet resolved = table.resolveOverloadSet(ArrayList.class);
        assertSame(collectionSet, resolved, "Should resolve to Collection regardless of registration order");
    }

    /**
     * 复现同名扩展挂在两个 target 上（接口 0 参 + 基类 1 参谓词），
     * 子类实例调用无参形式时 resolve 失败。
     * <p>
     * OverloadSet 是按「注册 target 类型」分桶的，不是把接口 target 与基类 target 的重载合成一个集合；
     * resolveOverloadSet 只选「最贴」的一个 target 的 OverloadSet，再在该集合内按 argCount 解析。
     * 该回归用 FluxonRuntime 注册扩展，验证 bake 后的派发表会把父类型桶合入更具体的 target 桶。
     */
    @Test
    void reproduceSameNameOnInterfaceAndBaseZeroArgMissesOnSubclass() {
        String name = "same_name_extension_regression";
        Function interfaceFunction = new NativeFunction<>(
                name,
                FunctionSignature.returns(Type.VOID).noParams(),
                ctx -> {}
        );
        Function baseFunction = new NativeFunction<>(
                name,
                FunctionSignature.returns(Type.Z).params(Function.TYPE),
                ctx -> ctx.setReturnBool(true)
        );
        try {
            runtime.registerExtensionFunction(InterfaceExtensionTargetStub.class, interfaceFunction);
            runtime.registerExtensionFunction(BaseExtensionTargetStub.class, baseFunction);

            Environment env = runtime.newEnvironment();
            int index = getExtensionIndex(name);

            assertSame(baseFunction, env.getExtensionFunctionOrNull(DerivedExtensionTargetStub.class, index, 1), "1 参谓词应仍在基类 target 桶内解析");
            assertSame(interfaceFunction, env.getExtensionFunctionOrNull(DerivedExtensionTargetStub.class, index, 0), "期望子类上同名扩展能命中接口 target 的 0 参；当前只查基类 target 桶会返回 null");
        } finally {
            runtime.unregisterExtensionFunction(InterfaceExtensionTargetStub.class, name, interfaceFunction);
            runtime.unregisterExtensionFunction(BaseExtensionTargetStub.class, name, baseFunction);
        }
    }

    /**
     * 验证 bake 合并父类型桶不依赖注册顺序。
     * 基类 target 先注册时，子类仍应能从接口 target 合入 0 参重载。
     */
    @Test
    void bakeMergesParentInterfaceBucketWhenBaseRegisteredFirst() {
        String name = "same_name_extension_reverse_order_regression";
        Function baseFunction = new NativeFunction<>(
                name,
                FunctionSignature.returns(Type.Z).params(Function.TYPE),
                ctx -> ctx.setReturnBool(true)
        );
        Function interfaceFunction = new NativeFunction<>(
                name,
                FunctionSignature.returns(Type.VOID).noParams(),
                ctx -> {}
        );
        try {
            runtime.registerExtensionFunction(BaseExtensionTargetStub.class, baseFunction);
            runtime.registerExtensionFunction(InterfaceExtensionTargetStub.class, interfaceFunction);

            Environment env = runtime.newEnvironment();
            int index = getExtensionIndex(name);

            assertSame(baseFunction, env.getExtensionFunctionOrNull(DerivedExtensionTargetStub.class, index, 1), "基类 target 先注册时，1 参谓词仍应命中基类桶");
            assertSame(interfaceFunction, env.getExtensionFunctionOrNull(DerivedExtensionTargetStub.class, index, 0), "基类 target 先注册时，0 参重载仍应从接口桶合入");
        } finally {
            runtime.unregisterExtensionFunction(BaseExtensionTargetStub.class, name, baseFunction);
            runtime.unregisterExtensionFunction(InterfaceExtensionTargetStub.class, name, interfaceFunction);
        }
    }

    /**
     * 验证更具体 target 自身的重载优先于合入的父类型重载。
     * 合并只补全漏查的父类型重载，不能覆盖子类实际应命中的基类 target 重载。
     */
    @Test
    void bakeKeepsSpecificBucketOverParentBucketForSameArgCount() {
        String name = "same_arg_count_extension_regression";
        Function interfaceFunction = new NativeFunction<>(
                name,
                FunctionSignature.returns(Type.I).noParams(),
                ctx -> ctx.setReturnInt(-1)
        );
        Function baseFunction = new NativeFunction<>(
                name,
                FunctionSignature.returns(Type.I).noParams(),
                ctx -> ctx.setReturnInt(1)
        );
        try {
            runtime.registerExtensionFunction(InterfaceExtensionTargetStub.class, interfaceFunction);
            runtime.registerExtensionFunction(BaseExtensionTargetStub.class, baseFunction);

            Environment env = runtime.newEnvironment();
            int index = getExtensionIndex(name);

            assertSame(baseFunction, env.getExtensionFunctionOrNull(DerivedExtensionTargetStub.class, index, 0), "同参数数量时，更具体的基类 target 重载应优先于接口 target 重载");
            assertSame(interfaceFunction, env.getExtensionFunctionOrNull(InterfaceExtensionTargetStub.class, index, 0), "接口 target 本身仍应命中接口桶重载");
        } finally {
            runtime.unregisterExtensionFunction(InterfaceExtensionTargetStub.class, name, interfaceFunction);
            runtime.unregisterExtensionFunction(BaseExtensionTargetStub.class, name, baseFunction);
        }
    }

    /**
     * 验证 bake 只合并可赋值的父类型桶。
     * 不相关 target 的同名重载不能被合入子类可见的派发桶。
     */
    @Test
    void bakeDoesNotMergeUnrelatedTargetBucket() {
        String name = "unrelated_target_extension_regression";
        Function interfaceFunction = new NativeFunction<>(
                name,
                FunctionSignature.returns(Type.VOID).noParams(),
                ctx -> {}
        );
        Function unrelatedFunction = new NativeFunction<>(
                name,
                FunctionSignature.returns(Type.Z).params(Function.TYPE),
                ctx -> ctx.setReturnBool(true)
        );
        try {
            runtime.registerExtensionFunction(InterfaceExtensionTargetStub.class, interfaceFunction);
            runtime.registerExtensionFunction(UnrelatedExtensionTargetStub.class, unrelatedFunction);

            Environment env = runtime.newEnvironment();
            int index = getExtensionIndex(name);

            assertSame(interfaceFunction, env.getExtensionFunctionOrNull(DerivedExtensionTargetStub.class, index, 0), "子类应能命中接口 target 的 0 参重载");
            assertNull(env.getExtensionFunctionOrNull(DerivedExtensionTargetStub.class, index, 1), "不相关 target 的 1 参重载不能合入子类派发桶");
            assertSame(unrelatedFunction, env.getExtensionFunctionOrNull(UnrelatedExtensionTargetStub.class, index, 1), "不相关 target 自身仍应命中自己的重载");
        } finally {
            runtime.unregisterExtensionFunction(InterfaceExtensionTargetStub.class, name, interfaceFunction);
            runtime.unregisterExtensionFunction(UnrelatedExtensionTargetStub.class, name, unrelatedFunction);
        }
    }

    interface InterfaceExtensionTargetStub {
    }

    static class BaseExtensionTargetStub implements InterfaceExtensionTargetStub {
    }

    static class DerivedExtensionTargetStub extends BaseExtensionTargetStub {
    }

    static class UnrelatedExtensionTargetStub {
    }

    @Test
    void testUnrelatedCandidatesStillWork() {
        // Collection 和 Map 是不相关的接口，LinkedHashMap 只匹配 Map
        OverloadSet collectionSet = new OverloadSet("test");
        collectionSet.add(new NativeFunction<>("test", FunctionSignature.returns(Type.I).noParams(), ctx -> ctx.setReturnInt(-1)));
        OverloadSet mapSet = new OverloadSet("test");
        mapSet.add(new NativeFunction<>("test", FunctionSignature.returns(Type.I).noParams(), ctx -> ctx.setReturnInt(1)));
        Map<Class<?>, OverloadSet> exactMap = new LinkedHashMap<>();
        exactMap.put(java.util.Collection.class, collectionSet);
        exactMap.put(java.util.Map.class, mapSet);
        Class<?>[] candidateClasses = { java.util.Collection.class, java.util.Map.class };
        OverloadSet[] candidateOverloadSets = { collectionSet, mapSet };
        ExtensionDispatchTable table = new ExtensionDispatchTable(exactMap, candidateClasses, candidateOverloadSets);
        OverloadSet resolved = table.resolveOverloadSet(LinkedHashMap.class);
        assertSame(mapSet, resolved, "LinkedHashMap should match Map, not Collection");
    }
}
