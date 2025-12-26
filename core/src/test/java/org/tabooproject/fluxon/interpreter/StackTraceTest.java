package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.tabooproject.fluxon.FluxonTestUtil.assertBothEqual;

/**
 * 堆栈跟踪测试类
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class StackTraceTest {

    @Test
    public void testBreakWithParentheses() {
        // 测试使用 break() 语法（解析器会报错 "Expected expression"）
        FluxonTestUtil.runExpectingError(
                "result = 'fail'\n" +
                        "for i in 1..10 {\n" +
                        "    if &i == 5 {\n" +
                        "        result = 'ok'\n" +
                        "        break()\n" +
                        "    }\n" +
                        "    print &i\n" +
                        "}\n" +
                        "&result",
                "Expected expression");
    }

    @Test
    public void testBreakStatement() {
        // 测试正确的 break 语句
        assertBothEqual("ok",
                FluxonTestUtil.runSilent(
                        "result = 'fail'\n" +
                                "for i in 1..10 {\n" +
                                "    if &i == 5 {\n" +
                                "        result = 'ok'\n" +
                                "        break\n" +
                                "    }\n" +
                                "}\n" +
                                "&result"));
    }

    @Test
    public void testMethodCallOnStringInLoop() {
        // 测试循环中对字符串调用方法会抛出异常（clear 不是字符串方法）
        FluxonTestUtil.runExpectingError(
                "result = 'fail'\n" +
                        "for i in 1..10 {\n" +
                        "    if &i == 10 {\n" +
                        "        result = 'ok'\n" +
                        "        &result::clear()\n" +
                        "        break\n" +
                        "    }\n" +
                        "}\n" +
                        "&result",
                "clear");
    }
}
