package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.tabooproject.fluxon.FluxonTestUtil.assertBothEqual;

/**
 * Import 语句测试类
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ImportTest {

    @Test
    public void testImportNotFoundFunction() {
        // 测试未导入时函数不可用（错误消息包含命名空间 "time"）
        FluxonTestUtil.interpretExpectingError("time :: formatTimestamp(1755611940830L)", "time");
    }

    @Test
    public void testImportTimeModule() {
        // 测试导入后函数可用
        assertBothEqual("2025-08-19 21:59:00", FluxonTestUtil.runSilent("import 'fs:time'; time :: formatTimestamp(1755611940830L)"));
    }
}
