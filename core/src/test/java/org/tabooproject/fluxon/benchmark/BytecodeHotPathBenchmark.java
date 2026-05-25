package org.tabooproject.fluxon.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 字节码热路径基准测试
 * 覆盖编译器优化时最容易回归的循环、字符串拼接和函数直连路径。
 *
 * @author sky
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(org.openjdk.jmh.annotations.Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@SuppressWarnings("deprecation")
public class BytecodeHotPathBenchmark {

    private static final AtomicInteger CLASS_COUNTER = new AtomicInteger(0);
    private static final PrintStream NULL_OUT = new PrintStream(new OutputStream() {
        @Override
        public void write(int b) {
        }
    });

    private RuntimeScriptBase rootRangeAssignLoop;
    private RuntimeScriptBase rootRangeCompoundLoop;
    private RuntimeScriptBase localRangeCompoundLoop;
    private RuntimeScriptBase stringConcatPrimitive;
    private RuntimeScriptBase whenConstantRange;
    private RuntimeScriptBase printPrimitive;

    @Setup
    public void setup() throws Exception {
        rootRangeAssignLoop = compile("sum = 0; for i in 1..10 { sum = &sum + &i }; &sum");
        rootRangeCompoundLoop = compile("sum = 0; for i in 1..10 { sum += &i }; &sum");
        localRangeCompoundLoop = compile("_sum = 0; for i in 1..10 { _sum += &i }; &_sum");
        stringConcatPrimitive = compile("sum = 55; \"Sum: \" + &sum");
        whenConstantRange = compile("LIMIT = 10; sum = 5; when &sum { in 0..&LIMIT -> 'hit' else -> 'miss' }");
        printPrimitive = compile("print(1)");
    }

    @Benchmark
    public void rootRangeAssignLoop(Blackhole bh) {
        bh.consume(rootRangeAssignLoop.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    @Benchmark
    public void rootRangeCompoundLoop(Blackhole bh) {
        bh.consume(rootRangeCompoundLoop.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    @Benchmark
    public void localRangeCompoundLoop(Blackhole bh) {
        bh.consume(localRangeCompoundLoop.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    @Benchmark
    public void stringConcatPrimitive(Blackhole bh) {
        bh.consume(stringConcatPrimitive.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    @Benchmark
    public void whenConstantRange(Blackhole bh) {
        bh.consume(whenConstantRange.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    @Benchmark
    public void printPrimitive(Blackhole bh) {
        Environment environment = FluxonRuntime.getInstance().newEnvironment();
        environment.setOut(NULL_OUT);
        bh.consume(printPrimitive.eval(environment));
    }

    private RuntimeScriptBase compile(String source) throws Exception {
        String className = "HotPathBenchmark_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        CompileResult result = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = result.defineClass(new FluxonClassLoader());
        return (RuntimeScriptBase) scriptClass.newInstance();
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(BytecodeHotPathBenchmark.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}
