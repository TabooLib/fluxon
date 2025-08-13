package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.FluxonRuntimeTest;

public class ContextCallTest {

    @Test
    public void testDef() {
        FluxonRuntimeTest.registerTestFunctions();
        System.out.println(Fluxon.eval("time :: day"));;
        System.out.println(Fluxon.eval("time :: { time = 10 ; print &time; print day }"));;
        System.out.println(Fluxon.eval("day = 1; &day"));
    }
}
