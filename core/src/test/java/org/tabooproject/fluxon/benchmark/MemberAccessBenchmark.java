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
import org.tabooproject.fluxon.type.TestObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Member Access 性能基准测试
 * 对比解释执行和编译执行在成员访问场景下的性能差异
 * <p>
 * 一期测试:
 * Benchmark                                        Mode  Cnt    Score     Error  Units
 * MemberAccessBenchmark.chainedAccess_Compile      avgt    5   66.533 ±   9.235  ns/op
 * MemberAccessBenchmark.chainedAccess_Interpret    avgt    5  191.598 ± 107.448  ns/op
 * MemberAccessBenchmark.chainedAccess_JavaDirect   avgt    5    2.225 ±   1.044  ns/op
 * MemberAccessBenchmark.complexChain_Compile       avgt    5  193.981 ±  57.771  ns/op
 * MemberAccessBenchmark.complexChain_Interpret     avgt    5  222.043 ±  20.638  ns/op
 * MemberAccessBenchmark.fieldAccess_Compile        avgt    5   23.421 ±   7.451  ns/op
 * MemberAccessBenchmark.fieldAccess_Interpret      avgt    5   82.257 ±  31.786  ns/op
 * MemberAccessBenchmark.fieldAccess_JavaDirect     avgt    5    2.284 ±   0.196  ns/op
 * MemberAccessBenchmark.methodNoArgs_Compile       avgt    5   35.294 ±  25.720  ns/op
 * MemberAccessBenchmark.methodNoArgs_Interpret     avgt    5  105.046 ±  45.424  ns/op
 * MemberAccessBenchmark.methodNoArgs_JavaDirect    avgt    5    2.140 ±   0.389  ns/op
 * MemberAccessBenchmark.methodOverload_Compile     avgt    5   53.877 ±  10.171  ns/op
 * MemberAccessBenchmark.methodOverload_Interpret   avgt    5  141.183 ±  15.265  ns/op
 * MemberAccessBenchmark.methodWithArgs_Compile     avgt    5   43.761 ±   6.563  ns/op
 * MemberAccessBenchmark.methodWithArgs_Interpret   avgt    5  133.324 ±  55.952  ns/op
 * MemberAccessBenchmark.methodWithArgs_JavaDirect  avgt    5    2.049 ±   1.447  ns/op
 * <p>
 * 二期测试：
 * Benchmark                                        Mode  Cnt    Score     Error  Units
 * MemberAccessBenchmark.chainedAccess_Compile      avgt    5   10.210 ±   3.302  ns/op
 * MemberAccessBenchmark.chainedAccess_Interpret    avgt    5  189.081 ±  90.190  ns/op
 * MemberAccessBenchmark.chainedAccess_JavaDirect   avgt    5    2.141 ±   1.501  ns/op
 * MemberAccessBenchmark.complexChain_Compile       avgt    5    8.811 ±   3.078  ns/op
 * MemberAccessBenchmark.complexChain_Interpret     avgt    5  125.742 ±  53.574  ns/op
 * MemberAccessBenchmark.complexChain_JavaDirect    avgt    5    2.714 ±   1.835  ns/op
 * MemberAccessBenchmark.fieldAccess_Compile        avgt    5   11.155 ±   3.540  ns/op
 * MemberAccessBenchmark.fieldAccess_Interpret      avgt    5   80.267 ±  28.187  ns/op
 * MemberAccessBenchmark.fieldAccess_JavaDirect     avgt    5    2.083 ±   0.594  ns/op
 * MemberAccessBenchmark.methodNoArgs_Compile       avgt    5   10.062 ±   2.592  ns/op
 * MemberAccessBenchmark.methodNoArgs_Interpret     avgt    5  143.149 ±  90.520  ns/op
 * MemberAccessBenchmark.methodNoArgs_JavaDirect    avgt    5    2.398 ±   0.947  ns/op
 * MemberAccessBenchmark.methodOverload_Compile     avgt    5   18.592 ±   2.822  ns/op
 * MemberAccessBenchmark.methodOverload_Interpret   avgt    5  148.093 ±  13.844  ns/op
 * MemberAccessBenchmark.methodWithArgs_Compile     avgt    5    9.771 ±   1.286  ns/op
 * MemberAccessBenchmark.methodWithArgs_Interpret   avgt    5  191.480 ± 185.897  ns/op
 * MemberAccessBenchmark.methodWithArgs_JavaDirect  avgt    5    2.085 ±   0.715  ns/op
 * <p>
 * 三期测试：
 * Benchmark                                        Mode  Cnt    Score    Error  Units
 * MemberAccessBenchmark.chainedAccess_Compile      avgt    5    8.547 ±  4.538  ns/op
 * MemberAccessBenchmark.chainedAccess_Interpret    avgt    5  181.284 ± 62.816  ns/op
 * MemberAccessBenchmark.chainedAccess_JavaDirect   avgt    5    2.099 ±  1.139  ns/op
 * MemberAccessBenchmark.complexChain_Compile       avgt    5    8.924 ±  3.651  ns/op
 * MemberAccessBenchmark.complexChain_Interpret     avgt    5  119.497 ± 88.123  ns/op
 * MemberAccessBenchmark.complexChain_JavaDirect    avgt    5    2.406 ±  0.977  ns/op
 * MemberAccessBenchmark.fieldAccess_Compile        avgt    5   10.430 ±  6.250  ns/op
 * MemberAccessBenchmark.fieldAccess_Interpret      avgt    5   81.841 ± 46.202  ns/op
 * MemberAccessBenchmark.fieldAccess_JavaDirect     avgt    5    2.084 ±  0.927  ns/op
 * MemberAccessBenchmark.methodNoArgs_Compile       avgt    5    8.517 ±  4.173  ns/op
 * MemberAccessBenchmark.methodNoArgs_Interpret     avgt    5   99.027 ± 54.215  ns/op
 * MemberAccessBenchmark.methodNoArgs_JavaDirect    avgt    5    2.109 ±  1.093  ns/op
 * MemberAccessBenchmark.methodOverload_Compile     avgt    5   16.979 ± 10.130  ns/op
 * MemberAccessBenchmark.methodOverload_Interpret   avgt    5  129.827 ± 84.681  ns/op
 * MemberAccessBenchmark.methodWithArgs_Compile     avgt    5   12.335 ±  5.263  ns/op
 * MemberAccessBenchmark.methodWithArgs_Interpret   avgt    5  138.368 ± 38.717  ns/op
 * MemberAccessBenchmark.methodWithArgs_JavaDirect  avgt    5    2.112 ±  0.368  ns/op
 *
 * @author sky
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@SuppressWarnings("deprecation")
public class MemberAccessBenchmark {

    private static final AtomicInteger CLASS_COUNTER = new AtomicInteger(0);

    // ========== 测试对象 ==========
    private TestObject testObject;

    // ========== 解释模式预解析结果 ==========
    private ParsedScript parsedFieldAccess;
    private ParsedScript parsedMethodNoArgs;
    private ParsedScript parsedMethodWithArgs;
    private ParsedScript parsedMethodOverload;
    private ParsedScript parsedChainedAccess;
    private ParsedScript parsedComplexChain;

    // ========== 编译模式预编译脚本 ==========
    private RuntimeScriptBase compiledFieldAccess;
    private RuntimeScriptBase compiledMethodNoArgs;
    private RuntimeScriptBase compiledMethodWithArgs;
    private RuntimeScriptBase compiledMethodOverload;
    private RuntimeScriptBase compiledChainedAccess;
    private RuntimeScriptBase compiledComplexChain;

    // ========== 环境 ==========
    private Environment interpretEnv;
    private Environment compileEnv;

    // ========== 测试表达式 ==========
    private static final String EXPR_FIELD_ACCESS = "&obj.publicField";
    private static final String EXPR_METHOD_NO_ARGS = "&obj.getName()";
    private static final String EXPR_METHOD_WITH_ARGS = "&obj.add(10, 20)";
    private static final String EXPR_METHOD_OVERLOAD = "&obj.process('hello')";
    private static final String EXPR_CHAINED_ACCESS = "&obj.getSelf().getSelf().getName()";
    // 使用预创建的嵌套对象，避免 getLevel1() 每次创建新对象的开销
    private static final String EXPR_COMPLEX_CHAIN = "&obj.nested.nested.publicField";

    @Setup
    public void setup() throws Exception {
        testObject = new TestObject();
        // 预创建嵌套对象结构
        testObject.nested = new TestObject();
        testObject.nested.publicField = "level1-value";
        testObject.nested.nested = new TestObject();
        testObject.nested.nested.publicField = "level2-value";

        // 初始化解释环境
        interpretEnv = FluxonRuntime.getInstance().newEnvironment();
        interpretEnv.defineRootVariable("obj", testObject);

        // 初始化编译环境
        compileEnv = FluxonRuntime.getInstance().newEnvironment();
        compileEnv.defineRootVariable("obj", testObject);

        // 预解析所有表达式（解释模式）
        parsedFieldAccess = parseExpression(EXPR_FIELD_ACCESS);
        parsedMethodNoArgs = parseExpression(EXPR_METHOD_NO_ARGS);
        parsedMethodWithArgs = parseExpression(EXPR_METHOD_WITH_ARGS);
        parsedMethodOverload = parseExpression(EXPR_METHOD_OVERLOAD);
        parsedChainedAccess = parseExpression(EXPR_CHAINED_ACCESS);
        parsedComplexChain = parseExpression(EXPR_COMPLEX_CHAIN);

        // 预编译所有表达式（编译模式）
        compiledFieldAccess = compileExpression(EXPR_FIELD_ACCESS);
        compiledMethodNoArgs = compileExpression(EXPR_METHOD_NO_ARGS);
        compiledMethodWithArgs = compileExpression(EXPR_METHOD_WITH_ARGS);
        compiledMethodOverload = compileExpression(EXPR_METHOD_OVERLOAD);
        compiledChainedAccess = compileExpression(EXPR_CHAINED_ACCESS);
        compiledComplexChain = compileExpression(EXPR_COMPLEX_CHAIN);
    }

    private ParsedScript parseExpression(String source) {
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        return Fluxon.parse(ctx, interpretEnv);
    }

    private RuntimeScriptBase compileExpression(String source) throws Exception {
        String className = "Benchmark_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("obj", testObject);
        CompileResult result = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = result.defineClass(new FluxonClassLoader());
        return (RuntimeScriptBase) scriptClass.newInstance();
    }

    // ========== 字段访问基准测试 ==========

    @Benchmark
    public void fieldAccess_Interpret(Blackhole bh) {
        bh.consume(parsedFieldAccess.eval(interpretEnv));
    }

    @Benchmark
    public void fieldAccess_Compile(Blackhole bh) {
        bh.consume(compiledFieldAccess.eval(compileEnv));
    }

    // ========== 无参方法调用基准测试 ==========

    @Benchmark
    public void methodNoArgs_Interpret(Blackhole bh) {
        bh.consume(parsedMethodNoArgs.eval(interpretEnv));
    }

    @Benchmark
    public void methodNoArgs_Compile(Blackhole bh) {
        bh.consume(compiledMethodNoArgs.eval(compileEnv));
    }

    // ========== 带参数方法调用基准测试 ==========

    @Benchmark
    public void methodWithArgs_Interpret(Blackhole bh) {
        bh.consume(parsedMethodWithArgs.eval(interpretEnv));
    }

    @Benchmark
    public void methodWithArgs_Compile(Blackhole bh) {
        bh.consume(compiledMethodWithArgs.eval(compileEnv));
    }

    // ========== 重载方法调用基准测试 ==========

    @Benchmark
    public void methodOverload_Interpret(Blackhole bh) {
        bh.consume(parsedMethodOverload.eval(interpretEnv));
    }

    @Benchmark
    public void methodOverload_Compile(Blackhole bh) {
        bh.consume(compiledMethodOverload.eval(compileEnv));
    }

    // ========== 链式调用基准测试 ==========

    @Benchmark
    public void chainedAccess_Interpret(Blackhole bh) {
        bh.consume(parsedChainedAccess.eval(interpretEnv));
    }

    @Benchmark
    public void chainedAccess_Compile(Blackhole bh) {
        bh.consume(compiledChainedAccess.eval(compileEnv));
    }

    // ========== 复杂链式调用基准测试 ==========

    @Benchmark
    public void complexChain_Interpret(Blackhole bh) {
        bh.consume(parsedComplexChain.eval(interpretEnv));
    }

    @Benchmark
    public void complexChain_Compile(Blackhole bh) {
        bh.consume(compiledComplexChain.eval(compileEnv));
    }

    // ========== 直接 Java 调用（作为基准对照）==========

    @Benchmark
    public void fieldAccess_JavaDirect(Blackhole bh) {
        bh.consume(testObject.publicField);
    }

    @Benchmark
    public void methodNoArgs_JavaDirect(Blackhole bh) {
        bh.consume(testObject.getName());
    }

    @Benchmark
    public void methodWithArgs_JavaDirect(Blackhole bh) {
        bh.consume(testObject.add(10, 20));
    }

    @Benchmark
    public void chainedAccess_JavaDirect(Blackhole bh) {
        bh.consume(testObject.getSelf().getSelf().getName());
    }

    @Benchmark
    public void complexChain_JavaDirect(Blackhole bh) {
        bh.consume(testObject.nested.nested.publicField);
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(MemberAccessBenchmark.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}
