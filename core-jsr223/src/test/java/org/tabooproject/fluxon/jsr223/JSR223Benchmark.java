package org.tabooproject.fluxon.jsr223;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.FunctionContextPool;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;

import javax.script.*;
import java.util.concurrent.TimeUnit;

/**
 * JSR-223 vs Fluxon API 性能基准测试
 *
 * <h2>基准测试结果</h2>
 * <pre>
 * Benchmark                                     Mode  Cnt      Score       Error  Units
 * fluxon_compiled_reuseEnv                      avgt    5     10.923 ±     1.988  ns/op  <- Fluxon 最佳
 * fluxon_callFunction                           avgt    5     32.977 ±    10.641  ns/op
 * jsr223_invokeFunction                         avgt    5     44.297 ±     2.743  ns/op
 * fluxon_compiled_withVars                      avgt    5    396.867 ±    27.974  ns/op
 * jsr223_compiled_evalFast                      avgt    5   1422.132 ±   505.700  ns/op  <- JSR-223 最佳
 * jsr223_compiled_withVars                      avgt    5   1629.975 ±    68.950  ns/op
 * fluxon_interpret_withVars                     avgt    5   4701.893 ±  1963.923  ns/op
 * jsr223_interpret_withVars                     avgt    5  10783.231 ±  1380.677  ns/op
 * </pre>
 *
 * <h2>性能对比</h2>
 * <pre>
 * | 场景                  | JSR-223    | Fluxon API | 倍数  |
 * |-----------------------|------------|------------|-------|
 * | 编译执行              | 1,630 ns   | 397 ns     | 4.1x  |
 * | 编译执行(evalFast)    | 1,422 ns   | -          | 3.6x  |
 * | 编译执行(复用env)     | -          | 11 ns      | -     |
 * | 解释执行              | 10,783 ns  | 4,702 ns   | 2.3x  |
 * | 函数调用              | 44 ns      | 33 ns      | 1.3x  |
 * </pre>
 *
 * <h2>使用注意事项</h2>
 *
 * <h3>1. 选择合适的 API</h3>
 * <pre>
 * // 简单场景：JSR-223 足够
 * ScriptEngine engine = new ScriptEngineManager().getEngineByName("fluxon");
 * engine.eval("1 + 2");
 *
 * // 高性能场景：直接用 Fluxon API
 * Environment env = FluxonRuntime.getInstance().newEnvironment();
 * RuntimeScriptBase script = ...;
 * script.eval(env);  // 10ns 级别
 * </pre>
 *
 * <h3>2. 预编译 + evalFast（推荐）</h3>
 * <pre>
 * // 预编译 + evalFast 跳过变量回写，比 eval() 快约 15%
 * FluxonCompiledScript compiled = (FluxonCompiledScript) engine.compile(script);
 * compiled.evalFast();
 * </pre>
 *
 * <h3>3. 启用编译缓存</h3>
 * <pre>
 * engine.put("fluxon.compile.cache", true);
 * // 相同脚本只编译一次
 * </pre>
 *
 * <h3>4. 执行成本限制（防止死循环）</h3>
 * <pre>
 * engine.put("fluxon.cost.limit", 10000L);
 * </pre>
 *
 * <h3>5. 函数调用几乎无额外开销</h3>
 * <pre>
 * engine.eval("def add(a, b) = &a + &b");
 * Invocable inv = (Invocable) engine;
 * inv.invokeFunction("add", 1, 2);  // 44ns，与 Fluxon API 接近
 * </pre>
 *
 * <h2>性能瓶颈说明</h2>
 * <pre>
 * | 开销来源              | 耗时      | 原因                           |
 * |-----------------------|-----------|--------------------------------|
 * | Environment 创建      | ~400ns    | JSR-223 语义要求执行隔离        |
 * | 变量注入              | ~300ns    | 需遍历 Bindings                 |
 * | 变量回写              | ~200ns    | 可用 evalFast() 跳过            |
 * </pre>
 *
 * <h2>场景推荐</h2>
 * <pre>
 * | 场景                   | 推荐方案                        |
 * |------------------------|---------------------------------|
 * | 框架集成（Spring 等）  | JSR-223                         |
 * | 一次性脚本执行         | JSR-223                         |
 * | 热点循环内执行         | Fluxon API                      |
 * | 需要最大吞吐量         | Fluxon API + 复用 Environment   |
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class JSR223Benchmark {

    // ==================== 测试脚本 ====================

    private static final String SIMPLE_EXPR = "1 + 2 * 3";
    private static final String VAR_EXPR = "&x + &y * 2";
    private static final String FUNC_DEF = "def add(a, b) = &a + &b";
    private static final String FUNC_CALL = "add(1, 2)";

    // ==================== JSR-223 对象 ====================

    private ScriptEngine jsr223Engine;
    private CompiledScript jsr223Compiled;
    private CompiledScript jsr223CompiledWithVars;

    // ==================== Fluxon API 对象 ====================

    private Environment fluxonEnv;
    private Interpreter fluxonInterpreter;
    private RuntimeScriptBase fluxonCompiled;
    private RuntimeScriptBase fluxonCompiledWithVars;
    private CompileResult fluxonCompileResult;
    private CompileResult fluxonCompileResultWithVars;
    private Function fluxonAddFunction;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // ========== JSR-223 初始化 ==========
        jsr223Engine = new FluxonScriptEngine(new FluxonScriptEngineFactory());
        jsr223Engine.put("x", 10);
        jsr223Engine.put("y", 20);

        // 预编译
        jsr223Compiled = ((Compilable) jsr223Engine).compile(SIMPLE_EXPR);
        jsr223CompiledWithVars = ((Compilable) jsr223Engine).compile(VAR_EXPR);

        // 定义函数
        jsr223Engine.eval(FUNC_DEF);

        // ========== Fluxon API 初始化 ==========
        fluxonEnv = FluxonRuntime.getInstance().newEnvironment();
        fluxonEnv.defineRootVariable("x", 10);
        fluxonEnv.defineRootVariable("y", 20);

        // 预编译
        fluxonCompileResult = Fluxon.compile(SIMPLE_EXPR, "BenchSimple");
        Class<?> simpleClass = fluxonCompileResult.defineClass(new FluxonClassLoader());
        fluxonCompiled = (RuntimeScriptBase) simpleClass.getDeclaredConstructor().newInstance();

        fluxonCompileResultWithVars = Fluxon.compile(VAR_EXPR, "BenchVars", fluxonEnv);
        Class<?> varsClass = fluxonCompileResultWithVars.defineClass(new FluxonClassLoader());
        fluxonCompiledWithVars = (RuntimeScriptBase) varsClass.getDeclaredConstructor().newInstance();

        // 定义函数
        Fluxon.eval(FUNC_DEF, fluxonEnv);
        fluxonAddFunction = fluxonEnv.getFunction("add");
    }

    // ==================== 简单表达式：解释执行 ====================

    @Benchmark
    public void jsr223_interpret_simple(Blackhole bh) throws ScriptException {
        bh.consume(jsr223Engine.eval(SIMPLE_EXPR));
    }

    @Benchmark
    public void fluxon_interpret_simple(Blackhole bh) {
        bh.consume(Fluxon.eval(SIMPLE_EXPR));
    }

    // ==================== 简单表达式：编译执行 ====================

    @Benchmark
    public void jsr223_compiled_simple(Blackhole bh) throws ScriptException {
        bh.consume(jsr223Compiled.eval());
    }

    @Benchmark
    public void fluxon_compiled_simple(Blackhole bh) {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        bh.consume(fluxonCompiled.eval(env));
    }

    // ==================== 带变量表达式：解释执行 ====================

    @Benchmark
    public void jsr223_interpret_withVars(Blackhole bh) throws ScriptException {
        // JSR-223 内部每次创建新 Environment
        bh.consume(jsr223Engine.eval(VAR_EXPR));
    }

    @Benchmark
    public void fluxon_interpret_withVars(Blackhole bh) {
        // 每次创建新环境（与 JSR-223 行为一致）
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("x", 10);
        env.defineRootVariable("y", 20);
        bh.consume(Fluxon.eval(SIMPLE_EXPR, env));
    }

    // ==================== 带变量表达式：编译执行 ====================

    @Benchmark
    public void jsr223_compiled_withVars(Blackhole bh) throws ScriptException {
        bh.consume(jsr223CompiledWithVars.eval());
    }

    @Benchmark
    public void jsr223_compiled_evalFast(Blackhole bh) throws ScriptException {
        // 使用 evalFast 跳过变量回写
        bh.consume(((FluxonCompiledScript) jsr223CompiledWithVars).evalFast());
    }

    @Benchmark
    public void fluxon_compiled_withVars(Blackhole bh) {
        // 每次创建新环境（与 JSR-223 行为一致）
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("x", 10);
        env.defineRootVariable("y", 20);
        bh.consume(fluxonCompiledWithVars.eval(env));
    }

    // ==================== 环境复用场景（Fluxon API 优势场景） ====================

    @Benchmark
    public void fluxon_compiled_reuseEnv(Blackhole bh) {
        // 复用已有环境 - 展示 Fluxon API 的最佳性能
        bh.consume(fluxonCompiledWithVars.eval(fluxonEnv));
    }

    // ==================== 函数调用 ====================

    @Benchmark
    public void jsr223_invokeFunction(Blackhole bh) throws ScriptException, NoSuchMethodException {
        bh.consume(((Invocable) jsr223Engine).invokeFunction("add", 1, 2));
    }

    @Benchmark
    public void fluxon_callFunction(Blackhole bh) {
        FunctionContextPool pool = FunctionContextPool.local();
        try (FunctionContext<?> ctx = pool.borrow(fluxonAddFunction, null, new Object[]{1, 2}, fluxonEnv)) {
            bh.consume(fluxonAddFunction.call(ctx));
        }
    }

    // ==================== 变量注入开销隔离测试 ====================

    @Benchmark
    public void jsr223_varInjection_overhead(Blackhole bh) throws ScriptException {
        // 每次创建新 engine 模拟完全隔离
        ScriptEngine engine = new FluxonScriptEngine(new FluxonScriptEngineFactory());
        engine.put("x", 10);
        engine.put("y", 20);
        bh.consume(engine.eval(VAR_EXPR));
    }

    @Benchmark
    public void fluxon_varInjection_overhead(Blackhole bh) {
        // 每次创建新环境
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("x", 10);
        env.defineRootVariable("y", 20);
        bh.consume(Fluxon.eval(VAR_EXPR, env));
    }

    // ==================== 主方法：可直接运行 ====================

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
