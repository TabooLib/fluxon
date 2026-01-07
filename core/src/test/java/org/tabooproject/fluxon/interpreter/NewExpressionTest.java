package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * new 表达式测试
 * 测试 Java 对象构造语法: new fully.qualified.ClassName(args)
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class NewExpressionTest {

    // ========== 辅助方法 ==========

    /**
     * 使用启用了 Java 构造特性的上下文执行脚本
     */
    private TestResult runWithJavaConstruction(String source) {
        // 解释执行
        Environment interpretEnv = FluxonRuntime.getInstance().newEnvironment();
        CompilationContext interpretCtx = new CompilationContext(source);
        interpretCtx.setAllowJavaConstruction(true);
        interpretCtx.setAllowReflectionAccess(true);  // 启用 member access (.)
        interpretCtx.setAllowInvalidReference(true);  // 允许延迟解析函数调用
        ParsedScript script = Fluxon.parse(interpretCtx, interpretEnv);
        Object interpretResult;
        try {
            interpretResult = script.eval(interpretEnv);
        } catch (ReturnValue rv) {
            interpretResult = rv.getValue();
        }
        // 编译执行
        Environment compileEnv = FluxonRuntime.getInstance().newEnvironment();
        CompilationContext compileCtx = new CompilationContext(source);
        compileCtx.setAllowJavaConstruction(true);
        compileCtx.setAllowReflectionAccess(true);  // 启用 member access (.)
        compileCtx.setAllowInvalidReference(true);  // 允许延迟解析函数调用
        CompileResult compileResultObj = Fluxon.compile(compileEnv, compileCtx, "TestScript");
        Class<?> scriptClass = compileResultObj.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base;
        try {
            base = (RuntimeScriptBase) scriptClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Object compileResult = base.eval(compileEnv);
        return new TestResult(interpretResult, compileResult);
    }

    /**
     * 仅解释执行（用于测试预期异常）
     */
    private Object interpretWithJavaConstruction(String source) {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowJavaConstruction(true);
        ctx.setAllowReflectionAccess(true);  // 启用 member access (.)
        ctx.setAllowInvalidReference(true);  // 允许延迟解析函数调用
        ParsedScript script = Fluxon.parse(ctx, env);
        try {
            return script.eval(env);
        } catch (ReturnValue rv) {
            return rv.getValue();
        }
    }

    private static class TestResult {
        final Object interpretResult;
        final Object compileResult;

        TestResult(Object interpretResult, Object compileResult) {
            this.interpretResult = interpretResult;
            this.compileResult = compileResult;
        }

        void assertBothEqual(Object expected) {
            assertEquals(expected, interpretResult, "Interpret result mismatch");
            assertEquals(expected, compileResult, "Compile result mismatch");
        }

        void assertBothToStringEqual(String expected) {
            assertEquals(expected, String.valueOf(interpretResult), "Interpret result toString mismatch");
            assertEquals(expected, String.valueOf(compileResult), "Compile result toString mismatch");
        }

        void assertBothInstanceOf(Class<?> clazz) {
            assertTrue(clazz.isInstance(interpretResult),
                    "Interpret result should be instance of " + clazz.getName() + ", but was " +
                            (interpretResult == null ? "null" : interpretResult.getClass().getName()));
            assertTrue(clazz.isInstance(compileResult),
                    "Compile result should be instance of " + clazz.getName() + ", but was " +
                            (compileResult == null ? "null" : compileResult.getClass().getName()));
        }
    }

    // ========== 基本构造语法测试 ==========

    @Test
    public void testBasicConstruction() {
        // 测试基本的 ArrayList 构造
        TestResult result = runWithJavaConstruction("new java.util.ArrayList()");
        result.assertBothInstanceOf(ArrayList.class);
    }

    @Test
    public void testConstructionWithInitialCapacity() {
        // 测试带参数的 ArrayList 构造
        // 使用 Fluxon 内置的 size() 方法验证对象被正确创建
        TestResult result = runWithJavaConstruction(
                "list = new java.util.ArrayList(10); " +
                        "&list::size()");
        result.assertBothEqual(0);  // ArrayList(initialCapacity) 创建空列表
    }

    @Test
    public void testHashMapConstruction() {
        // 测试 HashMap 构造
        TestResult result = runWithJavaConstruction("new java.util.HashMap()");
        result.assertBothInstanceOf(HashMap.class);
    }

    @Test
    public void testStringBuilderConstruction() {
        // 测试 StringBuilder 构造并使用内置的字符串拼接
        TestResult result = runWithJavaConstruction(
                "sb = new java.lang.StringBuilder('Hello'); " +
                        "&sb::toString()");
        result.assertBothEqual("Hello");
    }

    @Test
    public void testStringBuilderWithInitialValue() {
        // 测试带初始值的 StringBuilder
        TestResult result = runWithJavaConstruction(
                "sb = new java.lang.StringBuilder('Initial'); " +
                        "&sb::toString()");
        result.assertBothEqual("Initial");
    }

    // ========== 与 Member Access (.) 集成测试 ==========

    @Test
    public void testMemberAccessOnNewObject() {
        // 测试 new 表达式后直接使用成员访问
        TestResult result = runWithJavaConstruction(
                "size = new java.util.ArrayList().size; " +
                        "&size");
        result.assertBothEqual(0);
    }

    @Test
    public void testChainedMemberAccess() {
        // 测试链式成员访问
        TestResult result = runWithJavaConstruction(
                "new java.util.ArrayList().class.simpleName");
        result.assertBothEqual("ArrayList");
    }

    // ========== 与 Context Call (::) 集成测试 ==========

    @Test
    public void testContextCallOnNewObject() {
        // 测试 new 表达式后直接使用上下文调用
        TestResult result = runWithJavaConstruction(
                "new java.util.ArrayList()::size()");
        result.assertBothEqual(0);
    }

    @Test
    public void testChainedContextCall() {
        // 测试链式上下文调用
        TestResult result = runWithJavaConstruction(
                "new java.lang.StringBuilder('test')::toString()::uppercase()");
        result.assertBothEqual("TEST");
    }

    @Test
    public void testContextCallWithArguments() {
        // 测试带参数的上下文调用
        TestResult result = runWithJavaConstruction(
                "list = new java.util.ArrayList(); " +
                        "&list::add('item'); " +
                        "&list::get(0)");
        result.assertBothEqual("item");
    }

    // ========== Feature Flag 测试 ==========

    @Test
    public void testFeatureFlagDisabled() {
        // 测试未启用 Java 构造特性时的行为
        String source = "new java.util.ArrayList()";
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        CompilationContext ctx = new CompilationContext(source);
        // 默认不启用 Java 构造
        assertFalse(ctx.isAllowJavaConstruction());

        assertThrows(Exception.class, () -> {
            Fluxon.parse(ctx, env);
        }, "Should throw exception when Java construction is not enabled");
    }

    // ========== 构造函数重载解析测试 ==========

    @Test
    public void testConstructorOverloadResolutionEmpty() {
        // 测试无参构造函数
        TestResult result = runWithJavaConstruction(
                "new java.lang.StringBuilder()::toString()");
        result.assertBothEqual("");
    }

    @Test
    public void testConstructorOverloadResolutionString() {
        // 测试字符串参数构造函数
        TestResult result = runWithJavaConstruction(
                "new java.lang.StringBuilder('hello')::toString()");
        result.assertBothEqual("hello");
    }

    @Test
    public void testConstructorOverloadResolutionInt() {
        // 测试整数参数构造函数 (capacity)
        // StringBuilder(int) 创建指定容量的空 StringBuilder
        TestResult result = runWithJavaConstruction(
                "sb = new java.lang.StringBuilder(100); " +
                        "&sb::toString()");
        result.assertBothEqual("");  // 空字符串，但容量为100
    }

    // ========== Varargs 构造函数测试 ==========

    @Test
    public void testVarargsConstructor() {
        // 测试 varargs 构造函数 (Arrays.asList 返回的 List)
        // 注意: 这里测试 ArrayList 复制构造
        TestResult result = runWithJavaConstruction(
                "original = [1, 2, 3]; " +
                        "copy = new java.util.ArrayList(&original); " +
                        "&copy::size()");
        result.assertBothEqual(3);
    }

    // ========== 构造函数重载与 null ==========

    @Test
    public void testConstructorOverloadWithNullConsistency() {
        TestResult result = runWithJavaConstruction(
                "results = []; " +
                        "for i in 1..2 { " +
                        "  o = new org.tabooproject.fluxon.type.TestCtorOverload(null); " +
                        "  &results += &o.tag " +
                        "}; " +
                        "&results");
        result.assertBothToStringEqual("[string, string]");
    }

    // ========== 错误场景测试 ==========

    @Test
    public void testClassNotFound() {
        // 测试类不存在的情况
        assertThrows(RuntimeException.class, () -> {
            interpretWithJavaConstruction("new com.nonexistent.FakeClass()");
        }, "Should throw exception for non-existent class");
    }

    @Test
    public void testNoMatchingConstructor() {
        // 测试没有匹配构造函数的情况
        // ArrayList 没有接受两个 String 参数的构造函数
        assertThrows(RuntimeException.class, () -> {
            interpretWithJavaConstruction("new java.util.ArrayList('a', 'b')");
        }, "Should throw exception when no matching constructor found");
    }

    // ========== 复杂场景测试 ==========

    @Test
    public void testNewInExpression() {
        // 测试 new 表达式在更大表达式中的使用
        TestResult result = runWithJavaConstruction(
                "list1 = new java.util.ArrayList(); " +
                        "list2 = new java.util.ArrayList(); " +
                        "&list1::add(1); " +
                        "&list2::add(2); " +
                        "&list1::size() + &list2::size()");
        result.assertBothEqual(2);
    }

    @Test
    public void testNewInConditional() {
        // 测试条件表达式中的 new
        TestResult result = runWithJavaConstruction(
                "useList = true; " +
                        "container = if &useList then new java.util.ArrayList() else new java.util.HashMap(); " +
                        "&container::size()");
        result.assertBothEqual(0);
    }

    @Test
    public void testNewInLoop() {
        // 测试循环中的 new - 每次循环创建新的 StringBuilder
        TestResult result = runWithJavaConstruction(
                "results = []; " +
                        "for i in 1..3 { " +
                        "  sb = new java.lang.StringBuilder(&i::toString()); " +
                        "  &results += &sb::toString() " +
                        "}; " +
                        "&results");
        result.assertBothToStringEqual("[1, 2, 3]");
    }

    @Test
    public void testNewAsArgument() {
        // 测试 new 表达式作为函数参数
        TestResult result = runWithJavaConstruction(
                "def getSize(list) = &list::size(); " +
                        "getSize(new java.util.ArrayList())");
        result.assertBothEqual(0);
    }

    @Test
    public void testNestedNew() {
        // 测试嵌套的 new 表达式
        TestResult result = runWithJavaConstruction(
                "outer = new java.util.ArrayList(new java.util.ArrayList()); " +
                        "&outer::size()");
        result.assertBothEqual(0);
    }

    // ========== 边界情况测试 ==========

    @Test
    public void testNewWithNullArgument() {
        // 测试带 null 参数的构造
        // HashMap 构造函数不接受 null，但这测试参数传递
        TestResult result = runWithJavaConstruction(
                "sb = new java.lang.StringBuilder('hello'); " +
                        "&sb::toString()");
        result.assertBothEqual("hello");
    }

    @Test
    public void testNewWithExpressionArgument() {
        // 测试参数为表达式的情况
        TestResult result = runWithJavaConstruction(
                "prefix = 'Hello'; " +
                        "sb = new java.lang.StringBuilder(&prefix + ' World'); " +
                        "&sb::toString()");
        result.assertBothEqual("Hello World");
    }

    @Test
    public void testNewWithMultipleArguments() {
        // 测试多参数构造 (使用 ArrayList 的复制构造函数)
        TestResult result = runWithJavaConstruction(
                "original = [1, 2, 3]; " +
                        "copy = new java.util.ArrayList(&original); " +
                        "&copy::size()");
        result.assertBothEqual(3);
    }
}
