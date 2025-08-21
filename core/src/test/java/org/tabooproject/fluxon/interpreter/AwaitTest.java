package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;

import static org.junit.jupiter.api.Assertions.*;

public class AwaitTest {

    @Test
    public void testBasicAsync() {
        // 测试最基本的异步函数
        Object result = Fluxon.eval("async def test = { return 42 } await test()");
        assertEquals(42, result);
    }

    @Test
    public void testAsyncWithSleep() {
        // 测试带 sleep 的异步函数
        Object result = Fluxon.eval("async def test = { sleep(10) return 'ok' } await test()");
        assertEquals("ok", result);
    }

    @Test
    public void testAwaitNonAsync() {
        // 测试 await 非异步值
        Object result = Fluxon.eval("await 123");
        assertEquals(123, result);
    }
}
