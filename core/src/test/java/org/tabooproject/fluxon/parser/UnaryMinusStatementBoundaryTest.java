package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 一元负号语句边界测试
 *
 * @author sky
 */
public class UnaryMinusStatementBoundaryTest {

    @Test
    public void testUnaryMinusAfterPrintNewline() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "total = 10.0\n" +
                        "print(\"最终数值 ${&total * -1}\")\n" +
                        "-&total"
        );
        assertEquals(-10.0, result.getInterpretResult());
    }
}
