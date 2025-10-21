package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.compiler.FluxonFeatures;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 函数调用测试
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class FunctionCallTest {

    @BeforeEach
    public void BeforeEach() {
        FluxonFeatures.DEFAULT_ALLOW_KETHER_STYLE_CALL = true;
    }

    // ========== 基础函数调用测试 ==========

    @Test
    public void testZeroParamFunctionWithOperator() {
        // 测试 0 参数函数后面跟操作符
        // now 返回当前时间戳
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "start = now; " +
                        "end = now + 1000; " +
                        "diff = &end - &start; " +
                        "&diff");
        // 应该返回 1000
        assertEquals(1000L, result.getInterpretResult());
        assertEquals(1000L, result.getCompileResult());
    }

    @Test
    public void testFunctionWithExpressionArg() {
        // 测试函数参数是表达式
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = 5; " +
                        "b = 3; " +
                        "&a - &b");
        assertEquals(2, result.getInterpretResult());
        assertEquals(2, result.getCompileResult());
    }

    @Test
    public void testSimpleFunction() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def inc(x) = &x + 1; " +
                        "inc 5");
        assertEquals(6, result.getInterpretResult());
        assertEquals(6, result.getCompileResult());
    }

    @Test
    public void testFunctionWithMultipleParams() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def add(x, y) = &x + &y; " +
                        "add 3 4");
        assertEquals(7, result.getInterpretResult());
        assertEquals(7, result.getCompileResult());
    }

    // ========== 表达式参数测试 ==========

    @Test
    public void testMultipleArgsWithExpressions() {
        // 测试多个参数，其中包含表达式
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def max(x, y) = if &x > &y then &x else &y; " +
                        "a = 10; " +
                        "b = 5; " +
                        "max(&a + &b, &a * &b)");
        // max(15, 50) = 50
        assertEquals(50, result.getInterpretResult());
        assertEquals(50, result.getCompileResult());
    }

    @Test
    public void testNoParenthesesCallInExpression() {
        // 测试表达式中的无括号函数调用
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def double(x) = &x * 2; " +
                        "a = 5; " +
                        "double (&a + 3)");
        // double(5 + 3) = 8 * 2 = 16
        assertEquals(16, result.getInterpretResult());
        assertEquals(16, result.getCompileResult());
    }

    // ========== 链式调用测试 ==========

    @Test
    public void testChainedCalls() {
        // 测试链式调用
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def inc(x) = &x + 1; " +
                        "def double(x) = &x * 2; " +
                        "double (inc 5)");
        // double(inc(5)) = double(6) = 12
        assertEquals(12, result.getInterpretResult());
        assertEquals(12, result.getCompileResult());
    }

    @Test
    public void testNestedFunctionCalls() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def add(x, y) = &x + &y; " +
                        "def mul(x, y) = &x * &y; " +
                        "add(mul 2 3, mul 4 5)");
        // add(2*3, 4*5) = add(6, 20) = 26
        assertEquals(26, result.getInterpretResult());
        assertEquals(26, result.getCompileResult());
    }

    // ========== 引用函数测试 ==========

    @Test
    public void testRefCall1() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def test = 'Hello'; " +
                        "call(&test)");
        assertEquals("Hello", result.getInterpretResult());
        assertEquals("Hello", result.getCompileResult());
    }

    @Test
    public void testRefCall2() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def inc(x) = &x + 1; " +
                        "ref = &inc; " +
                        "call(&ref, [1])");
        assertEquals(2, result.getInterpretResult());
        assertEquals(2, result.getCompileResult());
    }

    // ========== 函数返回值测试 ==========

    @Test
    public void testFunctionWithReturnValue() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def getValue = 42; " +
                        "getValue");
        assertEquals(42, result.getInterpretResult());
        assertEquals(42, result.getCompileResult());
    }

    @Test
    public void testFunctionWithConditionalReturn() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent(
                "def abs(x) = if &x < 0 then -&x else &x; " +
                        "abs -5");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());

        result = FluxonTestUtil.runSilent(
                "def abs(x) = if &x < 0 then -&x else &x; " +
                        "abs 5");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());
    }

    // ========== 参数绑定测试 ==========

    @Test
    public void testParameterBinding() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def test(a, b, c) = &a + &b + &c; " +
                        "test 1 2 3");
        assertEquals(6, result.getInterpretResult());
        assertEquals(6, result.getCompileResult());
    }

    @Test
    public void testParameterWithSameName() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 100; " +
                        "def func(x) = &x * 2; " +
                        "result = func 5; " +
                        "[&x, &result]");
        assertEquals("[100, 10]", result.getInterpretResult().toString());
        assertEquals("[100, 10]", result.getCompileResult().toString());
    }

    // ========== 变量作用域测试 ==========

    @Test
    public void testFunctionAccessOuterVariable() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "outer = 10; " +
                        "def func(x) = &x + &outer; " +
                        "func 5");
        assertEquals(15, result.getInterpretResult());
        assertEquals(15, result.getCompileResult());
    }

    @Test
    public void testFunctionWithLocalVariable() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def func(x) = { y = &x * 2; &y + 1 }; " +
                        "func 5");
        assertEquals(11, result.getInterpretResult());
        assertEquals(11, result.getCompileResult());
    }

    // ========== 复杂场景测试 ==========

    @Test
    public void testFunctionInExpression() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def inc(x) = &x + 1; " +
                        "(inc 5) * (inc 3)");
        // (5+1) * (3+1) = 6 * 4 = 24
        assertEquals(24, result.getInterpretResult());
        assertEquals(24, result.getCompileResult());
    }

    @Test
    public void testFunctionInConditional() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def isEven(x) = &x % 2 == 0; " +
                        "if isEven 4 then 'yes' else 'no'");
        assertEquals("yes", result.getInterpretResult());
        assertEquals("yes", result.getCompileResult());
    }

    @Test
    public void testFunctionInLoop() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def square(x) = &x * &x; " +
                        "result = []; " +
                        "for i in 1..5 { &result += square &i }; " +
                        "&result");
        assertEquals("[1, 4, 9, 16, 25]", result.getInterpretResult().toString());
        assertEquals("[1, 4, 9, 16, 25]", result.getCompileResult().toString());
    }

    @Test
    public void testFunctionWithContextCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def getName = 'hello'; " +
                        "getName::uppercase()");
        assertEquals("HELLO", result.getInterpretResult());
        assertEquals("HELLO", result.getCompileResult());
    }

    // ========== 函数返回集合测试 ==========

    @Test
    public void testFunctionReturnsList() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def getList = [1, 2, 3]; " +
                        "getList");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3]", result.getCompileResult().toString());
    }

    @Test
    public void testFunctionReturnsMap() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def getMap = [a: 1, b: 2]; " +
                        "map = getMap; " +
                        "&map['a']");
        assertEquals(1, result.getInterpretResult());
        assertEquals(1, result.getCompileResult());
    }

    // ========== 递归函数测试 ==========

    @Test
    public void testSimpleRecursion() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def fib(n) = if &n <= 1 then &n else { fib(&n - 1) + fib(&n - 2); }; " +
                        "fib 6");
        assertEquals(8, result.getInterpretResult());
        // TODO assertEquals(8, result.getCompileResult());
        // 已知问题：递归函数暂时无法处理局部变量
    }

    @Test
    public void testFactorial() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def fact(n) = if &n <= 1 then 1 else &n * (fact (&n - 1)); " +
                        "fact 5");
        assertEquals(120, result.getInterpretResult());
        assertEquals(120, result.getCompileResult());
    }

    // ========== 边界情况测试 ==========

    @Test
    public void testFunctionWithZeroArguments() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def getConstant = 42; " +
                        "getConstant");
        assertEquals(42, result.getInterpretResult());
        assertEquals(42, result.getCompileResult());
    }

    @Test
    public void testFunctionWithNullReturn() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def returnNull = null; " +
                        "returnNull");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());
    }

    @Test
    public void testFunctionWithBooleanReturn() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent(
                "def isTrue = true; " +
                        "isTrue");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent(
                "def isFalse = false; " +
                        "isFalse");
    }
}