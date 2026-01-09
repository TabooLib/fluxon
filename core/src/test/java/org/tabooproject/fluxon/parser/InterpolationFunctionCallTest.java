package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试字符串插值中的各种函数调用场景
 */
public class InterpolationFunctionCallTest {

    @Test
    @DisplayName("测试插值中直接调用自定义函数")
    void testInterpolationWithDirectFunctionCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def foo() = 'hello'; \"Result: ${foo()}\""
        );
        assertEquals("Result: hello", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试插值中调用带参数的函数")
    void testInterpolationWithFunctionCallWithArgs() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def add(a, b) = &a + &b; \"Sum: ${add(1, 2)}\""
        );
        assertEquals("Sum: 3", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试插值中调用标准库函数")
    void testInterpolationWithStdlibFunction() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"Max: ${max(10, 20)}\""
        );
        assertEquals("Max: 20", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试插值中嵌套函数调用")
    void testInterpolationWithNestedFunctionCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def double(x) = &x * 2; \"Result: ${double(double(5))}\""
        );
        assertEquals("Result: 20", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试插值中函数调用与表达式组合")
    void testInterpolationWithFunctionCallAndExpression() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def foo() = 10; \"Result: ${foo() + 5}\""
        );
        assertEquals("Result: 15", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试插值中包含双引号字符串参数的函数调用")
    void testInterpolationWithNestedDoubleQuoteString() {
        // 模拟用户场景：外层双引号字符串，插值内部也有双引号字符串参数
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def player(name) = &name; def isPersistent(p, key) = true; " +
                "\"isPersistent=${isPersistent(player(\"test\"), \"key1\")}\""
        );
        assertEquals("isPersistent=true", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试插值中包含单引号字符串参数的函数调用")
    void testInterpolationWithSingleQuoteStringArg() {
        // 使用单引号作为内部字符串应该可以工作
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def player(name) = &name; def isPersistent(p, key) = true; " +
                "\"isPersistent=${isPersistent(player('test'), 'key1')}\""
        );
        assertEquals("isPersistent=true", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试嵌套字符串插值")
    void testNestedStringInterpolation() {
        // Kotlin 风格的嵌套插值
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 'inner'; \"outer ${\"nested ${&x}\"}\""
        );
        assertEquals("outer nested inner", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试多层嵌套字符串插值")
    void testDeeplyNestedStringInterpolation() {
        // 三层嵌套
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"a${\"b${\"c\"}\"}\""
        );
        assertEquals("abc", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试同一字符串中多个插值")
    void testMultipleInterpolationsInSameString() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "entry = 'test'; \"Entry: ${&entry}, entry=${&entry}\""
        );
        assertEquals("Entry: test, entry=test", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试函数参数中多个插值的字符串")
    void testMultipleInterpolationsInFunctionArg() {
        // 模拟 print("Entry: ${&entry}, entry=${&entry}") 场景
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def echo(s) = &s; entry = 'test'; echo(\"Entry: ${&entry}, entry=${&entry}\")"
        );
        assertEquals("Entry: test, entry=test", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    // ==================== 边界测试 ====================

    @Test
    @DisplayName("插值在字符串开头")
    void testInterpolationAtStart() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 'hello'; \"${&x} world\""
        );
        assertEquals("hello world", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值在字符串结尾")
    void testInterpolationAtEnd() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 'world'; \"hello ${&x}\""
        );
        assertEquals("hello world", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("字符串只有插值没有其他文本")
    void testOnlyInterpolation() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 'hello'; \"${&x}\""
        );
        assertEquals("hello", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("连续插值无中间文本")
    void testConsecutiveInterpolations() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = 'hello'; b = 'world'; \"${&a}${&b}\""
        );
        assertEquals("helloworld", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("三个连续插值")
    void testThreeConsecutiveInterpolations() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = 'a'; b = 'b'; c = 'c'; \"${&a}${&b}${&c}\""
        );
        assertEquals("abc", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("多个插值混合文本")
    void testManyInterpolationsWithText() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = 1; b = 2; c = 3; d = 4; \"a=${&a}, b=${&b}, c=${&c}, d=${&d}\""
        );
        assertEquals("a=1, b=2, c=3, d=4", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值内空表达式-null")
    void testInterpolationWithNull() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"value: ${null}\""
        );
        assertEquals("value: null", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值内布尔值")
    void testInterpolationWithBoolean() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"true=${true}, false=${false}\""
        );
        assertEquals("true=true, false=false", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值内数字运算")
    void testInterpolationWithArithmetic() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"result: ${1 + 2 * 3}\""
        );
        assertEquals("result: 7", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值内三元表达式")
    void testInterpolationWithTernary() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 10; \"${&x > 5 ? 'big' : 'small'}\""
        );
        assertEquals("big", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值内列表访问")
    void testInterpolationWithListAccess() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "arr = [1, 2, 3]; \"second: ${&arr[1]}\""
        );
        assertEquals("second: 2", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值内Map访问")
    void testInterpolationWithMapAccess() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "m = [name: 'test']; \"name: ${&m['name']}\""
        );
        assertEquals("name: test", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值与转义$组合")
    void testInterpolationWithEscapedDollar() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 100; \"price: \\${&x} = ${&x}\""
        );
        assertEquals("price: ${&x} = 100", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值内包含大括号的Map字面量")
    void testInterpolationWithMapLiteral() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"map: ${[a: 1, b: 2]}\""
        );
        assertNotNull(result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值内包含大括号的代码块")
    void testInterpolationWithCodeBlock() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"result: ${if true { 'yes' } else { 'no' }}\""
        );
        assertEquals("result: yes", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("嵌套插值内多个插值")
    void testNestedInterpolationWithMultiple() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = 'x'; b = 'y'; \"outer ${\"inner: ${&a}, ${&b}\"}\""
        );
        assertEquals("outer inner: x, y", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("单引号外层双引号内层插值")
    void testSingleQuoteOuterDoubleInner() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 'test'; 'value: ${\"nested: ${&x}\"}'"
        );
        assertEquals("value: nested: test", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("双引号外层单引号内层插值")
    void testDoubleQuoteOuterSingleInner() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 'test'; \"value: ${'nested: ${&x}'}\""
        );
        assertEquals("value: nested: test", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("函数调用带多个字符串参数")
    void testFunctionWithMultipleStringArgs() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def concat(a, b, c) = &a + &b + &c; \"${concat(\"x\", \"y\", \"z\")}\""
        );
        assertEquals("xyz", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("复杂嵌套：函数内插值字符串带函数调用")
    void testComplexNestedFunctionCalls() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def wrap(s) = '[' + &s + ']'; def greet(name) = 'Hello ' + &name; " +
                "\"result: ${wrap(\"${greet(\"World\")}\")}\"" 
        );
        assertEquals("result: [Hello World]", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值内调用存储的lambda")
    void testInterpolationWithLambda() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "fn = |x| &x * 2; \"result: ${call(&fn, [5])}\""
        );
        assertEquals("result: 10", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值内扩展函数调用")
    void testInterpolationWithExtensionFunction() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"length: ${'hello'::length()}\""
        );
        assertEquals("length: 5", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值内链式扩展函数调用")
    void testInterpolationWithChainedExtension() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"result: ${'  hello  '::trim()::length()}\""
        );
        assertEquals("result: 5", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("空字符串插值")
    void testEmptyStringInterpolation() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "s = ''; \"value: ${&s}!\""
        );
        assertEquals("value: !", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值字符串作为Map值")
    void testInterpolationStringAsMapValue() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 'test'; m = [key: \"value: ${&x}\"]; &m['key']"
        );
        assertEquals("value: test", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值字符串作为列表元素")
    void testInterpolationStringAsListElement() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 'test'; arr = [\"a: ${&x}\", \"b: ${&x}\"]; &arr[0] + ', ' + &arr[1]"
        );
        assertEquals("a: test, b: test", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("多层嵌套四层")
    void testFourLevelNesting() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"1${\"2${\"3${\"4\"}\"}\"}\""
        );
        assertEquals("1234", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("插值内Elvis操作符")
    void testInterpolationWithElvis() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = null; \"value: ${&?x ?: 'default'}\""
        );
        assertEquals("value: default", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("复杂实际场景：日志格式")
    void testRealWorldLogging() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def echo(s) = &s; level = 'INFO'; msg = 'test'; code = 200; " +
                "echo(\"[${&level}] ${&msg} (code=${&code})\")"
        );
        assertEquals("[INFO] test (code=200)", result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    @DisplayName("复杂实际场景：SQL拼接")
    void testRealWorldSql() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "table = 'users'; id = 123; name = 'test'; " +
                "\"SELECT * FROM ${&table} WHERE id = ${&id} AND name = '${&name}'\""
        );
        assertEquals("SELECT * FROM users WHERE id = 123 AND name = 'test'", result.getInterpretResult());
        assertTrue(result.isMatch());
    }
}
