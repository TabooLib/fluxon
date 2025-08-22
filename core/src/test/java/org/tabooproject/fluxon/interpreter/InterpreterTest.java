package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.FluxonFeatures;

public class InterpreterTest {

    @BeforeAll
    public static void beforeAll() {
        FluxonFeatures.DEFAULT_ALLOW_KETHER_STYLE_CALL = true;
    }

    @Test
    public void testDef() {
        Fluxon.eval("def msg(t) = print &t; msg 嗯嗯啊啊");
    }
}
