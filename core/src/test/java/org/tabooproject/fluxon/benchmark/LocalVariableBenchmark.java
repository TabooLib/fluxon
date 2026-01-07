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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 变量访问性能基准测试
 * 对比 "_" 前缀变量（数组索引访问）和普通变量（HashMap 查找）的读写性能差异
 * <p>
 * - 普通变量：存储在 rootVariables（HashMap），通过哈希查找
 * - "_" 前缀变量：存储在 localVariables（数组），通过索引访问
 *
 * @author sky
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class LocalVariableBenchmark {

    private static final AtomicInteger CLASS_COUNTER = new AtomicInteger(0);

    // ========== 测试表达式 ==========
    
    // 单次读取
    private static final String EXPR_LOCAL_READ = "_x = 100; &_x";
    private static final String EXPR_ROOT_READ = "x = 100; &x";
    
    // 单次写入（多次赋值）
    private static final String EXPR_LOCAL_WRITE = "_x = 1; _x = 2; _x = 3; _x = 4; _x = 5; &_x";
    private static final String EXPR_ROOT_WRITE = "x = 1; x = 2; x = 3; x = 4; x = 5; &x";
    
    // 多次读取
    private static final String EXPR_LOCAL_MULTI_READ = "_x = 100; &_x + &_x + &_x + &_x + &_x";
    private static final String EXPR_ROOT_MULTI_READ = "x = 100; &x + &x + &x + &x + &x";
    
    // 复合操作（读写混合）
    private static final String EXPR_LOCAL_COMPOUND = "_x = 0; _x += 1; _x += 2; _x += 3; &_x";
    private static final String EXPR_ROOT_COMPOUND = "x = 0; x += 1; x += 2; x += 3; &x";
    
    // 多变量场景
    private static final String EXPR_LOCAL_MULTI_VAR = "_a = 1; _b = 2; _c = 3; _d = 4; &_a + &_b + &_c + &_d";
    private static final String EXPR_ROOT_MULTI_VAR = "a = 1; b = 2; c = 3; d = 4; &a + &b + &c + &d";
    
    // 循环场景（密集读写）
    private static final String EXPR_LOCAL_LOOP = "_sum = 0; for i in [1,2,3,4,5,6,7,8,9,10] { _sum = &_sum + &i }; &_sum";
    private static final String EXPR_ROOT_LOOP = "sum = 0; for i in [1,2,3,4,5,6,7,8,9,10] { sum = &sum + &i }; &sum";

    // ========== 解释模式预解析结果 ==========
    private ParsedScript parsedLocalRead;
    private ParsedScript parsedRootRead;
    private ParsedScript parsedLocalWrite;
    private ParsedScript parsedRootWrite;
    private ParsedScript parsedLocalMultiRead;
    private ParsedScript parsedRootMultiRead;
    private ParsedScript parsedLocalCompound;
    private ParsedScript parsedRootCompound;
    private ParsedScript parsedLocalMultiVar;
    private ParsedScript parsedRootMultiVar;
    private ParsedScript parsedLocalLoop;
    private ParsedScript parsedRootLoop;

    // ========== 编译模式预编译脚本 ==========
    private RuntimeScriptBase compiledLocalRead;
    private RuntimeScriptBase compiledRootRead;
    private RuntimeScriptBase compiledLocalWrite;
    private RuntimeScriptBase compiledRootWrite;
    private RuntimeScriptBase compiledLocalMultiRead;
    private RuntimeScriptBase compiledRootMultiRead;
    private RuntimeScriptBase compiledLocalCompound;
    private RuntimeScriptBase compiledRootCompound;
    private RuntimeScriptBase compiledLocalMultiVar;
    private RuntimeScriptBase compiledRootMultiVar;
    private RuntimeScriptBase compiledLocalLoop;
    private RuntimeScriptBase compiledRootLoop;

    @Setup
    public void setup() throws Exception {
        // 预解析所有表达式（解释模式）
        parsedLocalRead = parseExpression(EXPR_LOCAL_READ);
        parsedRootRead = parseExpression(EXPR_ROOT_READ);
        parsedLocalWrite = parseExpression(EXPR_LOCAL_WRITE);
        parsedRootWrite = parseExpression(EXPR_ROOT_WRITE);
        parsedLocalMultiRead = parseExpression(EXPR_LOCAL_MULTI_READ);
        parsedRootMultiRead = parseExpression(EXPR_ROOT_MULTI_READ);
        parsedLocalCompound = parseExpression(EXPR_LOCAL_COMPOUND);
        parsedRootCompound = parseExpression(EXPR_ROOT_COMPOUND);
        parsedLocalMultiVar = parseExpression(EXPR_LOCAL_MULTI_VAR);
        parsedRootMultiVar = parseExpression(EXPR_ROOT_MULTI_VAR);
        parsedLocalLoop = parseExpression(EXPR_LOCAL_LOOP);
        parsedRootLoop = parseExpression(EXPR_ROOT_LOOP);

        // 预编译所有表达式（编译模式）
        compiledLocalRead = compileExpression(EXPR_LOCAL_READ);
        compiledRootRead = compileExpression(EXPR_ROOT_READ);
        compiledLocalWrite = compileExpression(EXPR_LOCAL_WRITE);
        compiledRootWrite = compileExpression(EXPR_ROOT_WRITE);
        compiledLocalMultiRead = compileExpression(EXPR_LOCAL_MULTI_READ);
        compiledRootMultiRead = compileExpression(EXPR_ROOT_MULTI_READ);
        compiledLocalCompound = compileExpression(EXPR_LOCAL_COMPOUND);
        compiledRootCompound = compileExpression(EXPR_ROOT_COMPOUND);
        compiledLocalMultiVar = compileExpression(EXPR_LOCAL_MULTI_VAR);
        compiledRootMultiVar = compileExpression(EXPR_ROOT_MULTI_VAR);
        compiledLocalLoop = compileExpression(EXPR_LOCAL_LOOP);
        compiledRootLoop = compileExpression(EXPR_ROOT_LOOP);
    }

    private ParsedScript parseExpression(String source) {
        CompilationContext ctx = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        return Fluxon.parse(ctx, env);
    }

    private RuntimeScriptBase compileExpression(String source) throws Exception {
        String className = "Benchmark_" + CLASS_COUNTER.incrementAndGet();
        CompilationContext ctx = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        CompileResult result = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = result.defineClass(new FluxonClassLoader());
        return (RuntimeScriptBase) scriptClass.newInstance();
    }

    // ========== 单次读取基准测试 ==========

    @Benchmark
    public void singleRead_Local_Interpret(Blackhole bh) {
        bh.consume(parsedLocalRead.eval());
    }

    @Benchmark
    public void singleRead_Root_Interpret(Blackhole bh) {
        bh.consume(parsedRootRead.eval());
    }

    @Benchmark
    public void singleRead_Local_Compile(Blackhole bh) {
        bh.consume(compiledLocalRead.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    @Benchmark
    public void singleRead_Root_Compile(Blackhole bh) {
        bh.consume(compiledRootRead.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    // ========== 多次写入基准测试 ==========

    @Benchmark
    public void multiWrite_Local_Interpret(Blackhole bh) {
        bh.consume(parsedLocalWrite.eval());
    }

    @Benchmark
    public void multiWrite_Root_Interpret(Blackhole bh) {
        bh.consume(parsedRootWrite.eval());
    }

    @Benchmark
    public void multiWrite_Local_Compile(Blackhole bh) {
        bh.consume(compiledLocalWrite.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    @Benchmark
    public void multiWrite_Root_Compile(Blackhole bh) {
        bh.consume(compiledRootWrite.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    // ========== 多次读取基准测试 ==========

    @Benchmark
    public void multiRead_Local_Interpret(Blackhole bh) {
        bh.consume(parsedLocalMultiRead.eval());
    }

    @Benchmark
    public void multiRead_Root_Interpret(Blackhole bh) {
        bh.consume(parsedRootMultiRead.eval());
    }

    @Benchmark
    public void multiRead_Local_Compile(Blackhole bh) {
        bh.consume(compiledLocalMultiRead.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    @Benchmark
    public void multiRead_Root_Compile(Blackhole bh) {
        bh.consume(compiledRootMultiRead.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    // ========== 复合操作基准测试 ==========

    @Benchmark
    public void compound_Local_Interpret(Blackhole bh) {
        bh.consume(parsedLocalCompound.eval());
    }

    @Benchmark
    public void compound_Root_Interpret(Blackhole bh) {
        bh.consume(parsedRootCompound.eval());
    }

    @Benchmark
    public void compound_Local_Compile(Blackhole bh) {
        bh.consume(compiledLocalCompound.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    @Benchmark
    public void compound_Root_Compile(Blackhole bh) {
        bh.consume(compiledRootCompound.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    // ========== 多变量基准测试 ==========

    @Benchmark
    public void multiVar_Local_Interpret(Blackhole bh) {
        bh.consume(parsedLocalMultiVar.eval());
    }

    @Benchmark
    public void multiVar_Root_Interpret(Blackhole bh) {
        bh.consume(parsedRootMultiVar.eval());
    }

    @Benchmark
    public void multiVar_Local_Compile(Blackhole bh) {
        bh.consume(compiledLocalMultiVar.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    @Benchmark
    public void multiVar_Root_Compile(Blackhole bh) {
        bh.consume(compiledRootMultiVar.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    // ========== 循环场景基准测试 ==========

    @Benchmark
    public void loop_Local_Interpret(Blackhole bh) {
        bh.consume(parsedLocalLoop.eval());
    }

    @Benchmark
    public void loop_Root_Interpret(Blackhole bh) {
        bh.consume(parsedRootLoop.eval());
    }

    @Benchmark
    public void loop_Local_Compile(Blackhole bh) {
        bh.consume(compiledLocalLoop.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    @Benchmark
    public void loop_Root_Compile(Blackhole bh) {
        bh.consume(compiledRootLoop.eval(FluxonRuntime.getInstance().newEnvironment()));
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(LocalVariableBenchmark.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}
