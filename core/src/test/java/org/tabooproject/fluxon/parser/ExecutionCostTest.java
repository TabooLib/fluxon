package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.error.ExecutionCostExceededError;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionCostTest {

    @Test
    void whileLoopStopsWhenCostExhausted() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(5);

        ParsedScript script = Fluxon.parse("while true {}", env);

        assertThrows(ExecutionCostExceededError.class, () -> script.eval(interpreter));
    }

    @Test
    void costDeductsPerStatement() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(5);

        ParsedScript script = Fluxon.parse("print('a')\nprint('b')", env);

        script.eval(interpreter);
        // 至少扣除了两步
        assertTrue(interpreter.getCostRemaining() <= 3);
    }

    @Test
    void whileWithExpressionBodyConsumesCost() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(2);

        ParsedScript script = Fluxon.parse("while true print('a')", env);

        assertThrows(ExecutionCostExceededError.class, () -> script.eval(interpreter));
    }

    @Test
    void forLoopWithExpressionBodyConsumesCost() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(2);

        ParsedScript script = Fluxon.parse("for x in [1,2,3] print(x)", env);

        assertThrows(ExecutionCostExceededError.class, () -> script.eval(interpreter));
    }

    @Test
    void whileUsedAsExpressionArgumentStillCharges() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(3);

        ParsedScript script = Fluxon.parse("print(while true 1)", env);

        assertThrows(ExecutionCostExceededError.class, () -> script.eval(interpreter));
    }

    @Test
    void forUsedAsExpressionArgumentStillCharges() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(2);

        ParsedScript script = Fluxon.parse("print(for x in [1,2,3] 1)", env);

        assertThrows(ExecutionCostExceededError.class, () -> script.eval(interpreter));
    }

    @Test
    void recursiveExpressionFunctionConsumesCost() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(5);

        ParsedScript script = Fluxon.parse("def loop() = loop()\nloop()", env);

        assertThrows(ExecutionCostExceededError.class, () -> script.eval(interpreter));
    }

    @Test
    void lambdaRecursionConsumesCost() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(5);

        ParsedScript script = Fluxon.parse(
                "def run(cb) = call(&cb)\n" +
                "f = null\n" +
                "f = || run(&f)\n" +
                "run(&f)", env);

        assertThrows(ExecutionCostExceededError.class, () -> script.eval(interpreter));
    }

    @Test
    void whileUsedInsideForCollectionStillCharges() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(3);

        ParsedScript script = Fluxon.parse("for x in (while true {}) {}", env);

        assertThrows(ExecutionCostExceededError.class, () -> script.eval(interpreter));
    }

    @Test
    void tryFinallyExpressionArgumentConsumesCost() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(3);

        ParsedScript script = Fluxon.parse("print(try 1 finally while true {})", env);

        assertThrows(ExecutionCostExceededError.class, () -> script.eval(interpreter));
    }

    @Test
    void tryCatchExpressionArgumentConsumesCost() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(3);

        ParsedScript script = Fluxon.parse("print(try throw('x') catch while true 1)", env);

        assertThrows(ExecutionCostExceededError.class, () -> script.eval(interpreter));
    }

    @Test
    void lambdaWithInfiniteLoopCalledThroughFunction() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(3);

        ParsedScript script = Fluxon.parse("def run(cb) = call(&cb)\nrun(|| while true 1)", env);

        assertThrows(ExecutionCostExceededError.class, () -> script.eval(interpreter));
    }
}
