package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class AwaitTest {

    @Test
    public void testBasicAsync() {
        // 测试最基本的异步函数
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("async def test = { return 42 } await test()");
        assertEquals(42, result.getInterpretResult());
        assertEquals(42, result.getCompileResult());
    }

    @Test
    public void testAsyncWithSleep() {
        // 测试带 sleep 的异步函数
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("async def test = { sleep(10) return 'ok' } await test()");
        assertEquals("ok", result.getInterpretResult());
        assertEquals("ok", result.getCompileResult());
    }

    @Test
    public void testAwaitNonAsync() {
        // 测试 await 非异步值
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("await 123");
        assertEquals(123, result.getInterpretResult());
        assertEquals(123, result.getCompileResult());
    }

    @Test
    public void testAwaitSync() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("sync def test = { return 42 } await test()");
        assertEquals(42, result.getInterpretResult());
        assertEquals(42, result.getCompileResult());
    }

    @Test
    public void testSyncWithSleep() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("sync def test = { sleep(10) return 'ok' } await test()");
        assertEquals("ok", result.getInterpretResult());
        assertEquals("ok", result.getCompileResult());
    }
}
