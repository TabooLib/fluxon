package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;

public class InterpreterTest {

    @Test
    public void testDef() {
        Fluxon.eval("def msg(t) = print &t; msg 嗯嗯啊啊");
    }
}
