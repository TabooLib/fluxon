package org.tabooproject.fluxon.interpreter.customized;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.tabooproject.fluxon.FluxonTestUtil.*;

/**
 * 简单测试，随便写写
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class SimpleTest {

    @Test
    public void testDef() {
        FluxonTestUtil.TestResult result = runSilent("def msg(t) = &t + 世界; msg(你好)");
        assertBothEqual("你好世界", result);
    }

    @Test
    public void test1_2() {
        FluxonTestUtil.TestResult result = runSilent("1-2");
        assertBothEqual(-1, result);
    }
}
