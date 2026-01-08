package org.tabooproject.fluxon.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.concurrent.TimeUnit;

/**
 * Environment 复用性能基准测试
 * 对比每次新建 Environment vs 复用 Environment 的性能差距
 * <p>
 * Benchmark                                          Mode  Cnt   Score    Error  Units
 * EnvironmentReuseBenchmark.baseline_NewEnvironment  avgt    3   8.847 ±  0.337  ns/op
 * EnvironmentReuseBenchmark.eval_NewEnv              avgt    3  37.130 ±  6.228  ns/op
 * EnvironmentReuseBenchmark.eval_ReuseEnv            avgt    3  25.895 ± 22.700  ns/op
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

    private ParsedScript parsed;
    private Environment reusableEnv;

    @Setup
    public void setup() {
        CompilationContext ctx = new CompilationContext(EXPR);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        parsed = Fluxon.parse(ctx, env);
        reusableEnv = parsed.newEnvironment();
    }

    /**
     * 测量 Environment 创建开销
     */
    @Benchmark
    public void baseline_NewEnvironment(Blackhole bh) {
        bh.consume(FluxonRuntime.getInstance().newEnvironment());
    }

    /**
     * 每次执行新建 Environment
     */
    @Benchmark
    public void eval_NewEnv(Blackhole bh) {
        bh.consume(parsed.eval());
    }

    /**
     * 复用同一 Environment
     */
    @Benchmark
    public void eval_ReuseEnv(Blackhole bh) {
        bh.consume(parsed.eval(reusableEnv));
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(EnvironmentReuseBenchmark.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}
