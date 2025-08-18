package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeTest {

    @Test
    public void testTime() {
        Fluxon.eval("print(time)");
        Fluxon.eval("print(time::formatTimestamp(now))");
    }
}
