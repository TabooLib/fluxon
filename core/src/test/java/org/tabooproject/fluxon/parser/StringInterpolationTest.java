package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字符串插值集成测试
 */
public class StringInterpolationTest {

    @Test
    @DisplayName("测试简单字符串插值")
    void testSimpleInterpolation() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "name = 'World'; \"Hello ${&name}!\""
        );
        assertEquals("Hello World!", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试多个插值")
    void testMultipleInterpolations() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "first = 'John'; last = 'Doe'; \"${&first} ${&last}\""
        );
        assertEquals("John Doe", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试插值内包含表达式")
    void testInterpolationWithExpression() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = 10; b = 20; \"Sum: ${&a + &b}\""
        );
        assertEquals("Sum: 30", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试插值内包含函数调用")
    void testInterpolationWithFunctionCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 'hello'; \"Length: ${&x::length()}\""
        );
        assertEquals("Length: 5", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试空插值表达式")
    void testInterpolationWithNull() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = null; \"Value: ${&?x}\""
        );
        assertEquals("Value: null", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试无插值字符串保持向后兼容")
    void testPlainStringBackwardCompatible() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"Hello World\""
        );
        assertEquals("Hello World", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试转义 $ 符号")
    void testEscapedDollarSign() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"Price: \\${100}\""
        );
        assertEquals("Price: ${100}", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试插值内包含三元表达式")
    void testInterpolationWithTernary() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 10; status = &x > 5 ? 'big' : 'small'; \"Status: ${&status}\""
        );
        assertEquals("Status: big", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }

    @Test
    @DisplayName("测试单引号字符串插值")
    void testSingleQuoteInterpolation() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "name = 'Test'; 'Hello ${&name}'"
        );
        assertEquals("Hello Test", result.getInterpretResult());
        assertTrue(result.isMatch(), "解释器和编译器结果应一致");
    }
}
