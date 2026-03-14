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
