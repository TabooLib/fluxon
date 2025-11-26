package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TypeTest {

    @Test
    public void testNumberCheck() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("isNumber(123)");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    @Test
    public void testArrayCheck() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("isArray(array([]))");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    @Test
    public void testListCheck() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("isList([])");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    @Test
    public void testMapCheck() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("isMap([:])");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }
}
