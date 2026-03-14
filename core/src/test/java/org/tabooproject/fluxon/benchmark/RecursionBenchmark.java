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
 * 递归函数性能基准测试
 * 测量 env-free 优化对高频函数调用场景的影响
 * <p>
 * fib(25) 产生约 242,785 次函数调用，充分放大 per-call 开销差异
 *
 * @author sky
 */
@SuppressWarnings("deprecation")
public class RecursionBenchmark {

    private static final AtomicInteger CLASS_COUNTER = new AtomicInteger(0);
    private static final int WARMUP = 5;
    private static final int ITERATIONS = 10;

    // 带类型注解的递归 fib（触发 env-free 优化 + 原始类型优化）
    private static final String FIB_TYPED = "def fib(n: int) = if &n <= 1 then &n else fib(&n - 1) + fib(&n - 2)\nfib(25)";
    // 无类型注解的递归 fib（触发 env-free 优化，但参数为 Object 类型）
    private static final String FIB_UNTYPED = "def fib(n) = if &n <= 1 then &n else fib(&n - 1) + fib(&n - 2)\nfib(25)";
    // 带 Lambda 捕获的递归（禁用 env-free 优化，回退到 Environment 路径）
    private static final String FIB_WITH_LAMBDA = "def fib(n) { _captured = || &n; if &n <= 1 then &n else fib(&n - 1) + fib(&n - 2) }\nfib(25)";

    private ParsedScript parseExpression(String source) {
        CompilationContext ctx = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        return Fluxon.parse(ctx, env);
    }

    private RuntimeScriptBase compileExpression(String source) throws Exception {
        String className = "RecBench_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        CompileResult result = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = result.defineClass(new FluxonClassLoader());
        return (RuntimeScriptBase) scriptClass.newInstance();
    }

    // 普通函数调用（1 次调用，无递归）
    private static final String SIMPLE_CALL = "def add(a, b) = &a + &b\nadd(3, 4)";
    // 循环内函数调用（1000 次调用）
    private static final String LOOP_CALL = "def inc(x) = &x + 1\nresult = 0\nfor i in 1..1000 { result = inc(&result) }\n&result";
    // 多参数函数（4 个参数）
    private static final String MULTI_PARAM = "def calc(a, b, c, d) = &a * &b + &c - &d\ncalc(10, 20, 30, 40)";
    // 带 Lambda 捕获版本（强制走传统 Environment 路径）
    private static final String SIMPLE_CALL_ENV = "def add(a, b) { _c = || &a; &a + &b }\nadd(3, 4)";
    private static final String LOOP_CALL_ENV = "def inc(x) { _c = || &x; &x + 1 }\nresult = 0\nfor i in 1..1000 { result = inc(&result) }\n&result";
    private static final String MULTI_PARAM_ENV = "def calc(a, b, c, d) { _c = || &a; &a * &b + &c - &d }\ncalc(10, 20, 30, 40)";

    @Test
    public void recursionBenchmark() throws Exception {
        ParsedScript parsedTyped = parseExpression(FIB_TYPED);
        ParsedScript parsedUntyped = parseExpression(FIB_UNTYPED);
        ParsedScript parsedWithLambda = parseExpression(FIB_WITH_LAMBDA);
        RuntimeScriptBase compiledTyped = compileExpression(FIB_TYPED);
        RuntimeScriptBase compiledUntyped = compileExpression(FIB_UNTYPED);
        RuntimeScriptBase compiledWithLambda = compileExpression(FIB_WITH_LAMBDA);

        System.out.println("=== Recursion Benchmark: fib(25) ~ 242,785 calls ===");
        System.out.println();

        // 解释模式
        System.out.println("--- Interpret Mode ---");
        bench("Typed   (env-free) ", () -> parsedTyped.eval());
        bench("Untyped (env-free) ", () -> parsedUntyped.eval());
        bench("WithLambda (env)   ", () -> parsedWithLambda.eval());
        System.out.println();

        // 编译模式
        System.out.println("--- Compile Mode ---");
        bench("Typed   (env-free) ", () -> compiledTyped.eval(FluxonRuntime.getInstance().newEnvironment()));
        bench("Untyped (env-free) ", () -> compiledUntyped.eval(FluxonRuntime.getInstance().newEnvironment()));
        bench("WithLambda (env)   ", () -> compiledWithLambda.eval(FluxonRuntime.getInstance().newEnvironment()));

        // 普通场景
        System.out.println();
        System.out.println("=== Normal Call Scenarios (non-recursive) ===");
        System.out.println();

        ParsedScript parsedSimple = parseExpression(SIMPLE_CALL);
        ParsedScript parsedLoop = parseExpression(LOOP_CALL);
        ParsedScript parsedMulti = parseExpression(MULTI_PARAM);
        ParsedScript parsedSimpleEnv = parseExpression(SIMPLE_CALL_ENV);
        ParsedScript parsedLoopEnv = parseExpression(LOOP_CALL_ENV);
        ParsedScript parsedMultiEnv = parseExpression(MULTI_PARAM_ENV);
        RuntimeScriptBase compiledSimple = compileExpression(SIMPLE_CALL);
        RuntimeScriptBase compiledLoop = compileExpression(LOOP_CALL);
        RuntimeScriptBase compiledMulti = compileExpression(MULTI_PARAM);
        RuntimeScriptBase compiledSimpleEnv = compileExpression(SIMPLE_CALL_ENV);
        RuntimeScriptBase compiledLoopEnv = compileExpression(LOOP_CALL_ENV);
        RuntimeScriptBase compiledMultiEnv = compileExpression(MULTI_PARAM_ENV);

        System.out.println("--- Interpret Mode ---");
        bench("Simple (env-free)  ", () -> parsedSimple.eval());
        bench("Simple (env)       ", () -> parsedSimpleEnv.eval());
        bench("Loop 1000 (env-free)", () -> parsedLoop.eval());
        bench("Loop 1000 (env)    ", () -> parsedLoopEnv.eval());
        bench("Multi4 (env-free)  ", () -> parsedMulti.eval());
        bench("Multi4 (env)       ", () -> parsedMultiEnv.eval());
        System.out.println();
        System.out.println("--- Compile Mode ---");
        bench("Simple (env-free)  ", () -> compiledSimple.eval(FluxonRuntime.getInstance().newEnvironment()));
        bench("Simple (env)       ", () -> compiledSimpleEnv.eval(FluxonRuntime.getInstance().newEnvironment()));
        bench("Loop 1000 (env-free)", () -> compiledLoop.eval(FluxonRuntime.getInstance().newEnvironment()));
        bench("Loop 1000 (env)    ", () -> compiledLoopEnv.eval(FluxonRuntime.getInstance().newEnvironment()));
        bench("Multi4 (env-free)  ", () -> compiledMulti.eval(FluxonRuntime.getInstance().newEnvironment()));
        bench("Multi4 (env)       ", () -> compiledMultiEnv.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    private void bench(String label, Runnable task) {
        // warmup
        for (int i = 0; i < WARMUP; i++) {
            task.run();
        }
        // measure
        long total = 0;
        long min = Long.MAX_VALUE;
        long max = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            task.run();
            long elapsed = System.nanoTime() - start;
            total += elapsed;
            min = Math.min(min, elapsed);
            max = Math.max(max, elapsed);
        }
        double avgMs = (total / (double) ITERATIONS) / 1_000_000.0;
        double minMs = min / 1_000_000.0;
        double maxMs = max / 1_000_000.0;
        System.out.printf("  %s  avg=%.3f ms  min=%.3f ms  max=%.3f ms%n", label, avgMs, minMs, maxMs);
    }
}
