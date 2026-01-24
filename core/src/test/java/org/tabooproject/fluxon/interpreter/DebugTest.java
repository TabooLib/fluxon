package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class DebugTest {

    @Test
    public void testGlobalVarMul() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 6; " +
                "&x * 2"
        );
        System.out.println("Global: &x * 2 Interpret result: " + result.getInterpretResult());
        System.out.println("Global: &x * 2 Compile result: " + result.getCompileResult());
        assertEquals(12, result.getInterpretResult());
        assertEquals(12, result.getCompileResult());
    }

    @Test
    public void testFunctionMul() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def double(x) = &x * 2; " +
                "double(6)"
        );
        System.out.println("Function: double(6) Interpret result: " + result.getInterpretResult());
        System.out.println("Function: double(6) Compile result: " + result.getCompileResult());
        assertEquals(12, result.getInterpretResult());
        assertEquals(12, result.getCompileResult());
    }
}
