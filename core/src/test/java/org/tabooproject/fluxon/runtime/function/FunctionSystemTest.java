package org.tabooproject.fluxon.runtime.function;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FunctionSystemTest {

    @Test
    void printAndErrorCanTargetCustomStreams() {
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream customOut = new PrintStream(outBuffer, true);
        PrintStream customErr = new PrintStream(errBuffer, true);

        try {
            Environment env = FluxonRuntime.getInstance().newEnvironment();
            env.setOut(customOut);
            env.setErr(customErr);

            Fluxon.eval("print(\"hello\")", env);
            Fluxon.eval("error(\"oops\")", env);

            assertEquals("hello" + System.lineSeparator(), new String(outBuffer.toByteArray(), StandardCharsets.UTF_8));
            assertEquals("oops" + System.lineSeparator(), new String(errBuffer.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            customOut.close();
            customErr.close();
        }
    }

    @Test
    void printWithNoArgsOutputsNewline() {
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        PrintStream customOut = new PrintStream(outBuffer, true);

        try {
            Environment env = FluxonRuntime.getInstance().newEnvironment();
            env.setOut(customOut);

            Fluxon.eval("print()", env);

            assertEquals(System.lineSeparator(), new String(outBuffer.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            customOut.close();
        }
    }

    @Test
    void errorWithNoArgsOutputsNewline() {
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream customErr = new PrintStream(errBuffer, true);

        try {
            Environment env = FluxonRuntime.getInstance().newEnvironment();
            env.setErr(customErr);

            Fluxon.eval("error()", env);

            assertEquals(System.lineSeparator(), new String(errBuffer.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            customErr.close();
        }
    }

    @Test
    void sleepPausesExecution() {
        long start = System.currentTimeMillis();
        Fluxon.eval("sleep(50)");
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 40, "Expected at least 40ms elapsed, got " + elapsed);
    }

    @Test
    void forNameReturnsClass() {
        Object result = Fluxon.eval("forName(\"java.lang.String\")");
        assertEquals(String.class, result);
    }

    @Test
    void forNameThrowsOnUnknownClass() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            Fluxon.eval("forName(\"com.nonexistent.FakeClass\")")
        );
        assertTrue(ex.getMessage().contains("Class not found"));
    }

    @Test
    void callInvokesNamedFunction() {
        // call() with function name as string and args list
        Object result = Fluxon.eval("call(\"forName\", [\"java.util.ArrayList\"])");
        assertEquals(java.util.ArrayList.class, result);
    }

    @Test
    void callWithNoArgsInvokesNamedFunction() {
        // call() with function name as string, no args
        Object result = Fluxon.eval("call(\"this\")");
        assertNull(result);
    }

    @Test
    void thisReturnsNullWhenNoTarget() {
        Object result = Fluxon.eval("this()");
        assertNull(result);
    }

    @Test
    void throwWithStringThrowsRuntimeException() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            Fluxon.eval("throw(\"something went wrong\")")
        );
        assertEquals("something went wrong", ex.getMessage());
    }
}
