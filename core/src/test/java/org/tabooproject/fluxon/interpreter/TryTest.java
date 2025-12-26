package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.FluxonTestUtil.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class TryTest {

    @Test
    public void testThrow() {
        interpretExpectingError("throw('error')", "error");
    }

    @Test
    public void testTry1() {
        // try without catch should suppress exception and return null
        FluxonTestUtil.TestResult result = runSilent("try throw('error')");
        assertMatch(result);
    }

    @Test
    public void testTry2() {
        FluxonTestUtil.TestResult result = runSilent("try throw('error') catch 'ok'");
        assertBothEqual("ok", result);
    }

    @Test
    public void testTry3() {
        FluxonTestUtil.TestResult result = runSilent("try throw('error') catch 'ok' finally print('ok')");
        assertBothEqual("ok", result);
    }

    @Test
    public void testTry4() {
        FluxonTestUtil.TestResult result = runSilent("try throw('error') catch (e) &e");
        assertTrue(result.getInterpretResult().toString().contains("error"));
        assertTrue(result.getCompileResult().toString().contains("error"));
        // 不使用 assertMatch，因为异常对象是不同实例
    }

    @Test
    public void testAsyncError() {
        FluxonTestUtil.TestResult result = runSilent(
                "async test() = throw('error')\n" +
                "test()");
        assertInstanceOf(CompletableFuture.class, result.getInterpretResult());
        assertInstanceOf(CompletableFuture.class, result.getCompileResult());
        // 不使用 assertMatch，因为 CompletableFuture 是不同实例
    }
}
