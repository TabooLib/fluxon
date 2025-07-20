package org.tabooproject.fluxon.interpreter

import org.junit.jupiter.api.Test
import org.tabooproject.fluxon.Fluxon

class TimeTest {

    @Test
    fun testFormatDateTime() {
        Fluxon.eval("print time")
        Fluxon.eval("print time::formatDateTime(now)")
    }
}