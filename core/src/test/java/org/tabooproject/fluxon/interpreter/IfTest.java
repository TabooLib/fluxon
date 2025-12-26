package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.tabooproject.fluxon.FluxonTestUtil.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class IfTest {

    @Test
    public void testIfBranchBreak() {
        // Test break inside if-else within for loop
        FluxonTestUtil.TestResult result = runSilent("for i in 1..10 then { if &i == 6 break else print(&i) }");
        assertMatch(result);
    }

    @Test
    public void testSimpleIfTrue() {
        FluxonTestUtil.TestResult result = runSilent("if true then 'yes' else 'no'");
        assertBothEqual("yes", result);
    }

    @Test
    public void testSimpleIfFalse() {
        FluxonTestUtil.TestResult result = runSilent("if false then 'yes' else 'no'");
        assertBothEqual("no", result);
    }

    @Test
    public void testIfWithComparison() {
        FluxonTestUtil.TestResult result = runSilent("x = 10; if &x > 5 then 'big' else 'small'");
        assertBothEqual("big", result);
    }

    @Test
    public void testNestedIf() {
        FluxonTestUtil.TestResult result = runSilent("x = 10; if &x > 5 then { if &x > 15 then 'very big' else 'medium' } else 'small'");
        assertBothEqual("medium", result);
    }

    @Test
    public void testIfWithoutElse() {
        FluxonTestUtil.TestResult result = runSilent("x = 10; if &x > 5 then 'big'");
        assertBothEqual("big", result);
    }

    @Test
    public void testIfWithBlock() {
        FluxonTestUtil.TestResult result = runSilent("x = 0; if true then { x = 1; x = &x + 1 }; &x");
        assertBothEqual(2, result);
    }
}
