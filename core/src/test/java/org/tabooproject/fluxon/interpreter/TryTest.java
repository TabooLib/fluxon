package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.FluxonFeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TryTest {

    @BeforeAll
    public static void beforeAll() {
        FluxonFeatures.DEFAULT_ALLOW_KETHER_STYLE_CALL = true;
    }

    @Test
    public void testThrow() {
        try {
            Fluxon.eval("throw 'error'");
            throw new IllegalStateException("Expected exception not thrown");
        } catch (RuntimeException e) {
            assertEquals("error", e.getMessage());
        }
    }

    @Test
    public void testTry1() {
        Fluxon.eval("try throw 'error'");
    }

    @Test
    public void testTry2() {
        assertEquals("ok", Fluxon.eval("try throw 'error' catch 'ok'"));
    }

    @Test
    public void testTry3() {
        assertEquals("ok", Fluxon.eval("try throw 'error' catch 'ok' finally print 'ok'"));
    }

    @Test
    public void testTry4() {
        assertEquals("java.lang.RuntimeException: error", Fluxon.eval("try throw 'error' catch (e) &e").toString());
    }
}
