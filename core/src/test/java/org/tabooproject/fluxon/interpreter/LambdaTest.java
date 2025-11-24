package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LambdaTest {

    @Test
    public void testSimpleLambdaCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("inc = |x| &x + 1; call(&inc, [5])");
        assertEquals(6, result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    public void testZeroArgumentLambda() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("producer = || 42; call(&producer)");
        assertEquals(42, result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    public void testLambdaWithIterableMap() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("list = [1, 2, 3]; doubled = &list::map(|x| &x * 2); &doubled");
        assertEquals(Arrays.asList(2, 4, 6), result.getInterpretResult());
        assertTrue(result.isMatch());
    }
}

