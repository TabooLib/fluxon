package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BreakContinueTest {

    @Test
    public void testBreak() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("result = 'fail'\n" +
                "for i in 1..10 {\n" +
                "    if &i == 5 {\n" +
                "        result = 'ok'\n" +
                "        break\n" +
                "    }\n" +
                "    print &i\n" +
                "}\n" +
                "&result");
        assertEquals("ok", testResult.getInterpretResult());
        assertEquals("ok", testResult.getCompileResult());
    }

    @Test
    public void testContinue() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("output = ''\n" +
                "for i in 1..10 {\n" +
                "    if &i % 2 == 0 {\n" +
                "        continue\n" +
                "    }\n" +
                "    output = &output + &i\n" +
                "}\n" +
                "&output");
        assertEquals("13579", testResult.getInterpretResult());
        assertEquals("13579", testResult.getCompileResult());
    }

    @Test
    public void testNestedContinue() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("output = ''\n" +
                "for i in 1..3 {\n" +
                "    for j in 1..5 {\n" +
                "        if &j % 2 == 0 {\n" +
                "            continue\n" +
                "        }\n" +
                "        output = &output + &j + ','\n" +
                "    }\n" +
                "}\n" +
                "&output");
        assertEquals("1,3,5,1,3,5,1,3,5,", testResult.getInterpretResult());
        assertEquals("1,3,5,1,3,5,1,3,5,", testResult.getCompileResult());
    }

    @Test
    public void testBreakInWhileLoop() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("output = ''\n" +
                "count = 0\n" +
                "while true {\n" +
                "    output = &output + &count + ','\n" +
                "    count = &count + 1\n" +
                "    if &count >= 5 {\n" +
                "        break\n" +
                "    }\n" +
                "}\n" +
                "&output");
        assertEquals("0,1,2,3,4,", testResult.getInterpretResult());
        assertEquals("0,1,2,3,4,", testResult.getCompileResult());
    }

    @Test
    public void testContinueInWhileLoop() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("output = ''\n" +
                "count = 0\n" +
                "while &count < 10 {\n" +
                "    count = &count + 1\n" +
                "    if &count % 2 == 0 {\n" +
                "        continue\n" +
                "    }\n" +
                "    output = &output + &count + ','\n" +
                "}\n" +
                "&output");
        assertEquals("1,3,5,7,9,", testResult.getInterpretResult());
        assertEquals("1,3,5,7,9,", testResult.getCompileResult());
    }

    @Test
    public void testElvisBreak() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("output = ''\n" +
                "for i in 1..3 {\n" +
                "    null ?: break\n" +
                "    output = 'should-not-run'\n" +
                "}\n" +
                "&output");
        assertEquals("", testResult.getInterpretResult());
        assertEquals("", testResult.getCompileResult());
    }

    @Test
    public void testElvisContinue() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("output = ''\n" +
                "for i in 1..3 {\n" +
                "    null ?: continue\n" +
                "    output = &output + 'x'\n" +
                "}\n" +
                "&output");
        assertEquals("", testResult.getInterpretResult());
        assertEquals("", testResult.getCompileResult());
    }

    @Test
    public void testElvisBreakBlock() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("output = ''\n" +
                "for i in 1..3 {\n" +
                "    null ?: { output = 'block'; break }\n" +
                "    output = 'should-not-run'\n" +
                "}\n" +
                "&output");
        assertEquals("block", testResult.getInterpretResult());
        assertEquals("block", testResult.getCompileResult());
    }

    @Test
    public void testElvisReturn() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("def foo() = {\n" +
                "    null ?: return 'ok'\n" +
                "    return 'fail'\n" +
                "}\n" +
                "foo()");
        assertEquals("ok", testResult.getInterpretResult());
        assertEquals("ok", testResult.getCompileResult());
    }

    @Test
    public void testElvisReturnNextLine() {
        // elvis + return 后下一行不应被返回
        FluxonTestUtil.TestResult r1 = FluxonTestUtil.runSilent("def foo(x) = {\n" +
                "    a = &x ?: return\n" +
                "    'next_line'\n" +
                "}\n" +
                "foo(null)");
        // null ?: return 应该提前返回 null，不应该返回 'next_line'
        assertEquals(null, r1.getInterpretResult(), "elvis+return should return null, not next line");
        assertEquals(null, r1.getCompileResult(), "elvis+return should return null, not next line (compile)");
    }

    @Test
    public void testElvisReturnValueNextLine() {
        // elvis + return 带值，下一行不应影响返回值
        FluxonTestUtil.TestResult r2 = FluxonTestUtil.runSilent("def foo(x) = {\n" +
                "    a = &x ?: return 'early'\n" +
                "    'should_not_reach'\n" +
                "}\n" +
                "foo(null)");
        assertEquals("early", r2.getInterpretResult(), "elvis+return with value should return 'early'");
        assertEquals("early", r2.getCompileResult(), "elvis+return with value should return 'early' (compile)");
    }

    @Test
    public void testElvisReturnNonNull() {
        // 非 null 时 elvis 右侧不执行
        FluxonTestUtil.TestResult r3 = FluxonTestUtil.runSilent("def foo(x) = {\n" +
                "    a = &x ?: return 'early'\n" +
                "    &a + '!'\n" +
                "}\n" +
                "foo('hello')");
        assertEquals("hello!", r3.getInterpretResult());
        assertEquals("hello!", r3.getCompileResult());
    }

    @Test
    public void testElvisBreakNextLine() {
        // elvis + break 后下一行不应被跳过
        FluxonTestUtil.TestResult r = FluxonTestUtil.runSilent("output = ''\n" +
                "for i in 1..3 {\n" +
                "    null ?: break\n" +
                "    output = &output + 'x'\n" +
                "}\n" +
                "&output");
        assertEquals("", r.getInterpretResult(), "elvis+break should exit loop immediately");
        assertEquals("", r.getCompileResult(), "elvis+break should exit loop immediately (compile)");
    }

    @Test
    public void testElvisContinueNextLine() {
        // elvis + continue 后下一行仍应执行（下一轮循环）
        FluxonTestUtil.TestResult r = FluxonTestUtil.runSilent("output = ''\n" +
                "for i in 1..3 {\n" +
                "    null ?: continue\n" +
                "    output = &output + &i\n" +
                "}\n" +
                "&output");
        assertEquals("", r.getInterpretResult(), "elvis+continue should skip rest of loop body");
        assertEquals("", r.getCompileResult(), "elvis+continue should skip rest of loop body (compile)");
    }

    @Test
    public void testElvisReturnSameLine() {
        // return 值在同一行时应正确解析
        FluxonTestUtil.TestResult r = FluxonTestUtil.runSilent("def foo(x) = {\n" +
                "    a = &x ?: return 'fallback'\n" +
                "    &a\n" +
                "}\n" +
                "foo(null)");
        assertEquals("fallback", r.getInterpretResult());
        assertEquals("fallback", r.getCompileResult());
    }

    @Test
    public void testBreakWithCondition() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("result = 'continue'\n" +
                "sum = 0\n" +
                "for i in 1..100 {\n" +
                "    sum = &sum + &i\n" +
                "    if &sum > 50 {\n" +
                "        result = 'threshold-reached'\n" +
                "        break\n" +
                "    }\n" +
                "}\n" +
                "&result");
        assertEquals("threshold-reached", testResult.getInterpretResult());
        assertEquals("threshold-reached", testResult.getCompileResult());
    }

    @Test
    public void testComplexContinueCase() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("output = ''\n" +
                "for i in 1..5 {\n" +
                "    if &i % 2 == 0 {\n" +
                "        output = &output + &i + ':skipped,'\n" +
                "        continue\n" +
                "    }\n" +
                "    output = &output + &i + ':odd,'\n" +
                "}\n" +
                "&output");
        assertEquals("1:odd,2:skipped,3:odd,4:skipped,5:odd,", testResult.getInterpretResult());
        assertEquals("1:odd,2:skipped,3:odd,4:skipped,5:odd,", testResult.getCompileResult());
    }

    @Test
    public void testBreakInWhen() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("output = ''\n" +
                "for i in 1..5 {\n" +
                "    when &i {\n" +
                "        3 -> break\n" +
                "        else -> output = &output + &i\n" +
                "    }\n" +
                "}\n" +
                "&output");
        assertEquals("12", testResult.getInterpretResult());
        assertEquals("12", testResult.getCompileResult());
    }

    @Test
    public void testContinueInWhen() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("output = ''\n" +
                "for i in 1..5 {\n" +
                "    when &i {\n" +
                "        3 -> continue\n" +
                "        else -> output = &output + &i\n" +
                "    }\n" +
                "}\n" +
                "&output");
        assertEquals("1245", testResult.getInterpretResult());
        assertEquals("1245", testResult.getCompileResult());
    }

    @Test
    public void testBreakInWhenWithCondition() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("output = ''\n" +
                "for i in 1..10 {\n" +
                "    when {\n" +
                "        &i > 5 -> break\n" +
                "        else -> output = &output + &i\n" +
                "    }\n" +
                "}\n" +
                "&output");
        assertEquals("12345", testResult.getInterpretResult());
        assertEquals("12345", testResult.getCompileResult());
    }
}
