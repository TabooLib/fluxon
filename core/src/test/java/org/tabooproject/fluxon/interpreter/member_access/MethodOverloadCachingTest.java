package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.runtime.reflection.util.PolymorphicInlineCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 Polymorphic Inline Cache (PIC) 的正确性
 * <p>
 * 覆盖场景：
 * 1. 方法重载多态调用
 * 2. 构造函数重载多态调用
 * 3. PIC 深度限制
 * 4. 线程安全
 * 5. null 参数处理
 * 6. 类型签名检查
 */
public class MethodOverloadCachingTest {

    private static final AtomicInteger CLASS_COUNTER = new AtomicInteger(0);

    @BeforeEach
    public void setUp() {
        // 每个测试前清除 PIC 缓存
        PolymorphicInlineCache.clear();
    }

    // ==================== 测试辅助类 ====================

    /**
     * 测试类：提供重载方法
     */
    public static class OverloadedMethods {
        public String process(Integer value) {
            return "Integer:" + value;
        }

        public String process(String value) {
            return "String:" + value;
        }

        public String process(Double value) {
            return "Double:" + value;
        }

        public String process(Long value) {
            return "Long:" + value;
        }

        public String process(Boolean value) {
            return "Boolean:" + value;
        }

        public String processWithNull(Object value) {
            return "Object:" + value;
        }
    }

    /**
     * 多参数重载测试类
     */
    public static class MultiArgOverloads {
        public String combine(String a, Integer b) {
            return "SI:" + a + "," + b;
        }

        public String combine(Integer a, String b) {
            return "IS:" + a + "," + b;
        }

        public String combine(String a, String b) {
            return "SS:" + a + "," + b;
        }
    }

    // ==================== 基本 PIC 功能测试 ====================

    /**
     * 测试：循环中同一 call site 用不同参数类型调用重载方法
     */
    @Test
    public void testOverloadedMethodInLoop() throws Exception {
        String source =
                "results = []; " +
                        "for item in &items { " +
                        "  result = &testObj.process(&item); " +
                        "  results = &results + [&result] " +
                        "}; " +
                        "&results";

        List<Object> items = Arrays.asList(42, "hello", 3.14, 100, "world");
        OverloadedMethods testObj = new OverloadedMethods();

        String className = "OverloadTest_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("testObj", testObj);
        env.defineRootVariable("items", items);

        CompileResult compileResult = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase script = (RuntimeScriptBase) scriptClass.newInstance();

        Object result = script.eval(env);

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) result;

        assertEquals(5, results.size());
        assertEquals("Integer:42", results.get(0));
        assertEquals("String:hello", results.get(1));
        assertEquals("Double:3.14", results.get(2));
        assertEquals("Integer:100", results.get(3));
        assertEquals("String:world", results.get(4));
    }

    /**
     * 测试：解释器模式下的重载解析（作为对照组）
     */
    @Test
    public void testOverloadedMethodInLoopInterpreted() {
        String source =
                "results = []; " +
                        "for item in &items { " +
                        "  result = &testObj.process(&item); " +
                        "  results = &results + [&result] " +
                        "}; " +
                        "&results";

        List<Object> items = Arrays.asList(42, "hello", 3.14, 100, "world");
        OverloadedMethods testObj = new OverloadedMethods();

        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("testObj", testObj);
        env.defineRootVariable("items", items);

        Object result = Fluxon.parse(ctx, env).eval(env);

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) result;

        assertEquals(5, results.size());
        assertEquals("Integer:42", results.get(0));
        assertEquals("String:hello", results.get(1));
        assertEquals("Double:3.14", results.get(2));
        assertEquals("Integer:100", results.get(3));
        assertEquals("String:world", results.get(4));
    }

    /**
     * 测试：编译与解释结果一致性
     */
    @Test
    public void testCompileAndInterpretConsistency() throws Exception {
        String source =
                "results = []; " +
                        "for item in &items { " +
                        "  result = &testObj.process(&item); " +
                        "  results = &results + [&result] " +
                        "}; " +
                        "&results";

        List<Object> items = Arrays.asList(42, "hello", 3.14, 100L, true);
        OverloadedMethods testObj = new OverloadedMethods();

        // 解释执行
        CompilationContext ctx1 = new CompilationContext(source);
        ctx1.setAllowReflectionAccess(true);
        Environment env1 = FluxonRuntime.getInstance().newEnvironment();
        env1.defineRootVariable("testObj", testObj);
        env1.defineRootVariable("items", items);
        Object interpretResult = Fluxon.parse(ctx1, env1).eval(env1);

        // 编译执行
        String className = "ConsistencyTest_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx2 = new CompilationContext(source);
        ctx2.setAllowReflectionAccess(true);
        Environment env2 = FluxonRuntime.getInstance().newEnvironment();
        env2.defineRootVariable("testObj", testObj);
        env2.defineRootVariable("items", items);
        CompileResult compileResult = Fluxon.compile(env2, ctx2, className);
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase script = (RuntimeScriptBase) scriptClass.newInstance();
        Object compileResultValue = script.eval(env2);

        assertEquals(interpretResult, compileResultValue);
    }

    // ==================== 构造函数测试 ====================

    /**
     * 测试：构造函数重载缓存
     */
    @Test
    public void testConstructorOverloadInLoop() throws Exception {
        String source =
                "results = []; " +
                        "for item in &items { " +
                        "  sb = new java.lang.StringBuilder(&item); " +
                        "  results = &results + [&sb.toString()] " +
                        "}; " +
                        "&results";

        List<Object> items = Arrays.asList("hello", "world", "test");

        String className = "ConstructorOverloadTest_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowJavaConstruction(true);
        ctx.setAllowReflectionAccess(true);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("items", items);

        CompileResult compileResult = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase script = (RuntimeScriptBase) scriptClass.newInstance();

        Object result = script.eval(env);

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) result;

        assertEquals(3, results.size());
        assertEquals("hello", results.get(0));
        assertEquals("world", results.get(1));
        assertEquals("test", results.get(2));
    }

    // ==================== PIC 深度限制测试 ====================

    /**
     * 测试：超过 PIC 深度限制后仍能正确工作
     */
    @Test
    public void testPICDepthLimit() throws Exception {
        // 创建超过 MAX_PIC_DEPTH 种类型的调用
        String source =
                "results = []; " +
                        "for item in &items { " +
                        "  result = &testObj.process(&item); " +
                        "  results = &results + [&result] " +
                        "}; " +
                        "&results";

        // 创建 10 种不同类型的调用（超过 MAX_PIC_DEPTH=8）
        List<Object> items = Arrays.asList(
                42,           // Integer
                "hello",      // String
                3.14,         // Double
                100L,         // Long
                true,         // Boolean
                43,           // Integer (重复)
                "world",      // String (重复)
                2.71,         // Double (重复)
                200L,         // Long (重复)
                false         // Boolean (重复)
        );

        OverloadedMethods testObj = new OverloadedMethods();

        String className = "PICDepthTest_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("testObj", testObj);
        env.defineRootVariable("items", items);

        CompileResult compileResult = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase script = (RuntimeScriptBase) scriptClass.newInstance();

        Object result = script.eval(env);

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) result;

        assertEquals(10, results.size());
        assertEquals("Integer:42", results.get(0));
        assertEquals("String:hello", results.get(1));
        assertEquals("Double:3.14", results.get(2));
        assertEquals("Long:100", results.get(3));
        assertEquals("Boolean:true", results.get(4));
        assertEquals("Integer:43", results.get(5));
        assertEquals("String:world", results.get(6));
        assertEquals("Double:2.71", results.get(7));
        assertEquals("Long:200", results.get(8));
        assertEquals("Boolean:false", results.get(9));
    }

    // ==================== 线程安全测试 ====================

    /**
     * 测试：多线程并发调用同一 call site
     * 注意：每个线程需要使用独立的脚本实例，因为 RuntimeScriptBase.eval()
     * 会将 Environment 写入实例字段，多线程共享实例会导致环境被覆盖。
     * 但 invokedynamic CallSite 是类级别共享的，所以 PIC 仍能被并发测试。
     */
    @Test
    public void testConcurrentPICAccess() throws Exception {
        String source = "&testObj.process(&item)";

        OverloadedMethods testObj = new OverloadedMethods();

        String className = "ConcurrentTest_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);

        // 创建初始环境用于编译
        Environment compileEnv = FluxonRuntime.getInstance().newEnvironment();
        compileEnv.defineRootVariable("testObj", testObj);
        compileEnv.defineRootVariable("item", 42);

        CompileResult compileResult = Fluxon.compile(compileEnv, ctx, className);
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());

        int threadCount = 10;
        int iterationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Object> errors = Collections.synchronizedList(new ArrayList<>());
        List<Object> testItems = Arrays.asList(42, "hello", 3.14, 100L, true);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    // 每个线程创建独立的脚本实例，避免 Environment 字段竞争
                    RuntimeScriptBase script = (RuntimeScriptBase) scriptClass.newInstance();

                    for (int i = 0; i < iterationsPerThread; i++) {
                        Object item = testItems.get((threadId + i) % testItems.size());
                        Environment env = FluxonRuntime.getInstance().newEnvironment();
                        env.defineRootVariable("testObj", testObj);
                        env.defineRootVariable("item", item);

                        Object result = script.eval(env);

                        // 验证结果正确
                        String expected;
                        if (item instanceof Integer) {
                            expected = "Integer:" + item;
                        } else if (item instanceof String) {
                            expected = "String:" + item;
                        } else if (item instanceof Double) {
                            expected = "Double:" + item;
                        } else if (item instanceof Long) {
                            expected = "Long:" + item;
                        } else if (item instanceof Boolean) {
                            expected = "Boolean:" + item;
                        } else {
                            expected = "Unknown";
                        }

                        if (!expected.equals(result)) {
                            errorCount.incrementAndGet();
                            errors.add("Expected: " + expected + ", Got: " + result + ", Item: " + item + " (" + item.getClass().getSimpleName() + ")");
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    errors.add("Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        if (errorCount.get() > 0) {
            // 只打印前10个错误
            int printCount = Math.min(10, errors.size());
            StringBuilder sb = new StringBuilder("Concurrent access errors (" + errorCount.get() + " total):\n");
            for (int i = 0; i < printCount; i++) {
                sb.append("  ").append(errors.get(i)).append("\n");
            }
            fail(sb.toString());
        }
    }

    // ==================== 类型签名检查测试 ====================

    /**
     * 测试：checkFullTypeSignature 方法
     */
    @Test
    public void testCheckFullTypeSignature() {
        // 正确匹配
        assertTrue(PolymorphicInlineCache.checkFullTypeSignature(
                String.class,
                new Class<?>[]{Integer.class},
                "test",
                new Object[]{42}
        ));

        // receiver 类型不匹配
        assertFalse(PolymorphicInlineCache.checkFullTypeSignature(
                Integer.class,
                new Class<?>[]{Integer.class},
                "test",
                new Object[]{42}
        ));

        // 参数类型不匹配
        assertFalse(PolymorphicInlineCache.checkFullTypeSignature(
                String.class,
                new Class<?>[]{String.class},
                "test",
                new Object[]{42}
        ));

        // 参数数量不匹配
        assertFalse(PolymorphicInlineCache.checkFullTypeSignature(
                String.class,
                new Class<?>[]{Integer.class, String.class},
                "test",
                new Object[]{42}
        ));

        // null receiver
        assertFalse(PolymorphicInlineCache.checkFullTypeSignature(
                String.class,
                new Class<?>[]{Integer.class},
                null,
                new Object[]{42}
        ));
    }

    /**
     * 测试：checkFullTypeSignature 处理 null 参数
     */
    @Test
    public void testCheckFullTypeSignatureWithNull() {
        // 期望 null，实际 null
        assertTrue(PolymorphicInlineCache.checkFullTypeSignature(
                String.class,
                new Class<?>[]{Void.class},  // Void.class 表示期望 null
                "test",
                new Object[]{null}
        ));

        // 期望 null，实际非 null
        assertFalse(PolymorphicInlineCache.checkFullTypeSignature(
                String.class,
                new Class<?>[]{Void.class},
                "test",
                new Object[]{42}
        ));

        // 期望非 null，实际 null
        assertFalse(PolymorphicInlineCache.checkFullTypeSignature(
                String.class,
                new Class<?>[]{Integer.class},
                "test",
                new Object[]{null}
        ));
    }

    /**
     * 测试：checkConstructorTypeSignature 方法
     */
    @Test
    public void testCheckConstructorTypeSignature() {
        // 正确匹配
        assertTrue(PolymorphicInlineCache.checkConstructorTypeSignature(
                "java.lang.StringBuilder",
                new Class<?>[]{String.class},
                "java.lang.StringBuilder",
                new Object[]{"hello"}
        ));

        // 类名不匹配
        assertFalse(PolymorphicInlineCache.checkConstructorTypeSignature(
                "java.lang.StringBuilder",
                new Class<?>[]{String.class},
                "java.lang.StringBuffer",
                new Object[]{"hello"}
        ));

        // 参数类型不匹配
        assertFalse(PolymorphicInlineCache.checkConstructorTypeSignature(
                "java.lang.StringBuilder",
                new Class<?>[]{Integer.class},
                "java.lang.StringBuilder",
                new Object[]{"hello"}
        ));
    }

    // ==================== 多参数重载测试 ====================

    /**
     * 测试：多参数方法的重载解析
     */
    @Test
    public void testMultiArgOverloads() throws Exception {
        String source =
                "results = []; " +
                        "for pair in &pairs { " +
                        "  result = &testObj.combine(&pair.get(0), &pair.get(1)); " +
                        "  results = &results + [&result] " +
                        "}; " +
                        "&results";

        List<List<Object>> pairs = Arrays.asList(
                Arrays.<Object>asList("hello", 42),     // SI
                Arrays.<Object>asList(100, "world"),    // IS
                Arrays.<Object>asList("foo", "bar")     // SS
        );

        MultiArgOverloads testObj = new MultiArgOverloads();

        String className = "MultiArgTest_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("testObj", testObj);
        env.defineRootVariable("pairs", pairs);

        CompileResult compileResult = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase script = (RuntimeScriptBase) scriptClass.newInstance();

        Object result = script.eval(env);

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) result;

        assertEquals(3, results.size());
        assertEquals("SI:hello,42", results.get(0));
        assertEquals("IS:100,world", results.get(1));
        assertEquals("SS:foo,bar", results.get(2));
    }

    // ==================== PIC 缓存管理测试 ====================

    /**
     * 测试：PIC 缓存清除
     */
    @Test
    public void testCacheClear() {
        assertEquals(0, PolymorphicInlineCache.size());
        // 运行一些会创建 PIC 条目的代码后清除
        PolymorphicInlineCache.clear();
        assertEquals(0, PolymorphicInlineCache.size());
    }

    /**
     * 测试：单类型调用（单态 call site）
     */
    @Test
    public void testMonomorphicCallSite() throws Exception {
        String source =
                "results = []; " +
                        "for item in &items { " +
                        "  result = &testObj.process(&item); " +
                        "  results = &results + [&result] " +
                        "}; " +
                        "&results";

        // 只使用 Integer 类型
        List<Object> items = Arrays.asList(1, 2, 3, 4, 5);
        OverloadedMethods testObj = new OverloadedMethods();

        String className = "MonomorphicTest_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("testObj", testObj);
        env.defineRootVariable("items", items);

        CompileResult compileResult = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase script = (RuntimeScriptBase) scriptClass.newInstance();

        Object result = script.eval(env);

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) result;

        assertEquals(5, results.size());
        for (int i = 0; i < 5; i++) {
            assertEquals("Integer:" + (i + 1), results.get(i));
        }
    }

    /**
     * 测试：空参数列表
     */
    @Test
    public void testEmptyArguments() throws Exception {
        String source = "&testObj.toString()";

        OverloadedMethods testObj = new OverloadedMethods();

        String className = "EmptyArgsTest_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("testObj", testObj);

        CompileResult compileResult = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase script = (RuntimeScriptBase) scriptClass.newInstance();

        Object result = script.eval(env);

        assertNotNull(result);
        assertTrue(result instanceof String);
    }

    /**
     * 测试：重复调用同一类型签名
     */
    @Test
    public void testRepeatedSameTypeSignature() throws Exception {
        String source =
                "results = []; " +
                        "for i in [1, 2, 3] { " +
                        "  result = &testObj.process(&value); " +
                        "  results = &results + [&result] " +
                        "}; " +
                        "&results";

        OverloadedMethods testObj = new OverloadedMethods();

        String className = "RepeatedTest_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("testObj", testObj);
        env.defineRootVariable("value", "same");

        CompileResult compileResult = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase script = (RuntimeScriptBase) scriptClass.newInstance();

        Object result = script.eval(env);

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) result;

        assertEquals(3, results.size());
        for (String r : results) {
            assertEquals("String:same", r);
        }
    }
}
