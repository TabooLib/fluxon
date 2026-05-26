package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 错误传播运算符 ? 测试
 *
 * @author sky
 */
public class ErrorPropagationTest {

    @Test
    public void testNullPropagation() {
        // null? 应从函数返回 null
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def foo() = {\n" +
                "    x = null?\n" +
                "    'should_not_reach'\n" +
                "}\n" +
                "foo()");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());
    }

    @Test
    public void testNonNullPassThrough() {
        // 非 null 值应正常通过
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def foo() = {\n" +
                "    x = 42?\n" +
                "    &x\n" +
                "}\n" +
                "foo()");
        assertEquals(42, result.getInterpretResult());
    }

    @Test
    public void testStringPassThrough() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def foo() = {\n" +
                "    x = 'hello'?\n" +
                "    &x\n" +
                "}\n" +
                "foo()");
        assertEquals("hello", result.getInterpretResult());
    }

    @Test
    public void testTopLevelNullPropagation() {
        // 顶层脚本中 null? 应返回 null，不能走 FunctionContext 路径
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = null?\n" +
                "'should_not_reach'");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());
    }

    @Test
    public void testTopLevelNonNullPassThrough() {
        // 顶层脚本中非 null 值应正常通过
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 42?\n" +
                "&x");
        FluxonTestUtil.assertBothEqual(42, result);
    }

    @Test
    public void testTopLevelNullPropagationWithAssignment() {
        // 顶层赋值中使用 ? 操作符，值为 null 时整个脚本返回 null
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = 'hello'\n" +
                "b = null?\n" +
                "&a");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());
    }
}
