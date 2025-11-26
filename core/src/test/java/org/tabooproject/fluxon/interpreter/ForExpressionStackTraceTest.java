package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * For 循环表达式测试
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ForExpressionStackTraceTest {

    // @Test
    public void testError() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; for i in 1..10 { if &i == 1 then throw(1) }; &result"
        );
    }
}