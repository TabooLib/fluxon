package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;

public class TimeTest {

    @Test
    public void testTime() {
        Fluxon.eval("import 'fs:time'; print(time())");
        Fluxon.eval("import 'fs:time'; print(time::formatTimestamp(now()))");
    }
}
