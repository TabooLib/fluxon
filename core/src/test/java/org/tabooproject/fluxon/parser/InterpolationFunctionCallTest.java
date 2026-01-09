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
}
