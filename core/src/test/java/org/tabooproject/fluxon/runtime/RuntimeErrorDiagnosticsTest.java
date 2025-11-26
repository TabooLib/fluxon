package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeErrorDiagnosticsTest {

    @Test
    void indexErrorShowsSourceExcerpt1() {
        FluxonRuntimeError error = assertThrows(FluxonRuntimeError.class, () -> FluxonTestUtil.interpret("[1][1]"));
        String message = error.getMessage();
        // error.printStackTrace();
        assertTrue(message.contains("Index out of bounds"), "should keep original error message");
        assertTrue(message.contains("main:1"), "should include filename and position");
        assertTrue(message.contains("^"), "should render caret marker");
        assertTrue(message.contains("[1][1]"), "should render offending line");
    }

    @Test
    void indexErrorShowsSourceExcerpt2() {
        FluxonRuntimeError error = assertThrows(FluxonRuntimeError.class, () -> FluxonTestUtil.compile("[1][1]", "TestScript"));
        String message = error.getMessage();
        // error.printStackTrace();
        assertTrue(message.contains("Index out of bounds"), "should keep original error message");
        assertTrue(message.contains("main:1"), "should include filename and position");
        assertTrue(message.contains("^"), "should render caret marker");
        assertTrue(message.contains("[1][1]"), "should render offending line");
    }

    @Test
    void lambdaErrorShowsExcerptInterpret() {
        FluxonRuntimeError error = assertThrows(FluxonRuntimeError.class, () -> FluxonTestUtil.interpret("[1] :: map(|x| [1][1])"));
        String message = error.getMessage();
        // error.printStackTrace();
        assertTrue(message.contains("Index out of bounds"), "should keep original error message");
        assertTrue(message.contains("main:1"), "should include filename and position");
        assertTrue(message.contains("^"), "should render caret marker");
        assertTrue(message.contains("map(|x| [1][1])"), "should render offending line");
    }

    @Test
    void lambdaErrorShowsExcerptCompile() {
        FluxonRuntimeError error = assertThrows(FluxonRuntimeError.class, () -> FluxonTestUtil.compile("[1] :: map(|x| [1][1])", "TestScript"));
        String message = error.getMessage();
        // error.printStackTrace();
        assertTrue(message.contains("Index out of bounds"), "should keep original error message");
        assertTrue(message.contains("main:1"), "should include filename and position");
        assertTrue(message.contains("^"), "should render caret marker");
        assertTrue(message.contains("map(|x| [1][1])"), "should render offending line");
    }

    @Test
    void testLambdaEachCountInsideFunction() {
        String script = ""
                + "def countAll(list) = {\n"
                + "  counter = 0\n"
                + "  &list::each(|item| { [1][1] })\n"
                + "  &counter\n"
                + "}\n"
                + "numbers = [1,2,3,4]!; countAll(&numbers)";
        FluxonRuntimeError error1 = assertThrows(FluxonRuntimeError.class, () -> FluxonTestUtil.interpret(script));
        FluxonRuntimeError error2 = assertThrows(FluxonRuntimeError.class, () -> FluxonTestUtil.compile(script, "TestScript"));
        String msg1 = error1.getMessage();
        String msg2 = error2.getMessage();
        // error1.printStackTrace();
        // error2.printStackTrace();
        assertTrue(msg1.contains("main:3"), "interpretation should point to lambda body line");
        assertTrue(msg1.contains("[1][1]"), "interpretation should render offending code");
        assertTrue(msg2.contains("main:3"), "compiled should point to lambda body line");
        assertTrue(msg2.contains("[1][1]"), "compiled should render offending code");
    }
}
