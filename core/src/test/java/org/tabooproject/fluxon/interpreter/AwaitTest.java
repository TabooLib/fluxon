package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;

import static org.junit.jupiter.api.Assertions.*;

public class AwaitTest {

    @Test
    public void test1() {
        assertEquals("ok", Fluxon.eval("async def test = { sleep 1000 return ok } await test"));
    }

    @Test
    public void test2() {
        assertEquals("ok", Fluxon.eval("async def test = { sleep 1000 return ok }\n" +
                "result = test\n" +
                "print Executable\n" +
                "await &result"));
    }
}
