package org.tabooproject.fluxon.web;

import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.interpreter.Interpreter;

import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Service layer that runs Fluxon scripts and streams output to the provided OutputStream.
 */
public class InterpreterService {

    private final InterpreterServiceConfig config;

    public InterpreterService() {
        this(InterpreterServiceConfig.fromSystem());
    }

    public InterpreterService(InterpreterServiceConfig config) {
        this.config = config;
    }

    public void execute(String source, OutputStream output) throws IOException {
        // Wrap caller output so print/error go straight to the HTTP response.
        PrintStream streaming = new PrintStream(output, true, StandardCharsets.UTF_8.name());
        try {
            Environment env = FluxonRuntime.getInstance().newEnvironment();
            env.setOut(streaming);
            env.setErr(streaming);
            Interpreter interpreter = new Interpreter(env);
            interpreter.setCostLimit(config.getCostLimit());
            interpreter.setCostPerStep(config.getCostPerStep());
            ParsedScript parsed = Fluxon.parse(source, env);
            Object result = parsed.eval(interpreter);
            streaming.println("RESULT: " + result);
        } catch (Exception e) {
            streaming.println("ERROR: " + e);
        } finally {
            streaming.flush();
        }
    }
}
