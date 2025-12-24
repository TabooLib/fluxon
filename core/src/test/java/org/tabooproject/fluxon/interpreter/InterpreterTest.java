package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.Fluxon;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class InterpreterTest {

    @Test
    public void testDef() {
        assertEquals("你好世界", Fluxon.eval("def msg(t) = &t + 世界; msg(你好)"));
    }

    @Test
    public void test1_2() {
        assertEquals(-1, Fluxon.eval("1-2"));
    }
}
