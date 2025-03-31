package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BreakContinueTest {

    @Test
    public void testBreak() {
        assertEquals("ok", Fluxon.eval("result = 'fail'\n" +
                "for i in 1..10 {\n" +
                "    if &i == 5 {\n" +
                "        result = 'ok'\n" +
                "        break\n" +
                "    }\n" +
                "    print &i\n" +
                "}\n" +
                "&result"));
    }

    @Test
    public void testContinue() {
        assertEquals("13579", Fluxon.eval("output = ''\n" +
                "for i in 1..10 {\n" +
                "    if &i % 2 == 0 {\n" +
                "        continue\n" +
                "    }\n" +
                "    output = &output + &i\n" +
                "}\n" +
                "&output"));
    }

    @Test
    public void testNestedContinue() {
        assertEquals("1,3,5,1,3,5,1,3,5,", Fluxon.eval("output = ''\n" +
                "for i in 1..3 {\n" +
                "    for j in 1..5 {\n" +
                "        if &j % 2 == 0 {\n" +
                "            continue\n" +
                "        }\n" +
                "        output = &output + &j + ','\n" +
                "    }\n" +
                "}\n" +
                "&output"));
    }

    @Test
    public void testBreakInWhileLoop() {
        assertEquals("0,1,2,3,4,", Fluxon.eval("output = ''\n" +
                "count = 0\n" +
                "while true {\n" +
                "    output = &output + &count + ','\n" +
                "    count = &count + 1\n" +
                "    if &count >= 5 {\n" +
                "        break\n" +
                "    }\n" +
                "}\n" +
                "&output"));
    }

    @Test
    public void testContinueInWhileLoop() {
        assertEquals("1,3,5,7,9,", Fluxon.eval("output = ''\n" +
                "count = 0\n" +
                "while &count < 10 {\n" +
                "    count = &count + 1\n" +
                "    if &count % 2 == 0 {\n" +
                "        continue\n" +
                "    }\n" +
                "    output = &output + &count + ','\n" +
                "}\n" +
                "&output"));
    }

    @Test
    public void testBreakWithCondition() {
        assertEquals("threshold-reached", Fluxon.eval("result = 'continue'\n" +
                "sum = 0\n" +
                "for i in 1..100 {\n" +
                "    sum = &sum + &i\n" +
                "    if &sum > 50 {\n" +
                "        result = 'threshold-reached'\n" +
                "        break\n" +
                "    }\n" +
                "}\n" +
                "&result"));
    }

    @Test
    public void testComplexContinueCase() {
        assertEquals("1:odd,2:skipped,3:odd,4:skipped,5:odd,", Fluxon.eval("output = ''\n" +
                "for i in 1..5 {\n" +
                "    if &i % 2 == 0 {\n" +
                "        output = &output + &i + ':skipped,'\n" +
                "        continue\n" +
                "    }\n" +
                "    output = &output + &i + ':odd,'\n" +
                "}\n" +
                "&output"));
    }
}
