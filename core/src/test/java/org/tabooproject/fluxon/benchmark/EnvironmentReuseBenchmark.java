package org.tabooproject.fluxon.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;

import java.util.concurrent.TimeUnit;

/**
 * Environment 复用性能基准测试
 * 对比每次新建 Environment vs 复用 Environment 的性能差距
 *
 * @author sky
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
public class EnvironmentReuseBenchmark {

    private static final String EXPR = "1 + 2";
    // 包含用户函数的脚本（触发 defineRootFunction）
    private static final String EXPR_WITH_FUNC = "def add(a, b) = a + b\nadd(1, 2)";

    private ParsedScript parsed;
    private Environment reusableEnv;
    private RuntimeScriptBase compiledScript;
    private RuntimeScriptBase compiledFuncScript;

    @Setup
    public void setup() throws Exception {
        CompilationContext ctx = new CompilationContext(EXPR);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        parsed = Fluxon.parse(ctx, env);
        reusableEnv = parsed.newEnvironment();
        // 编译简单表达式
        CompileResult result = Fluxon.compile(EXPR, "BenchSimple");
        compiledScript = (RuntimeScriptBase) result.defineClass(new FluxonClassLoader()).getDeclaredConstructor().newInstance();
        // 编译带用户函数的表达式
        CompileResult funcResult = Fluxon.compile(EXPR_WITH_FUNC, "BenchFunc");
        compiledFuncScript = (RuntimeScriptBase) funcResult.defineClass(new FluxonClassLoader()).getDeclaredConstructor().newInstance();
    }

    /**
     * 测量 Environment 创建开销
     */
    @Benchmark
    public void baseline_NewEnvironment(Blackhole bh) {
        bh.consume(FluxonRuntime.getInstance().newEnvironment());
    }

    /**
     * 每次执行新建 Environment（解释模式，无用户函数）
     */
    @Benchmark
    public void eval_NewEnv(Blackhole bh) {
        bh.consume(parsed.eval());
    }

    /**
     * 复用同一 Environment（解释模式，无用户函数）
     */
    @Benchmark
    public void eval_ReuseEnv(Blackhole bh) {
        bh.consume(parsed.eval(reusableEnv));
    }

    /**
     * 编译模式：简单表达式，每次新建 Environment
     */
    @Benchmark
    public void compiled_Simple_NewEnv(Blackhole bh) {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        bh.consume(compiledScript.eval(env));
    }

    /**
     * 编译模式：带用户函数，每次新建 Environment
     * 测量 defineRootFunction 对 CopyOnWriteMap 的影响
     */
    @Benchmark
    public void compiled_WithFunc_NewEnv(Blackhole bh) {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        bh.consume(compiledFuncScript.eval(env));
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(EnvironmentReuseBenchmark.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}
