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

    @Test
    public void testLambdaCaptureOuterVariable() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "outer = 5; adder = |x| &x + &outer; outer = 7; call(&adder, [3])");
        assertEquals(10, result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    public void testNestedLambdaFactory() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "n = 10; makeAdder = |x| &x + &n; add10 = &makeAdder; call(&add10, [2])");
        assertEquals(12, result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    public void testLambdaEachCountInsideFunction() {
        String script = ""
                + "def countAll(list) = {\n"
                + "  counter = 0\n"
                + "  &list::each(|item| { print('counter: ' + &counter); counter += 1 })\n"
                + "  &counter\n"
                + "}\n"
                + "numbers = [1,2,3,4]; countAll(&numbers)";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(script);
        assertEquals(4, result.getInterpretResult());
        assertTrue(result.isMatch());
    }
}
