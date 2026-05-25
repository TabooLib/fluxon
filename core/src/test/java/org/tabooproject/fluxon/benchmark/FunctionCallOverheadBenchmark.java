package org.tabooproject.fluxon.benchmark;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 函数调用开销基准测试
 * 用等价循环拆开直接内联、直接调用和 FunctionContext 框架路径的成本。
 *
 * @author sky
 */
@SuppressWarnings("deprecation")
public class FunctionCallOverheadBenchmark {

    private static final AtomicInteger CLASS_COUNTER = new AtomicInteger(0);
    private static final int WARMUP = 8;
    private static final int ITERATIONS = 12;
    private static volatile Object sink;

    private static final String LOOP_BASELINE =
            "r = 0\n" +
            "for i in 1..100000 { r = &r + 1 }\n" +
            "&r";
    private static final String INLINE_CALL =
            "def inc(x: int) = &x + 1\n" +
            "r = 0\n" +
            "for i in 1..100000 { r = inc(&r) }\n" +
            "&r";
    private static final String DIRECT_CALL =
            "def inc(x: int) { _touch = 0; &x + 1 }\n" +
            "r = 0\n" +
            "for i in 1..100000 { r = inc(&r) }\n" +
            "&r";
    private static final String DIRECT_BASELINE =
            "r = 0\n" +
            "for i in 1..100000 { _touch = 0; r = &r + 1 }\n" +
            "&r";
    private static final String FRAMEWORK_CALL =
            "def inc(x: int) { return &x + 1 }\n" +
            "r = 0\n" +
            "for i in 1..100000 { r = inc(&r) }\n" +
            "&r";
    private static final String HIGH_ORDER_BASELINE =
            "sum = 0\n" +
            "for i in 1..100000 { sum += &i }\n" +
            "&sum";
    private static final String HIGH_ORDER_LAMBDA =
            "(1..100000)::sumOf(|| &it)";
    private static final String HIGH_ORDER_READ_CAPTURE =
            "base = 1\n" +
            "(1..100000)::sumOf(|| &it + &base)";
    private static final String HIGH_ORDER_WRITE_CAPTURE =
            "sum = 0\n" +
            "(1..100000)::each(|| sum += &it)\n" +
            "&sum";
    private static final String DYNAMIC_LAMBDA_CALL =
            "f = |x| &x + 1\n" +
            "r = 0\n" +
            "for i in 1..100000 { r = call(&f, [&r]) }\n" +
            "&r";
    private static final String INTERPRET_ROOT_LOOP =
            "r = 0\n" +
            "for i in 1..1000 { r = &r + 1 }\n" +
            "&r";
    private static final String INTERPRET_LOCAL_LOOP =
            "_r = 0\n" +
            "for i in 1..1000 { _r = &_r + 1 }\n" +
            "&_r";
    private static final String INTERPRET_FUNCTION_LOOP =
            "def run() {\n" +
            "  r = 0\n" +
            "  for i in 1..1000 { r = &r + 1 }\n" +
            "  &r\n" +
            "}\n" +
            "run()";
    private static final String INTERPRET_FUNCTION_CALL_LOOP =
            "def inc(x) = &x + 1\n" +
            "r = 0\n" +
            "for i in 1..1000 { r = inc(&r) }\n" +
            "&r";
    private static final String INTERPRET_ENV_FUNCTION_CALL_LOOP =
            "def inc(x) { _c = || &x; &x + 1 }\n" +
            "r = 0\n" +
            "for i in 1..1000 { r = inc(&r) }\n" +
            "&r";
    private static final String INTERPRET_DYNAMIC_LAMBDA_CALL =
            "f = |x| &x + 1\n" +
            "r = 0\n" +
            "for i in 1..1000 { r = call(&f, [&r]) }\n" +
            "&r";

    @Test
    public void functionCallOverhead() throws Exception {
        RuntimeScriptBase baseline = compile(LOOP_BASELINE);
        RuntimeScriptBase inlineCall = compile(INLINE_CALL);
        RuntimeScriptBase directCall = compile(DIRECT_CALL);
        RuntimeScriptBase directBaseline = compile(DIRECT_BASELINE);
        RuntimeScriptBase frameworkCall = compile(FRAMEWORK_CALL);

        System.out.println("=== Function Call Overhead Benchmark: 100000 loop iterations ===");
        BenchResult baselineResult = bench("Loop baseline       ", baseline);
        BenchResult inlineResult = bench("Inline expression   ", inlineCall);
        BenchResult directBaselineResult = bench("Direct baseline     ", directBaseline);
        BenchResult directResult = bench("callDirect method   ", directCall);
        BenchResult frameworkResult = bench("FunctionContext call", frameworkCall);

        printDelta("Inline vs loop       ", inlineResult, baselineResult);
        printDelta("callDirect overhead  ", directResult, directBaselineResult);
        printDelta("Framework overhead   ", frameworkResult, baselineResult);
    }

    @Test
    public void lambdaCallOverhead() throws Exception {
        RuntimeScriptBase baseline = compile(HIGH_ORDER_BASELINE);
        RuntimeScriptBase highOrderLambda = compile(HIGH_ORDER_LAMBDA);
        RuntimeScriptBase readCapture = compile(HIGH_ORDER_READ_CAPTURE);
        RuntimeScriptBase writeCapture = compile(HIGH_ORDER_WRITE_CAPTURE);
        RuntimeScriptBase dynamicCall = compile(DYNAMIC_LAMBDA_CALL);

        System.out.println("=== Lambda Call Overhead Benchmark: 100000 callback invocations ===");
        BenchResult baselineResult = bench("Loop baseline       ", baseline);
        BenchResult highOrderResult = bench("sumOf lambda        ", highOrderLambda);
        BenchResult readCaptureResult = bench("sumOf read capture  ", readCapture);
        BenchResult writeCaptureResult = bench("each write capture  ", writeCapture);
        BenchResult dynamicResult = bench("call(lambda, list)  ", dynamicCall);

        printDelta("sumOf lambda overhead", highOrderResult, baselineResult);
        printDelta("Read capture extra   ", readCaptureResult, highOrderResult);
        printDelta("Write capture extra  ", writeCaptureResult, baselineResult);
        printDelta("Dynamic call overhead", dynamicResult, baselineResult);
    }

    @Test
    public void interpretHotPathBreakdown() {
        ParsedScript rootLoop = parse(INTERPRET_ROOT_LOOP);
        ParsedScript localLoop = parse(INTERPRET_LOCAL_LOOP);
        ParsedScript functionLoop = parse(INTERPRET_FUNCTION_LOOP);
        ParsedScript functionCallLoop = parse(INTERPRET_FUNCTION_CALL_LOOP);
        ParsedScript envFunctionCallLoop = parse(INTERPRET_ENV_FUNCTION_CALL_LOOP);
        ParsedScript dynamicLambdaCall = parse(INTERPRET_DYNAMIC_LAMBDA_CALL);

        System.out.println("=== Interpret Hot Path Breakdown: 1000 loop iterations ===");
        BenchResult rootLoopResult = bench("Root loop           ", rootLoop);
        BenchResult localLoopResult = bench("Local loop          ", localLoop);
        BenchResult functionLoopResult = bench("Function loop       ", functionLoop);
        BenchResult functionCallResult = bench("Function call loop  ", functionCallLoop);
        BenchResult envFunctionCallResult = bench("Env function call   ", envFunctionCallLoop);
        BenchResult dynamicLambdaResult = bench("call(lambda, list)  ", dynamicLambdaCall);

        printDelta("Root map extra      ", rootLoopResult, localLoopResult, 1000);
        printDelta("Function frame extra", functionLoopResult, localLoopResult, 1000);
        printDelta("User call extra     ", functionCallResult, functionLoopResult, 1000);
        printDelta("Env call extra      ", envFunctionCallResult, functionCallResult, 1000);
        printDelta("Lambda call extra   ", dynamicLambdaResult, localLoopResult, 1000);
    }

    private RuntimeScriptBase compile(String source) throws Exception {
        String className = "FunctionCallOverhead_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        CompileResult result = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = result.defineClass(new FluxonClassLoader());
        return (RuntimeScriptBase) scriptClass.newInstance();
    }

    private ParsedScript parse(String source) {
        CompilationContext ctx = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        return Fluxon.parse(ctx, env);
    }

    private BenchResult bench(String label, RuntimeScriptBase script) {
        for (int i = 0; i < WARMUP; i++) {
            sink = script.eval(FluxonRuntime.getInstance().newEnvironment());
        }
        long total = 0;
        long min = Long.MAX_VALUE;
        long max = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            sink = script.eval(FluxonRuntime.getInstance().newEnvironment());
            long elapsed = System.nanoTime() - start;
            total += elapsed;
            min = Math.min(min, elapsed);
            max = Math.max(max, elapsed);
        }
        BenchResult result = new BenchResult(total / (double) ITERATIONS, min, max);
        System.out.printf(
                "  %s avg=%.3f ms  min=%.3f ms  max=%.3f ms%n",
                label,
                result.avgNs / 1_000_000.0,
                result.minNs / 1_000_000.0,
                result.maxNs / 1_000_000.0
        );
        return result;
    }

    private BenchResult bench(String label, ParsedScript script) {
        for (int i = 0; i < WARMUP; i++) {
            sink = script.eval();
        }
        long total = 0;
        long min = Long.MAX_VALUE;
        long max = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            sink = script.eval();
            long elapsed = System.nanoTime() - start;
            total += elapsed;
            min = Math.min(min, elapsed);
            max = Math.max(max, elapsed);
        }
        BenchResult result = new BenchResult(total / (double) ITERATIONS, min, max);
        System.out.printf(
                "  %s avg=%.3f ms  min=%.3f ms  max=%.3f ms%n",
                label,
                result.avgNs / 1_000_000.0,
                result.minNs / 1_000_000.0,
                result.maxNs / 1_000_000.0
        );
        return result;
    }

    private void printDelta(String label, BenchResult current, BenchResult baseline) {
        double deltaNs = (current.avgNs - baseline.avgNs) / 100000.0;
        System.out.printf("  %s %.2f ns/call%n", label, deltaNs);
    }

    private void printDelta(String label, BenchResult current, BenchResult baseline, int operations) {
        double deltaNs = (current.avgNs - baseline.avgNs) / operations;
        System.out.printf("  %s %.2f ns/op%n", label, deltaNs);
    }

    private static final class BenchResult {

        private final double avgNs;
        private final long minNs;
        private final long maxNs;

        private BenchResult(double avgNs, long minNs, long maxNs) {
            this.avgNs = avgNs;
            this.minNs = minNs;
            this.maxNs = maxNs;
        }
    }
}
