package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.parser.SourceTrace;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeErrorDiagnosticsTest {

    @Test
    void indexErrorShowsSourceExcerpt1() {
        FluxonRuntimeError error = assertThrows(FluxonRuntimeError.class, () -> FluxonTestUtil.interpret("[1][1]"));
        String message = error.getMessage();
        error.printStackTrace();
        assertTrue(message.contains("Index out of bounds"), "should keep original error message");
        assertTrue(message.contains("main:1"), "should include filename and position");
        assertTrue(message.contains("^"), "should render caret marker");
        assertTrue(message.contains("[1][1]"), "should render offending line");
    }

    @Test
    void indexErrorShowsSourceExcerpt2() {
        FluxonRuntimeError error = assertThrows(FluxonRuntimeError.class, () -> FluxonTestUtil.compile("[1][1]", "TestScript"));
        String message = error.getMessage();
        error.printStackTrace();
        assertTrue(message.contains("Index out of bounds"), "should keep original error message");
        assertTrue(message.contains("main:1"), "should include filename and position");
        assertTrue(message.contains("^"), "should render caret marker");
        assertTrue(message.contains("[1][1]"), "should render offending line");
    }

    @Test
    void lambdaErrorShowsExcerptInterpret() {
        FluxonRuntimeError error = assertThrows(FluxonRuntimeError.class, () -> FluxonTestUtil.interpret("[1] :: map(|x| [1][1])"));
        String message = error.getMessage();
        error.printStackTrace();
        assertTrue(message.contains("Index out of bounds"), "should keep original error message");
        assertTrue(message.contains("main:1"), "should include filename and position");
        assertTrue(message.contains("^"), "should render caret marker");
        assertTrue(message.contains("map(|x| [1][1])"), "should render offending line");
    }

    @Test
    void lambdaErrorShowsExcerptCompile() {
        FluxonRuntimeError error = assertThrows(FluxonRuntimeError.class, () -> FluxonTestUtil.compile("[1] :: map(|x| [1][1])", "TestScript"));
        String message = error.getMessage();
        error.printStackTrace();
        assertTrue(message.contains("Index out of bounds"), "should keep original error message");
        assertTrue(message.contains("main:1"), "should include filename and position");
        assertTrue(message.contains("^"), "should render caret marker");
        assertTrue(message.contains("map(|x| [1][1])"), "should render offending line");
    }
}
