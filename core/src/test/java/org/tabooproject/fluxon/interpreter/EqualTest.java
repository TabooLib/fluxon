package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.parser.ParseException;
import org.tabooproject.fluxon.runtime.stdlib.Operations;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class EqualTest {

    @Test
    public void test() {
        System.out.println(Fluxon.parse("-1.0"));
        System.out.println(Fluxon.parse("-0.0"));
        System.out.println(Fluxon.parse("-0"));
        assertEquals(true, Operations.isEqual(-0.0, 0.0));
        assertEquals(true, Fluxon.eval("1 == 1.0"));
        assertEquals(true, Fluxon.eval("-0.0 == 0.0"));
    }
}
