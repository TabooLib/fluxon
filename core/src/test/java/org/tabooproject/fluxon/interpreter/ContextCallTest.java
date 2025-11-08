package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.runtime.error.ArgumentTypeMismatchError;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 上下文调用功能测试
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ContextCallTest {

    // ========== 基础上下文调用测试 ==========

    // 1755611940830L = 2025-08-19 21:59:00
    @Test
    public void testBasicContextCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:time'; time :: formatTimestamp(1755611940830L)");
        assertEquals("2025-08-19 21:59:00", result.getInterpretResult());
        assertEquals("2025-08-19 21:59:00", result.getCompileResult());
    }

    @Test
    public void testChainedContextCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:time'; time :: formatTimestamp(1755611940830L) :: split('-')");
        assertEquals("[2025, 08, 19 21:59:00]", result.getInterpretResult().toString());
        assertEquals("[2025, 08, 19 21:59:00]", result.getCompileResult().toString());
    }

    @Test
    public void testContextCallInLoop() {
        // print 和 random 不会被视为扩展函数
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:time'; " +
                "i = 0; " +
                "while (&i < 3) { " +
                "  time :: formatTimestamp(1755611940830L) :: split('-'); " +
                "  i += random(); " +
                "}; " +
                "&i");
        assertTrue((Number) result.getInterpretResult() instanceof Number);
        assertTrue((Number) result.getCompileResult() instanceof Number);
        assertTrue(((Number) result.getInterpretResult()).doubleValue() >= 3);
        assertTrue(((Number) result.getCompileResult()).doubleValue() >= 3);
    }

    @Test
    public void testContextCallWithClosure() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("day = 1; &day");
        assertEquals(1, result.getInterpretResult());
        assertEquals(1, result.getCompileResult());

        // Test closure context
        FluxonTestUtil.runSilent("import 'fs:time'; time :: { time = 10 }");
    }

    @Test
    public void testReferenceWithContextCall() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("list = [1,2,3]; &list::contains(1)");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("list = [1,2,3]; !&list::contains(1)");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());
    }

    // ========== 操作符优先级测试 ==========

    @Test
    public void testOperatorPrecedence() {
        FluxonTestUtil.TestResult result;

        // 测试 ! 和 :: 的优先级
        result = FluxonTestUtil.runSilent("list = [1,2,3]; !&list::contains(1)");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());

        result = FluxonTestUtil.runSilent("list = [1,2,3]; !&list::contains(4)");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        // 测试 & 和 :: 的结合
        result = FluxonTestUtil.runSilent("list = [1,2,3]; &list::contains(2)");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("list = [1,2,3]; &list::contains(5)");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());
    }

    @Test
    public void testComplexPrecedence() {
        FluxonTestUtil.TestResult result;

        // 测试复杂的优先级组合
        result = FluxonTestUtil.runSilent("list = [1,2,3]; !&list::contains(1) && true");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());

        result = FluxonTestUtil.runSilent("list = [1,2,3]; !&list::contains(4) || false");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        // 测试多重上下文调用
        result = FluxonTestUtil.runSilent("text = 'hello'; &text::uppercase()");
        assertEquals("HELLO", result.getInterpretResult());
        assertEquals("HELLO", result.getCompileResult());

        result = FluxonTestUtil.runSilent("text = 'hello'; &text::length()");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());

        // 测试引用后的多重操作
        result = FluxonTestUtil.runSilent("text = 'hello'; &text::uppercase()::substring(0, 2)");
        assertEquals("HE", result.getInterpretResult());
        assertEquals("HE", result.getCompileResult());
    }

    // ========== 一元运算符测试 ==========

    @Test
    public void testUnaryWithReference() {
        FluxonTestUtil.TestResult result;

        // 测试负号与引用
        result = FluxonTestUtil.runSilent("num = 5; -&num");
        assertEquals(-5, result.getInterpretResult());
        assertEquals(-5, result.getCompileResult());

        result = FluxonTestUtil.runSilent("num = -5; -&num");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());
    }

    // ========== 混合运算符测试 ==========

    @Test
    public void testMixedOperators() {
        FluxonTestUtil.TestResult result;

        // 测试算术运算符与引用
        result = FluxonTestUtil.runSilent("a = 3; b = 7; &a + &b");
        assertEquals(10, result.getInterpretResult());
        assertEquals(10, result.getCompileResult());

        result = FluxonTestUtil.runSilent("a = 3; b = 7; &a - &b");
        assertEquals(-4, result.getInterpretResult());
        assertEquals(-4, result.getCompileResult());

        result = FluxonTestUtil.runSilent("a = 3; b = 7; &a * &b");
        assertEquals(21, result.getInterpretResult());
        assertEquals(21, result.getCompileResult());

        // 测试比较运算符与引用
        result = FluxonTestUtil.runSilent("a = 3; b = 7; &a < &b");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("a = 3; b = 7; &a > &b");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());

        result = FluxonTestUtil.runSilent("a = 3; b = 3; &a == &b");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    // ========== 表达式中的引用测试 ==========

    @Test
    public void testReferenceInExpressions() {
        FluxonTestUtil.TestResult result;

        // 测试引用在括号表达式中
        result = FluxonTestUtil.runSilent("list = [1,2,3]; !(&list::contains(1))");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());

        result = FluxonTestUtil.runSilent("list = [1,2,3]; !(!&list::contains(1))");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        // 测试引用在条件表达式中
        result = FluxonTestUtil.runSilent("list = [1,2,3]; if &list::contains(1) then 3 else 5");
        assertEquals(3, result.getInterpretResult());
        assertEquals(3, result.getCompileResult());

        result = FluxonTestUtil.runSilent("list = [1,2,3]; if &list::contains(4) then 3 else 5");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());
    }

    // ========== 上下文调用链测试 ==========

    @Test
    public void testContextCallChaining() {
        FluxonTestUtil.TestResult result;

        // 测试连续的上下文调用
        result = FluxonTestUtil.runSilent("list = [1,2,3]; &list::size()");
        assertEquals(3, result.getInterpretResult());
        assertEquals(3, result.getCompileResult());

        result = FluxonTestUtil.runSilent("list = [1,2,3]; &list::size()::toString()");
        assertEquals("3", result.getInterpretResult());
        assertEquals("3", result.getCompileResult());

        // 测试上下文调用与其他操作符的组合
        result = FluxonTestUtil.runSilent("list = [1,2,3]; &list::size() * 2");
        assertEquals(6, result.getInterpretResult());
        assertEquals(6, result.getCompileResult());

        result = FluxonTestUtil.runSilent("list = [1,2,3]; &list::size() > 2");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    // ========== Elvis 操作符测试 ==========

    @Test
    public void testElvisWithReference() {
        FluxonTestUtil.TestResult result;

        // 测试 Elvis 操作符与引用
        result = FluxonTestUtil.runSilent("a = 5; &a ?: 10");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());

        result = FluxonTestUtil.runSilent("a = null; &a ?: 10");
        assertEquals(10, result.getInterpretResult());
        assertEquals(10, result.getCompileResult());

        // 测试 Elvis 与上下文调用
        result = FluxonTestUtil.runSilent("list = [1,2,3]; &list::contains(1) ?: false");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("list = null; (&list ?: [1]) :: contains(1)");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    // ========== 赋值与引用测试 ==========

    @Test
    public void testAssignmentWithReference() {
        FluxonTestUtil.TestResult result;

        // 测试赋值与引用的组合
        result = FluxonTestUtil.runSilent("a = 5; b = &a * 2; &b");
        assertEquals(10, result.getInterpretResult());
        assertEquals(10, result.getCompileResult());

        result = FluxonTestUtil.runSilent("a = 5; a += &a * 2; &a");
        assertEquals(15, result.getInterpretResult());
        assertEquals(15, result.getCompileResult());

        // 测试复合赋值
        result = FluxonTestUtil.runSilent("a = 5; a *= &a; &a");
        assertEquals(25, result.getInterpretResult());
        assertEquals(25, result.getCompileResult());
    }

    // ========== 边界情况测试 ==========

    @Test
    public void testEdgeCases() {
        FluxonTestUtil.TestResult result;

        // 测试空集合
        result = FluxonTestUtil.runSilent("list = []; !&list::isEmpty()");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());

        result = FluxonTestUtil.runSilent("list = []; &list::isEmpty()");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        // 测试可选引用 - &?unknownVar 不会抛出异常
        result = FluxonTestUtil.runSilent("&?unknownVar");
        assertEquals(null, result.getInterpretResult());
        assertEquals(null, result.getCompileResult());

        // 测试多重否定
        result = FluxonTestUtil.runSilent("list = [1,2,3]; !!&list::contains(1)");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent("list = [1,2,3]; !!!&list::contains(1)");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());
    }

    // ========== 全局函数调用测试 ==========

    @Test
    public void testGlobalFunctionCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("[1]::get(g::random(1))");
        assertEquals(1, result.getInterpretResult());
        assertEquals(1, result.getCompileResult());
    }

    // ========== 错误处理测试 ==========

    @Test
    public void testArgumentTypeMismatch() {
        try {
            FluxonTestUtil.runSilent("[1]::get(random(1))");
            fail("Should throw ArgumentTypeMismatchException");
        } catch (ArgumentTypeMismatchError e) {
            assertEquals("Argument 0 expect Number but got ArrayList ([1])", e.getMessage());
        }
    }

    @Test
    public void testParameterTypes() {
        try {
            FluxonTestUtil.runSilent("env()::function(0)");
            fail("Should throw ArgumentTypeMismatchException");
        } catch (ArgumentTypeMismatchError e) {
            assertEquals("Argument 0 expect String but got Integer (0)", e.getMessage());
        }
    }

    // ========== 复杂场景测试 ==========

    @Test
    public void testComplexChaining() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("text = 'hello world'; &text::uppercase()::split(' ')");
        assertEquals("[HELLO, WORLD]", result.getInterpretResult().toString());
        assertEquals("[HELLO, WORLD]", result.getCompileResult().toString());
    }

    @Test
    public void testNestedContextCalls() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [[1, 2], [3, 4]]; &list::get(0)::get(1)");
        assertEquals(2, result.getInterpretResult());
        assertEquals(2, result.getCompileResult());
    }

    @Test
    public void testContextCallWithArithmetic() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list1 = [1, 2, 3]; list2 = [4, 5]; &list1::size() + &list2::size()");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());
    }

    @Test
    public void testContextCallInCondition() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; if &list::size() > 2 then 'large' else 'small'");
        assertEquals("large", result.getInterpretResult());
        assertEquals("large", result.getCompileResult());
    }

    @Test
    public void testOptionalReferenceWithContextCall() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("&?unknownList ?: []");
        assertEquals("[]", result.getInterpretResult().toString());
        assertEquals("[]", result.getCompileResult().toString());

        result = FluxonTestUtil.runSilent("list = [1, 2]; (&?list ?: [])::size()");
        assertEquals(2, result.getInterpretResult());
        assertEquals(2, result.getCompileResult());
    }
}
