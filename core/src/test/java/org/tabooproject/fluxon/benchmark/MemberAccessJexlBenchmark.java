package org.tabooproject.fluxon.benchmark;

import org.apache.commons.jexl3.*;
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
import org.tabooproject.fluxon.type.TestObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Member Access 性能基准测试 - Fluxon vs JEXL 对比
 * <p>
 * 测试场景：
 * 1. 字段访问
 * 2. 无参方法调用
 * 3. 有参方法调用
 * 4. 链式调用
 * 5. 复杂链式调用
 * <p>
 * 对比对象：
 * - Fluxon 解释模式
 * - Fluxon 编译模式
 * - JEXL 解释模式
 * - JEXL 预编译脚本
 * - Java 直接调用（基准线）
 * <p>
 * Benchmark                                                 Mode  Cnt    Score     Error  Units
 * MemberAccessJexlBenchmark.chainedAccess_FluxonCompile     avgt    5   11.653 ±   5.917  ns/op
 * MemberAccessJexlBenchmark.chainedAccess_FluxonInterpret   avgt    5  208.860 ± 107.463  ns/op
 * MemberAccessJexlBenchmark.chainedAccess_JavaDirect        avgt    5    2.204 ±   1.095  ns/op
 * MemberAccessJexlBenchmark.chainedAccess_JexlScript        avgt    5  710.923 ± 321.795  ns/op
 * MemberAccessJexlBenchmark.complexChain_FluxonCompile      avgt    5    9.424 ±   4.378  ns/op
 * MemberAccessJexlBenchmark.complexChain_FluxonInterpret    avgt    5  114.439 ±  46.333  ns/op
 * MemberAccessJexlBenchmark.complexChain_JavaDirect         avgt    5    2.392 ±   0.771  ns/op
 * MemberAccessJexlBenchmark.fieldAccess_FluxonCompile       avgt    5   10.205 ±   7.226  ns/op
 * MemberAccessJexlBenchmark.fieldAccess_FluxonInterpret     avgt    5   81.125 ±  55.204  ns/op
 * MemberAccessJexlBenchmark.fieldAccess_JavaDirect          avgt    5    2.076 ±   1.126  ns/op
 * MemberAccessJexlBenchmark.methodNoArgs_FluxonCompile      avgt    5    6.898 ±   0.202  ns/op
 * MemberAccessJexlBenchmark.methodNoArgs_FluxonInterpret    avgt    5   84.726 ±  11.156  ns/op
 * MemberAccessJexlBenchmark.methodNoArgs_JavaDirect         avgt    5    2.399 ±   0.694  ns/op
 * MemberAccessJexlBenchmark.methodNoArgs_JexlScript         avgt    5  624.520 ±  38.788  ns/op
 * MemberAccessJexlBenchmark.methodWithArgs_FluxonCompile    avgt    5    7.549 ±   1.025  ns/op
 * MemberAccessJexlBenchmark.methodWithArgs_FluxonInterpret  avgt    5  100.175 ±   3.638  ns/op
 * MemberAccessJexlBenchmark.methodWithArgs_JavaDirect       avgt    5    1.497 ±   0.097  ns/op
 * MemberAccessJexlBenchmark.methodWithArgs_JexlScript       avgt    5  785.310 ±  84.309  ns/op
 *
 * @author sky
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class MemberAccessJexlBenchmark {

    private static final AtomicInteger CLASS_COUNTER = new AtomicInteger(0);

    // ========== 测试对象 ==========
    private TestObject testObject;

    // ========== JEXL ==========
    private JexlEngine jexlEngine;
    private JexlContext jexlContext;

    // JEXL 预编译脚本
    private JexlScript jexlFieldAccess;
    private JexlScript jexlMethodNoArgs;
    private JexlScript jexlMethodWithArgs;
    private JexlScript jexlChainedAccess;
    private JexlScript jexlComplexChain;

    // ========== Fluxon 解释模式 ==========
    private ParsedScript fluxonParsedFieldAccess;
    private ParsedScript fluxonParsedMethodNoArgs;
    private ParsedScript fluxonParsedMethodWithArgs;
    private ParsedScript fluxonParsedChainedAccess;
    private ParsedScript fluxonParsedComplexChain;
    private Environment fluxonInterpretEnv;

    // ========== Fluxon 编译模式 ==========
    private RuntimeScriptBase fluxonCompiledFieldAccess;
    private RuntimeScriptBase fluxonCompiledMethodNoArgs;
    private RuntimeScriptBase fluxonCompiledMethodWithArgs;
    private RuntimeScriptBase fluxonCompiledChainedAccess;
    private RuntimeScriptBase fluxonCompiledComplexChain;
    private Environment fluxonCompileEnv;

    // ========== 表达式 ==========
    // Fluxon 语法
    private static final String FLUXON_FIELD_ACCESS = "&obj.publicField";
    private static final String FLUXON_METHOD_NO_ARGS = "&obj.getName()";
    private static final String FLUXON_METHOD_WITH_ARGS = "&obj.add(10, 20)";
    private static final String FLUXON_CHAINED_ACCESS = "&obj.getSelf().getSelf().getName()";
    private static final String FLUXON_COMPLEX_CHAIN = "&obj.nested.nested.publicField";

    // JEXL 语法
    private static final String JEXL_FIELD_ACCESS = "obj.publicField";
    private static final String JEXL_METHOD_NO_ARGS = "obj.getName()";
    private static final String JEXL_METHOD_WITH_ARGS = "obj.add(10, 20)";
    private static final String JEXL_CHAINED_ACCESS = "obj.getSelf().getSelf().getName()";
    private static final String JEXL_COMPLEX_CHAIN = "obj.nested.nested.publicField";

    @Setup
    public void setup() throws Exception {
        // 初始化测试对象
        testObject = new TestObject();
        testObject.nested = new TestObject();
        testObject.nested.publicField = "level1-value";
        testObject.nested.nested = new TestObject();
        testObject.nested.nested.publicField = "level2-value";

        // ========== JEXL 初始化 ==========
        jexlEngine = new JexlBuilder()
                .cache(512)
                .strict(true)
                .silent(false)
                .create();
        jexlContext = new MapContext();
        jexlContext.set("obj", testObject);

        // 预编译 JEXL 脚本
        jexlFieldAccess = jexlEngine.createScript(JEXL_FIELD_ACCESS);
        jexlMethodNoArgs = jexlEngine.createScript(JEXL_METHOD_NO_ARGS);
        jexlMethodWithArgs = jexlEngine.createScript(JEXL_METHOD_WITH_ARGS);
        jexlChainedAccess = jexlEngine.createScript(JEXL_CHAINED_ACCESS);
        jexlComplexChain = jexlEngine.createScript(JEXL_COMPLEX_CHAIN);

        // ========== Fluxon 解释模式初始化 ==========
        fluxonInterpretEnv = FluxonRuntime.getInstance().newEnvironment();
        fluxonInterpretEnv.defineRootVariable("obj", testObject);

        fluxonParsedFieldAccess = parseFluxon(FLUXON_FIELD_ACCESS);
        fluxonParsedMethodNoArgs = parseFluxon(FLUXON_METHOD_NO_ARGS);
        fluxonParsedMethodWithArgs = parseFluxon(FLUXON_METHOD_WITH_ARGS);
        fluxonParsedChainedAccess = parseFluxon(FLUXON_CHAINED_ACCESS);
        fluxonParsedComplexChain = parseFluxon(FLUXON_COMPLEX_CHAIN);

        // ========== Fluxon 编译模式初始化 ==========
        fluxonCompileEnv = FluxonRuntime.getInstance().newEnvironment();
        fluxonCompileEnv.defineRootVariable("obj", testObject);

        fluxonCompiledFieldAccess = compileFluxon(FLUXON_FIELD_ACCESS);
        fluxonCompiledMethodNoArgs = compileFluxon(FLUXON_METHOD_NO_ARGS);
        fluxonCompiledMethodWithArgs = compileFluxon(FLUXON_METHOD_WITH_ARGS);
        fluxonCompiledChainedAccess = compileFluxon(FLUXON_CHAINED_ACCESS);
        fluxonCompiledComplexChain = compileFluxon(FLUXON_COMPLEX_CHAIN);
    }

    private ParsedScript parseFluxon(String source) {
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        return Fluxon.parse(ctx, fluxonInterpretEnv);
    }

    private RuntimeScriptBase compileFluxon(String source) throws Exception {
        String className = "JexlBenchmark_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("obj", testObject);
        CompileResult result = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = result.defineClass(new FluxonClassLoader());
        return (RuntimeScriptBase) scriptClass.newInstance();
    }

    // ==================== 字段访问 ====================

    @Benchmark
    public void fieldAccess_JavaDirect(Blackhole bh) {
        bh.consume(testObject.publicField);
    }

    @Benchmark
    public void fieldAccess_FluxonCompile(Blackhole bh) {
        bh.consume(fluxonCompiledFieldAccess.eval(fluxonCompileEnv));
    }

    @Benchmark
    public void fieldAccess_FluxonInterpret(Blackhole bh) {
        bh.consume(fluxonParsedFieldAccess.eval(fluxonInterpretEnv));
    }

    @Benchmark
    public void fieldAccess_JexlScript(Blackhole bh) {
        bh.consume(jexlFieldAccess.execute(jexlContext));
    }

    // ==================== 无参方法调用 ====================

    @Benchmark
    public void methodNoArgs_JavaDirect(Blackhole bh) {
        bh.consume(testObject.getName());
    }

    @Benchmark
    public void methodNoArgs_FluxonCompile(Blackhole bh) {
        bh.consume(fluxonCompiledMethodNoArgs.eval(fluxonCompileEnv));
    }

    @Benchmark
    public void methodNoArgs_FluxonInterpret(Blackhole bh) {
        bh.consume(fluxonParsedMethodNoArgs.eval(fluxonInterpretEnv));
    }

    @Benchmark
    public void methodNoArgs_JexlScript(Blackhole bh) {
        bh.consume(jexlMethodNoArgs.execute(jexlContext));
    }

    // ==================== 有参方法调用 ====================

    @Benchmark
    public void methodWithArgs_JavaDirect(Blackhole bh) {
        bh.consume(testObject.add(10, 20));
    }

    @Benchmark
    public void methodWithArgs_FluxonCompile(Blackhole bh) {
        bh.consume(fluxonCompiledMethodWithArgs.eval(fluxonCompileEnv));
    }

    @Benchmark
    public void methodWithArgs_FluxonInterpret(Blackhole bh) {
        bh.consume(fluxonParsedMethodWithArgs.eval(fluxonInterpretEnv));
    }

    @Benchmark
    public void methodWithArgs_JexlScript(Blackhole bh) {
        bh.consume(jexlMethodWithArgs.execute(jexlContext));
    }

    // ==================== 链式调用 ====================

    @Benchmark
    public void chainedAccess_JavaDirect(Blackhole bh) {
        bh.consume(testObject.getSelf().getSelf().getName());
    }

    @Benchmark
    public void chainedAccess_FluxonCompile(Blackhole bh) {
        bh.consume(fluxonCompiledChainedAccess.eval(fluxonCompileEnv));
    }

    @Benchmark
    public void chainedAccess_FluxonInterpret(Blackhole bh) {
        bh.consume(fluxonParsedChainedAccess.eval(fluxonInterpretEnv));
    }

    @Benchmark
    public void chainedAccess_JexlScript(Blackhole bh) {
        bh.consume(jexlChainedAccess.execute(jexlContext));
    }

    // ==================== 复杂链式调用 ====================

    @Benchmark
    public void complexChain_JavaDirect(Blackhole bh) {
        bh.consume(testObject.nested.nested.publicField);
    }

    @Benchmark
    public void complexChain_FluxonCompile(Blackhole bh) {
        bh.consume(fluxonCompiledComplexChain.eval(fluxonCompileEnv));
    }

    @Benchmark
    public void complexChain_FluxonInterpret(Blackhole bh) {
        bh.consume(fluxonParsedComplexChain.eval(fluxonInterpretEnv));
    }

    @Benchmark
    public void complexChain_JexlScript(Blackhole bh) {
        bh.consume(jexlComplexChain.execute(jexlContext));
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(MemberAccessJexlBenchmark.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}
