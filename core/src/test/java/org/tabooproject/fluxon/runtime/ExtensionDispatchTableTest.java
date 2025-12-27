package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

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

    // ========== 精确匹配测试 ==========

    @Test
    void testExactMatchForString() {
        Environment env = runtime.newEnvironment();
        
        // String 类型应该精确匹配到 String 的扩展函数
        Function func = env.getExtensionFunctionOrNull(String.class, "length", getExtensionIndex("length"));
        assertNotNull(func, "String.length 扩展函数应该存在");
    }

    @Test
    void testExactMatchForArrayList() {
        Environment env = runtime.newEnvironment();
        
        // ArrayList 应该匹配到 List 或 Collection 的扩展函数
        Function func = env.getExtensionFunctionOrNull(ArrayList.class, "size", getExtensionIndex("size"));
        assertNotNull(func, "ArrayList.size 扩展函数应该存在");
    }

    // ========== 可赋值匹配测试 ==========

    @Test
    void testAssignableMatchForSubclass() {
        Environment env = runtime.newEnvironment();
        
        // 自定义子类应该能匹配到父类的扩展函数
        // LinkedHashMap 是 Map 的实现，应该匹配到 Map 的扩展函数
        Function func = env.getExtensionFunctionOrNull(LinkedHashMap.class, "keySet", getExtensionIndex("keySet"));
        assertNotNull(func, "LinkedHashMap 应该匹配到 Map.keySet 扩展函数");
    }

    // ========== 缓存稳定性测试 ==========

    @Test
    void testCacheStabilityForSameClass() {
        Environment env = runtime.newEnvironment();
        int index = getExtensionIndex("length");
        
        // 对同一目标类型多次调用，应该返回相同的函数实例
        Function func1 = env.getExtensionFunctionOrNull(String.class, "length", index);
        Function func2 = env.getExtensionFunctionOrNull(String.class, "length", index);
        Function func3 = env.getExtensionFunctionOrNull(String.class, "length", index);
        
        assertSame(func1, func2, "多次调用应该返回相同的函数实例");
        assertSame(func2, func3, "多次调用应该返回相同的函数实例");
    }

    @Test
    void testCacheStabilityForAssignableClass() {
        Environment env = runtime.newEnvironment();
        int index = getExtensionIndex("size");
        
        // 对可赋值匹配的类型多次调用，应该返回相同的函数实例
        Function func1 = env.getExtensionFunctionOrNull(ArrayList.class, "size", index);
        Function func2 = env.getExtensionFunctionOrNull(ArrayList.class, "size", index);
        
        assertSame(func1, func2, "可赋值匹配的缓存应该稳定");
    }

    // ========== 派发表结构测试 ==========

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

    // ========== 名称回退路径测试 ==========

    @Test
    void testNameFallbackPath() {
        Environment env = runtime.newEnvironment();
        
        // 使用 index=-1 触发名称回退路径
        // 注册一个动态扩展函数来测试
        env.defineRootExtensionFunction(HashMap.class, "testDynamic", 
            new NativeFunction<>(null, null, ctx -> "dynamic"));
        
        // 使用名称回退路径查找
        Function func = env.getExtensionFunctionOrNull(HashMap.class, "testDynamic", -1);
        assertNotNull(func, "名称回退路径应该能找到动态注册的扩展函数");
    }

    @Test
    void testNameFallbackPathAssignableMatch() {
        Environment env = runtime.newEnvironment();
        
        // 注册一个针对 List 的动态扩展函数
        env.defineRootExtensionFunction(List.class, "testListDynamic",
            new NativeFunction<>(null, null, ctx -> "list-dynamic"));
        
        // ArrayList 应该能通过可赋值匹配找到
        Function func = env.getExtensionFunctionOrNull(ArrayList.class, "testListDynamic", -1);
        assertNotNull(func, "名称回退路径应该支持可赋值匹配");
    }

    // ========== 性能测试 ==========

    @Test
    void testDispatchPerformance() {
        Environment env = runtime.newEnvironment();
        int index = getExtensionIndex("length");
        int iterations = 100000;
        
        // 预热
        for (int i = 0; i < 1000; i++) {
            env.getExtensionFunctionOrNull(String.class, "length", index);
        }
        
        // 测试派发表性能
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            env.getExtensionFunctionOrNull(String.class, "length", index);
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
        env.getExtensionFunctionOrNull(ArrayList.class, "size", index);
        
        // 测试缓存后的性能
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            env.getExtensionFunctionOrNull(ArrayList.class, "size", index);
        }
        long endTime = System.nanoTime();
        
        long avgTimeNanos = (endTime - startTime) / iterations;
        System.out.printf("可赋值匹配（缓存后）查找平均耗时: %d ns%n", avgTimeNanos);
        
        // 缓存后应该和精确匹配一样快
        assertTrue(avgTimeNanos < 1000, "缓存后的可赋值匹配查找应该非常快（< 1000ns）");
    }

    // ========== 辅助方法 ==========

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
}
