package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.FluxonFeatures;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 函数调用测试
 *
 * @author sky
 */
public class FunctionCallTest {

    @BeforeAll
    public static void beforeAll() {
        FluxonFeatures.DEFAULT_ALLOW_KETHER_STYLE_CALL = true;
    }

    @Test
    public void testZeroParamFunctionWithOperator() {
        // 测试 0 参数函数后面跟操作符
        // now 返回当前时间戳
        Object result = Fluxon.eval("start = now\n" +
                "end = now + 1000\n" +
                "diff = &end - &start\n" +
                "return &diff");
        // 应该返回 1000
        assertEquals(1000L, result);
    }

    @Test
    public void testFunctionWithExpressionArg() {
        // 测试函数参数是表达式
        Object result = Fluxon.eval("a = 5\n" +
                "b = 3\n" +
                "// print 应该接收 a - b 的结果作为参数\n" +
                "print &a - &b\n" +
                "&a - &b");
        assertEquals(2, result);
    }

    @Test
    public void testMultipleArgsWithExpressions() {
        // 测试多个参数，其中包含表达式
        Object result = Fluxon.eval("def max(x, y) = { if (&x > &y) &x else &y }\n" +
                "a = 10\n" +
                "b = 5\n" +
                "result = max &a + &b &a * &b\n" +
                "&result");
        // max(15, 50) = 50
        assertEquals(50, result);
    }

    @Test
    public void testNoParenthesesCallInExpression() {
        // 测试表达式中的无括号函数调用
        Object result = Fluxon.eval("def double(x) = &x * 2\n" +
                "a = 5\n" +
                "result = double &a + 3\n" +
                "&result");
        // double(5 + 3) = 8 * 2 = 16
        assertEquals(16, result);
    }

    @Test
    public void testChainedCalls() {
        // 测试链式调用
        Object result = Fluxon.eval("def inc(x) = &x + 1\n" +
                "def double(x) = &x * 2\n" +
                "result = double inc 5\n" +
                "&result");
        // double(inc(5)) = double(6) = 12
        assertEquals(12, result);
    }

    @Test
    public void testRefCall1() {
        Object result = Fluxon.eval("def test = \"Hello\"\n" +
                "call(&test)");
        assertEquals("Hello", result);
    }

    @Test
    public void testRefCall2() {
        Object result = Fluxon.eval("def inc(x) = &x + 1\n" +
                "ref = &inc\n" +
                "call(&ref, [1])");
        assertEquals(2, result);
    }
}