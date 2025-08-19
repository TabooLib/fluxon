package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.FluxonRuntimeTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContextCallTest {

    // 1755611940830L = 2025-08-19 21:59:00
    @Test
    public void test1() {
        assertEquals("2025-08-19 21:59:00", Fluxon.eval("time :: formatTimestamp(1755611940830L)"));
    }

    @Test
    public void test2() {
        assertEquals("[2025, 08, 19 21:59:00]", Fluxon.eval("time :: formatTimestamp(1755611940830L) :: split('-')").toString());
    }

    @Test
    public void test3() {
        // 检索扩展函数: formatTimestamp
        // 检索扩展函数: split
        // 0
        // 0.9793372836056653
        // 1.266626930227011
        // 2.0234217076733807
        // 2.9088529523673623
        // 3.267841899894786
        // print 和 random 不会被视为扩展函数
        Fluxon.eval("i = 0" +
                "while (&i < 3) { " +
                "  time :: formatTimestamp(1755611940830L) :: split('-') " +
                "  print &i" +
                "  i += random()" +
                "}" +
                "print &i");
    }

    @Test
    public void test4() {
        assertEquals(1, Fluxon.eval("day = 1; &day"));
        Fluxon.eval("time :: { time = 10 ; print &time; print day }");
    }
}
