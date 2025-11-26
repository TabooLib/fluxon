package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.FluxonTestUtil;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class TryTest {

    @Test
    public void testThrow() {
        try {
            Fluxon.eval("throw('error')");
            throw new IllegalStateException("Expected exception not thrown");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("error"));
        }
    }

    @Test
    public void testTry1() {
        Fluxon.eval("try throw('error')");
    }

    @Test
    public void testTry2() {
        assertEquals("ok", Fluxon.eval("try throw('error') catch 'ok'"));
    }

    @Test
    public void testTry3() {
        assertEquals("ok", Fluxon.eval("try throw('error') catch 'ok' finally print('ok')"));
    }

    @Test
    public void testTry4() {
        assertTrue(Fluxon.eval("try throw('error') catch (e) &e").toString().contains("error"));
    }

    @Test
    public void testAsyncError() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                        "async test() = throw('error')\n" +
                        "test()");
        assertInstanceOf(CompletableFuture.class, result.getCompileResult());
        assertInstanceOf(CompletableFuture.class, result.getCompileResult());
    }
}
