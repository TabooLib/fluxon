package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.Fluxon;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class TryTest {

    @Test
    public void testThrow() {
        try {
            Fluxon.eval("throw('error')");
            throw new IllegalStateException("Expected exception not thrown");
        } catch (RuntimeException e) {
            assertEquals("error", e.getMessage());
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
        assertEquals("java.lang.RuntimeException: error", Fluxon.eval("try throw('error') catch (e) &e").toString());
    }
}
