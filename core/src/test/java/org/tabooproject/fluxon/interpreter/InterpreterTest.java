package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.FluxonFeatures;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class InterpreterTest {

    @BeforeEach
    public void BeforeEach() {
        FluxonFeatures.DEFAULT_ALLOW_KETHER_STYLE_CALL = true;
    }

    @Test
    public void testDef() {
        Fluxon.eval("def msg(t) = print &t; msg 嗯嗯啊啊");
    }
}
