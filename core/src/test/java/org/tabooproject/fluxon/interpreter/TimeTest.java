package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.tabooproject.fluxon.FluxonTestUtil.assertMatch;

/**
 * 时间函数测试类
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class TimeTest {

    @Test
    public void testTimeFunction() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("import 'fs:time'; time()");
        assertNotNull(result.getInterpretResult());
        assertNotNull(result.getCompileResult());
        assertMatch(result);
    }

    @Test
    public void testFormatTimestamp() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:time'; time::formatTimestamp(1755611940830L)");
        assertNotNull(result.getInterpretResult());
        assertNotNull(result.getCompileResult());
        assertMatch(result);
    }

    @Test
    public void testNowFunction() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("import 'fs:time'; now()");
        assertNotNull(result.getInterpretResult());
        assertNotNull(result.getCompileResult());
        // now() 返回时间戳，解释和编译执行时间不同，所以不能断言相等
    }
}
