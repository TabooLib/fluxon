package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.compiler.FluxonFeatures;

import static org.junit.jupiter.api.Assertions.*;

/**
 * For 循环表达式测试
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ForExpressionTest {

    @BeforeEach
    public void BeforeEach() {
        FluxonFeatures.DEFAULT_ALLOW_KETHER_STYLE_CALL = true;
    }

    // ========== 基础 For 循环测试 ==========

    @Test
    public void testBasicListIteration() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent(
                "result = 0; for i in [1, 2, 3] { &result += &i }; &result");
        assertEquals(6, result.getInterpretResult());
        assertEquals(6, result.getCompileResult());
    }

    @Test
    public void testEmptyListIteration() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; for i in [] { &result += 1 }; &result");
        assertEquals(0, result.getInterpretResult());
        assertEquals(0, result.getCompileResult());
    }

    @Test
    public void testRangeIteration() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent(
                "result = 0; for i in 1..4 { &result += &i }; &result");
        assertEquals(10, result.getInterpretResult());
        assertEquals(10, result.getCompileResult());
    }

    @Test
    public void testExclusiveRangeIteration() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; for i in 1..<4 { &result += &i }; &result");
        assertEquals(6, result.getInterpretResult());
        assertEquals(6, result.getCompileResult());
    }

    @Test
    public void testMapIteration() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "map = [a: 10, b: 20]; result = 0; for (k, v) in &map { &result += &v }; &result");
        assertEquals(30, result.getInterpretResult());
        assertEquals(30, result.getCompileResult());
    }

    @Test
    public void testStringIteration() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text = 'hello'; result = ''; for c in &text { &result += &c }; &result");
        assertEquals("hello", result.getInterpretResult());
        assertEquals("hello", result.getCompileResult());
    }

    // ========== For 循环中的变量引用测试 ==========

    @Test
    public void testForLoopVariableReference() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; result = []; for i in &list { &result += &i }; &result");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3]", result.getCompileResult().toString());
    }

    @Test
    public void testForLoopVariableModification() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; for i in &list { i = &i * 2 }; &list");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3]", result.getCompileResult().toString());
    }

    // ========== 嵌套 For 循环测试 ==========

    @Test
    public void testNestedForLoop() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in 1..3 { " +
                        "  for j in 1..3 { " +
                        "    result += (&i * &j) " +
                        "  } " +
                        "}; " +
                        "&result");
        assertEquals(36, result.getInterpretResult());
        assertEquals(36, result.getCompileResult());
    }

    @Test
    public void testDeepNestedForLoop() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in 1..2 { " +
                        "  for j in 1..2 { " +
                        "    for k in 1..2 { " +
                        "      result += 1 " +
                        "    } " +
                        "  } " +
                        "}; " +
                        "&result");
        assertEquals(8, result.getInterpretResult());
        assertEquals(8, result.getCompileResult());
    }

    // ========== 解构测试 ==========

    @Test
    public void testMapDestructuring() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "map = [a: 1, b: 2]; " +
                        "result = 0; " +
                        "for (key, value) in &map { " +
                        "  result += &value " +
                        "}; " +
                        "&result");
        assertEquals(3, result.getInterpretResult());
        assertEquals(3, result.getCompileResult());
    }

    @Test
    public void testListOfListsDestructuring() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [[1, 2], [3, 4], [5, 6]]; " +
                        "result = 0; " +
                        "for (first, second) in &list { " +
                        "  result += &first + &second " +
                        "}; " +
                        "&result");
        assertEquals(21, result.getInterpretResult());
        assertEquals(21, result.getCompileResult());
    }

    @Test
    public void testMultipleDestructuring() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [[1, 2, 3], [4, 5, 6]]; " +
                        "result = 0; " +
                        "for (a, b, c) in &list { " +
                        "  result += &a + &b + &c " +
                        "}; " +
                        "&result");
        assertEquals(21, result.getInterpretResult());
        assertEquals(21, result.getCompileResult());
    }

    // ========== 循环控制测试 ==========

    @Test
    public void testForLoopWithConditional() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in [1, 2, 3, 4, 5] { " +
                        "  if &i > 2 then result += &i " +
                        "}; " +
                        "&result");
        assertEquals(12, result.getInterpretResult());
        assertEquals(12, result.getCompileResult());
    }

    @Test
    public void testForLoopAccumulation() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = []; " +
                        "for i in [10, 20, 30] { " +
                        "  result += &i * 2 " +
                        "}; " +
                        "&result");
        assertEquals("[20, 40, 60]", result.getInterpretResult().toString());
        assertEquals("[20, 40, 60]", result.getCompileResult().toString());
    }

    @Test
    public void testForLoopFiltering() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = []; " +
                        "for i in [1, 2, 3, 4, 5, 6] { " +
                        "  if &i % 2 == 0 then result += &i " +
                        "}; " +
                        "&result");
        assertEquals("[2, 4, 6]", result.getInterpretResult().toString());
        assertEquals("[2, 4, 6]", result.getCompileResult().toString());
    }

    // ========== 复杂场景测试 ==========

    @Test
    public void testForLoopWithContextCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "texts = ['hello', 'world']; " +
                        "result = []; " +
                        "for t in &texts { " +
                        "  result += &t::uppercase() " +
                        "}; " +
                        "&result");
        assertEquals("[HELLO, WORLD]", result.getInterpretResult().toString());
        assertEquals("[HELLO, WORLD]", result.getCompileResult().toString());
    }

    @Test
    public void testForLoopWithIndexAccess() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "matrix = [[1, 2], [3, 4]]; " +
                        "result = 0; " +
                        "for row in &matrix { " +
                        "  result += &row[0] + &row[1] " +
                        "}; " +
                        "&result");
        assertEquals(10, result.getInterpretResult());
        assertEquals(10, result.getCompileResult());
    }

    @Test
    public void testForLoopWithCompoundAssignment() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; " +
                        "result = 1; " +
                        "for i in &list { " +
                        "  result *= &i " +
                        "}; " +
                        "&result");
        assertEquals(6, result.getInterpretResult());
        assertEquals(6, result.getCompileResult());
    }

    // ========== 外部变量访问测试 ==========

    @Test
    public void testForLoopAccessOuterVariable() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "multiplier = 10; " +
                        "result = 0; " +
                        "for i in [1, 2, 3] { " +
                        "  result += &i * &multiplier " +
                        "}; " +
                        "&result");
        assertEquals(60, result.getInterpretResult());
        assertEquals(60, result.getCompileResult());
    }

    @Test
    public void testForLoopModifyOuterVariable() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "total = 0; " +
                        "for i in 1..5 { " +
                        "  total += &i " +
                        "}; " +
                        "&total");
        assertEquals(15, result.getInterpretResult());
        assertEquals(15, result.getCompileResult());
    }

    // ========== 边界情况测试 ==========

    @Test
    public void testForLoopSingleElement() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in [42] { " +
                        "  result += &i " +
                        "}; " +
                        "&result");
        assertEquals(42, result.getInterpretResult());
        assertEquals(42, result.getCompileResult());
    }

    @Test
    public void testForLoopLargeCollection() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in 1..100 { " +
                        "  result += &i " +
                        "}; " +
                        "&result");
        assertEquals(5050, result.getInterpretResult());
        assertEquals(5050, result.getCompileResult());
    }

    @Test
    public void testForLoopNegativeRange() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in -3..-1 { " +
                        "  result += &i " +
                        "}; " +
                        "&result");
        assertEquals(-6, result.getInterpretResult());
        assertEquals(-6, result.getCompileResult());
    }

    @Test
    public void testForLoopReverseRange() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in 5..1 { " +
                        "  result += &i " +
                        "}; " +
                        "&result");
        assertEquals(15, result.getInterpretResult());
        assertEquals(15, result.getCompileResult());
    }

    // ========== 集合操作测试 ==========

    @Test
    public void testForLoopListBuilding() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = []; " +
                        "for i in 1..5 { " +
                        "  result += &i " +
                        "}; " +
                        "&result");
        assertEquals("[1, 2, 3, 4, 5]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3, 4, 5]", result.getCompileResult().toString());
    }

    @Test
    public void testForLoopMapBuilding() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = [:]; " +
                        "for i in [1, 2, 3] { " +
                        "  &result['key' + &i::toString()] = &i * 10 " +
                        "}; " +
                        "&result");
        assertNotNull(result.getInterpretResult());
        assertNotNull(result.getCompileResult());
    }

    @Test
    public void testForLoopStringBuilding() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = ''; " +
                        "for i in [1, 2, 3] { " +
                        "  result += &i::toString() " +
                        "}; " +
                        "&result");
        assertEquals("123", result.getInterpretResult());
        assertEquals("123", result.getCompileResult());
    }

    // ========== 复杂嵌套测试 ==========

    @Test
    public void testComplexNestedStructure() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "data = [[1, 2], [3, 4], [5, 6]]; " +
                        "result = 0; " +
                        "for row in &data { " +
                        "  for col in &row { " +
                        "    result += &col " +
                        "  } " +
                        "}; " +
                        "&result");
        assertEquals(21, result.getInterpretResult());
        assertEquals(21, result.getCompileResult());
    }

    @Test
    public void testForLoopWithBreakSimulation() {
        // 使用条件语句模拟 break 的效果
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = []; " +
                        "for i in [1, 2, 3, 4, 5] { " +
                        "  if &i > 3 then { result } else { result += &i } " +
                        "}; " +
                        "&result");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3]", result.getCompileResult().toString());
    }
}