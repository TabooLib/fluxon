package org.tabooproject.fluxon.runtime.function;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
