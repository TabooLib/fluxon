package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.type.TestRuntime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 语句边界与换行续链测试
 *
 * @author sky
 */
public class StatementBoundaryLineContinuationTest {

    @BeforeEach
    public void BeforeEach() {
        TestRuntime.registerTestFunctions();
    }

    @Test
    public void testUnaryMinusStartsNewStatementAfterNewline() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "total = 10.0\n" +
                        "print(\"最终数值 ${&total * -1}\")\n" +
                        "-&total"
        );
        assertEquals(-10.0, result.getInterpretResult());
        assertEquals(-10.0, result.getCompileResult());
    }

    @Test
    public void testMinusDoesNotContinuePreviousNumberAfterNewline() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "10.0\n" +
                        "-2.0"
        );
        assertEquals(-2.0, result.getInterpretResult());
        assertEquals(-2.0, result.getCompileResult());
    }

    @Test
    public void testMemberAccessCanContinueAfterNewline() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "value = &text\n" +
                        "    .trim()\n" +
                        "    .toString()\n" +
                        "&value",
                ctx -> {},
                env -> env.defineRootVariable("text", " abc ")
        );
        assertEquals("abc", result.getInterpretResult());
        assertEquals("abc", result.getCompileResult());
    }

    @Test
    public void testSafeMemberAccessCanContinueAfterNewline() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "value = &text\n" +
                        "    ?.trim()\n" +
                        "&value",
                ctx -> {},
                env -> env.defineRootVariable("text", null)
        );
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());
    }

    @Test
    public void testContextCallCanContinueAfterNewline() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "value = \" abc \"\n" +
                        "    ::trim()\n" +
                        "    ::uppercase()\n" +
                        "&value"
        );
        assertEquals("ABC", result.getInterpretResult());
        assertEquals("ABC", result.getCompileResult());
    }

    @Test
    public void testSafeContextCallCanContinueAfterNewline() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "value = null\n" +
                        "    ?::trim()\n" +
                        "&value"
        );
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());
    }
}
