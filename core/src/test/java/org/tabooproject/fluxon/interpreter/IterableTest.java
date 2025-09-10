package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IterableTest {

    @Test
    public void testIterableFirst() {
        assertEquals(1, Fluxon.eval("list = [1,2,3]; &list::first()"));
    }

    @Test
    public void testIterableLast() {
        assertEquals(3, Fluxon.eval("list = [1,2,3]; &list::last()"));
    }

    @Test
    public void testIterableTake() {
        assertEquals("[]", Fluxon.eval("list = [1,2,3]; &list::take(0)").toString());
        assertEquals("[1]", Fluxon.eval("list = [1,2,3]; &list::take(1)").toString());
        assertEquals("[1, 2]", Fluxon.eval("list = [1,2,3]; &list::take(2)").toString());
        assertEquals("[1, 2, 3]", Fluxon.eval("list = [1,2,3]; &list::take(3)").toString());
    }

    @Test
    public void testIterableDrop() {
        assertEquals("[1, 2, 3]", Fluxon.eval("list = [1,2,3]; &list::drop(0)").toString());
        assertEquals("[2, 3]", Fluxon.eval("list = [1,2,3]; &list::drop(1)").toString());
        assertEquals("[3]", Fluxon.eval("list = [1,2,3]; &list::drop(2)").toString());
        assertEquals("[]", Fluxon.eval("list = [1,2,3]; &list::drop(3)").toString());
    }

    @Test
    public void testIterableTakeLast() {
        assertEquals("[]", Fluxon.eval("list = [1,2,3]; &list::takeLast(0)").toString());
        assertEquals("[3]", Fluxon.eval("list = [1,2,3]; &list::takeLast(1)").toString());
        assertEquals("[2, 3]", Fluxon.eval("list = [1,2,3]; &list::takeLast(2)").toString());
        assertEquals("[1, 2, 3]", Fluxon.eval("list = [1,2,3]; &list::takeLast(3)").toString());
    }

    @Test
    public void testIterableDropLast() {
        assertEquals("[1, 2, 3]", Fluxon.eval("list = [1,2,3]; &list::dropLast(0)").toString());
        assertEquals("[1, 2]", Fluxon.eval("list = [1,2,3]; &list::dropLast(1)").toString());
        assertEquals("[1]", Fluxon.eval("list = [1,2,3]; &list::dropLast(2)").toString());
        assertEquals("[]", Fluxon.eval("list = [1,2,3]; &list::dropLast(3)").toString());
    }
}
