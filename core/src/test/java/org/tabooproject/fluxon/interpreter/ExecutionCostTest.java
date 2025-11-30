package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.error.ExecutionCostExceededError;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionCostTest {

    @Test
    void whileLoopStopsWhenCostExhausted() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(5);

        List<ParseResult> results = Fluxon.parse("while true {}", env);

        assertThrows(ExecutionCostExceededError.class, () -> interpreter.execute(results));
    }

    @Test
    void costDeductsPerStatement() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(5);

        List<ParseResult> results = Fluxon.parse("print('a')\nprint('b')", env);

        interpreter.execute(results);
        // 至少扣除了两步
        assertTrue(interpreter.getCostRemaining() <= 3);
    }

    @Test
    void whileWithExpressionBodyConsumesCost() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(2);

        List<ParseResult> results = Fluxon.parse("while true print('a')", env);

        assertThrows(ExecutionCostExceededError.class, () -> interpreter.execute(results));
    }

    @Test
    void forLoopWithExpressionBodyConsumesCost() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(2);

        List<ParseResult> results = Fluxon.parse("for x in [1,2,3] print(x)", env);

        assertThrows(ExecutionCostExceededError.class, () -> interpreter.execute(results));
    }

    @Test
    void whileUsedAsExpressionArgumentStillCharges() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(3);

        List<ParseResult> results = Fluxon.parse("print(while true 1)", env);

        assertThrows(ExecutionCostExceededError.class, () -> interpreter.execute(results));
    }

    @Test
    void forUsedAsExpressionArgumentStillCharges() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(2);

        List<ParseResult> results = Fluxon.parse("print(for x in [1,2,3] 1)", env);

        assertThrows(ExecutionCostExceededError.class, () -> interpreter.execute(results));
    }

    @Test
    void recursiveExpressionFunctionConsumesCost() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(5);

        List<ParseResult> results = Fluxon.parse("def loop() = loop()\nloop()", env);

        assertThrows(ExecutionCostExceededError.class, () -> interpreter.execute(results));
    }

    @Test
    void lambdaRecursionConsumesCost() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(5);

        List<ParseResult> results = Fluxon.parse(
                "def run(cb) = call(&cb)\n" +
                "f = null\n" +
                "f = || run(&f)\n" +
                "run(&f)", env);

        assertThrows(ExecutionCostExceededError.class, () -> interpreter.execute(results));
    }

    @Test
    void whileUsedInsideForCollectionStillCharges() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(3);

        List<ParseResult> results = Fluxon.parse("for x in (while true {}) {}", env);

        assertThrows(ExecutionCostExceededError.class, () -> interpreter.execute(results));
    }

    @Test
    void tryFinallyExpressionArgumentConsumesCost() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(3);

        List<ParseResult> results = Fluxon.parse("print(try 1 finally while true {})", env);

        assertThrows(ExecutionCostExceededError.class, () -> interpreter.execute(results));
    }

    @Test
    void tryCatchExpressionArgumentConsumesCost() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(3);

        List<ParseResult> results = Fluxon.parse("print(try throw('x') catch while true 1)", env);

        assertThrows(ExecutionCostExceededError.class, () -> interpreter.execute(results));
    }

    @Test
    void lambdaWithInfiniteLoopCalledThroughFunction() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Interpreter interpreter = new Interpreter(env);
        interpreter.setCostLimit(3);

        List<ParseResult> results = Fluxon.parse("def run(cb) = call(&cb)\nrun(|| while true 1)", env);

        assertThrows(ExecutionCostExceededError.class, () -> interpreter.execute(results));
    }
}
