package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.parser.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ImportTest {

    @Test
    public void test1() {
        try {
            Fluxon.eval("time :: formatTimestamp(1755611940830L)");
            throw new IllegalStateException("Failure");
        } catch (ParseException ex) {
            assertEquals(true, ex.getMessage().contains("not found"));
        }
    }

    @Test
    public void test2() {
        assertEquals("2025-08-19 21:59:00", Fluxon.eval("import 'fs:time'; time :: formatTimestamp(1755611940830L)"));
    }
}
