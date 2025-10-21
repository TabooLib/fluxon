package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 相等性测试
 * 测试不同类型值的相等性比较
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class EqualTest {

    // ========== 基础类型相等性测试 ==========

    @Test
    public void testIntegerEquality() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("1 == 1");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("1 == 2");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());
    }

    @Test
    public void testDoubleEquality() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("1.0 == 1.0");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("1.5 == 1.5");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("1.0 == 2.0");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());
    }

    @Test
    public void testStringEquality() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("'hello' == 'hello'");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("'hello' == 'world'");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());

        result = FluxonTestUtil.runSilent("'' == ''");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    @Test
    public void testBooleanEquality() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("true == true");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("false == false");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("true == false");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());
    }

    @Test
    public void testNullEquality() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("null == null");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("null == 0");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());
    }

    // ========== 数值类型转换相等性测试 ==========

    @Test
    public void testIntegerAndDoubleEquality() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("1 == 1.0");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("5 == 5.0");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("1 == 1.5");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());
    }

    @Test
    public void testNegativeNumberEquality() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("-1 == -1");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("-1.0 == -1.0");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("-1 == 1");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());
    }

    @Test
    public void testZeroEquality() {
        FluxonTestUtil.TestResult result;

        // 测试 -0.0 == 0.0
        result = FluxonTestUtil.runSilent("-0.0 == 0.0");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        // 测试 -0 == 0
        result = FluxonTestUtil.runSilent("-0 == 0");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        // 测试 0.0 == 0
        result = FluxonTestUtil.runSilent("0.0 == 0");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    // ========== 集合相等性测试 ==========

    @Test
    public void testListEquality() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("[1, 2, 3] == [1, 2, 3]");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("[1, 2, 3] == [1, 2, 4]");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());

        result = FluxonTestUtil.runSilent("[] == []");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    @Test
    public void testMapEquality() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("[a: 1, b: 2] == [a: 1, b: 2]");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("[a: 1, b: 2] == [a: 1, b: 3]");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());

        result = FluxonTestUtil.runSilent("[:] == [:]");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    @Test
    public void testNestedCollectionEquality() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("[[1, 2], [3, 4]] == [[1, 2], [3, 4]]");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("[data: [a: 1]] == [data: [a: 1]]");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    // ========== 不相等测试 ==========

    @Test
    public void testNotEqual() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("1 != 2");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("1 != 1");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());

        result = FluxonTestUtil.runSilent("'a' != 'b'");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    // ========== 引用相等性测试 ==========

    @Test
    public void testReferenceEquality() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("a = 5; b = 5; &a == &b");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("a = 5; b = 10; &a == &b");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());
    }

    @Test
    public void testIndexAccessEquality() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("list = [1, 2, 3]; &list[0] == 1");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("map = [name: 'test']; &map['name'] == 'test'");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    // ========== 复杂表达式相等性测试 ==========

    @Test
    public void testExpressionEquality() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("1 + 2 == 3");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("10 - 5 == 5");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("2 * 3 == 6");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    @Test
    public void testStringConcatenationEquality() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("'hello' + ' ' + 'world' == 'hello world'");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    // ========== 条件表达式中的相等性测试 ==========

    @Test
    public void testEqualityInCondition() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("if 1 == 1 then 'yes' else 'no'");
        assertEquals("yes", result.getInterpretResult());
        assertEquals("yes", result.getCompileResult());

        result = FluxonTestUtil.runSilent("if 1 == 2 then 'yes' else 'no'");
        assertEquals("no", result.getInterpretResult());
        assertEquals("no", result.getCompileResult());
    }

    @Test
    public void testEqualityInLoop() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "sum = 0; i = 0; " +
                        "while &i < 3 { " +
                        "  if &i == 1 then sum += 10 else sum += &i; " +
                        "  i += 1 " +
                        "}; &sum");
        assertEquals(12, result.getInterpretResult());
        assertEquals(12, result.getCompileResult());
    }

    // ========== 类型混合相等性测试 ==========

    @Test
    public void testMixedTypeInequality() {
        FluxonTestUtil.TestResult result;

        // 不同类型通常不相等
        result = FluxonTestUtil.runSilent("1 == 'hello'");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());

        result = FluxonTestUtil.runSilent("[] == [:]");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());
    }

    // ========== 逻辑表达式中的相等性测试 ==========

    @Test
    public void testEqualityInLogicalExpression() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("1 == 1 && 2 == 2");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("1 == 1 || 2 == 3");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("1 == 2 && 2 == 2");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());
    }
}
