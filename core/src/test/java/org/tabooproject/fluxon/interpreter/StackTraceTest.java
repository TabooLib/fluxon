package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.FluxonTestUtil;

public class StackTraceTest {

    public static void main(String[] args) {
        try {
            FluxonTestUtil.runSilent("result = 'fail'\n" +
                    "for i in 1..10 {\n" +
                    "    if &i == 5 {\n" +
                    "        result = 'ok'\n" +
                    "        break()\n" +
                    "    }\n" +
                    "    print &i\n" +
                    "}\n" +
                    "&result");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            FluxonTestUtil.runSilent("result = 'fail'\n" +
                    "for i in 1..10 {\n" +
                    "    if &i == 10 {\n" +
                    "        result = 'ok'\n" +
                    "        &result::clear()\n" +
                    "        break\n" +
                    "    }\n" +
                    "    print &i\n" +
                    "}\n" +
                    "&result");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
