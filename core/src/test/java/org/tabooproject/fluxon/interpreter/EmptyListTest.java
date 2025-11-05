package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmptyListTest {

    @Test
    public void testEmptyListLiteral() {
        // 测试独立的空列表字面量
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("[]");
        assertTrue(testResult.getInterpretResult() instanceof java.util.List);
        assertTrue(((java.util.List<?>) testResult.getInterpretResult()).isEmpty());
        assertTrue(testResult.getCompileResult() instanceof java.util.List);
        assertTrue(((java.util.List<?>) testResult.getCompileResult()).isEmpty());
    }

    @Test
    public void testEmptyListAfterFunctionCall() {
        // 测试函数调用后跟空列表（在新行）
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent(
                "def test() = { return 1 }\n" +
                "test()\n" +
                "[]");
        assertTrue(testResult.getInterpretResult() instanceof java.util.List);
        assertTrue(((java.util.List<?>) testResult.getInterpretResult()).isEmpty());
        assertTrue(testResult.getCompileResult() instanceof java.util.List);
        assertTrue(((java.util.List<?>) testResult.getCompileResult()).isEmpty());
    }

    @Test
    public void testMethodCallWithIndexAccess() {
        // 测试方法调用后跟索引访问
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent(
                "field = 'a\"b\"c\"d\"e'\n" +
                "&field::split(\"\\\"\")[3]");
        assertEquals("d", testResult.getInterpretResult());
        assertEquals("d", testResult.getCompileResult());
    }

    @Test
    public void testFunctionReturningListWithIndex() {
        // 测试返回列表的函数后跟索引访问
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent(
                "def getList() = { return [1, 2, 3, 4, 5] }\n" +
                "getList()[3]");
        assertEquals(4, testResult.getInterpretResult());
        assertEquals(4, testResult.getCompileResult());
    }
}

