package org.tabooproject.fluxon.benchmark;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.FluxonRuntimeTest;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class SimpleFluxonJexlBenchmark {

    // JEXL引擎
    private JexlEngine jexlEngine;
    
    // 测试表达式
    private Map<String, String> fluxonExpressions;
    private Map<String, String> jexlExpressions;

    private Environment fluxonEnv;
    private JexlContext jexlContext;
    
    @Setup
    public void setup() {
        FluxonRuntimeTest.registerTestFunctions();
        // 初始化JEXL引擎
        jexlEngine = new JexlBuilder().cache(0).create();
        
        // 初始化测试表达式
        setupExpressions();
        
        // 初始化上下文变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("a", 10);
        variables.put("b", 5);
        variables.put("c", 3);
        variables.put("d", 8);
        variables.put("e", 2);
        variables.put("flag1", true);
        variables.put("flag2", false);
        variables.put("text", "Hello World");
        
        // 创建JEXL上下文
        jexlContext = new MapContext(variables);
        
        // 创建Fluxon环境
        fluxonEnv = FluxonRuntime.getInstance().newEnvironment();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            fluxonEnv.defineRootVariable(entry.getKey(), entry.getValue());
        }
    }
    
    private void setupExpressions() {
        fluxonExpressions = new HashMap<>();
        jexlExpressions = new HashMap<>();
        
        // 1. 简单算术表达式
        fluxonExpressions.put("simpleArithmetic", "&a + &b * &c");
        jexlExpressions.put("simpleArithmetic", "a + b * c");
        
        // 2. 复杂算术表达式
        fluxonExpressions.put("complexArithmetic", "(&a + &b) * &c / (&d - &e)");
        jexlExpressions.put("complexArithmetic", "(a + b) * c / (d - e)");
        
        // 3. 条件表达式
        fluxonExpressions.put("conditional", "if &a > &b then &a * 2 else &b * 3");
        jexlExpressions.put("conditional", "a > b ? a * 2 : b * 3");
        
        // 4. 布尔逻辑
        fluxonExpressions.put("booleanLogic", "&flag1 && !&flag2");
        jexlExpressions.put("booleanLogic", "flag1 && !flag2");
        
        // 5. 简单变量计算
        fluxonExpressions.put("variableCalculation", "&a * &b + &c - &d");
        jexlExpressions.put("variableCalculation", "a * b + c - d");
    }
    
    // Fluxon执行简单算术表达式
    @Benchmark
    public void fluxonSimpleArithmetic(Blackhole blackhole) {
        blackhole.consume(Fluxon.eval(fluxonExpressions.get("simpleArithmetic"), fluxonEnv));
    }
    
    // JEXL执行简单算术表达式
    @Benchmark
    public void jexlSimpleArithmetic(Blackhole blackhole) {
        blackhole.consume(jexlEngine.createExpression(jexlExpressions.get("simpleArithmetic")).evaluate(jexlContext));
    }
    
    // Fluxon执行复杂算术表达式
    @Benchmark
    public void fluxonComplexArithmetic(Blackhole blackhole) {
        blackhole.consume(Fluxon.eval(fluxonExpressions.get("complexArithmetic"), fluxonEnv));
    }
    
    // JEXL执行复杂算术表达式
    @Benchmark
    public void jexlComplexArithmetic(Blackhole blackhole) {
        blackhole.consume(jexlEngine.createExpression(jexlExpressions.get("complexArithmetic")).evaluate(jexlContext));
    }
    
    // Fluxon执行条件表达式
    @Benchmark
    public void fluxonConditional(Blackhole blackhole) {
        blackhole.consume(Fluxon.eval(fluxonExpressions.get("conditional"), fluxonEnv));
    }
    
    // JEXL执行条件表达式
    @Benchmark
    public void jexlConditional(Blackhole blackhole) {
        blackhole.consume(jexlEngine.createExpression(jexlExpressions.get("conditional")).evaluate(jexlContext));
    }
    
    // Fluxon执行布尔逻辑
    @Benchmark
    public void fluxonBooleanLogic(Blackhole blackhole) {
        blackhole.consume(Fluxon.eval(fluxonExpressions.get("booleanLogic"), fluxonEnv));
    }
    
    // JEXL执行布尔逻辑
    @Benchmark
    public void jexlBooleanLogic(Blackhole blackhole) {
        blackhole.consume(jexlEngine.createExpression(jexlExpressions.get("booleanLogic")).evaluate(jexlContext));
    }
    
    // Fluxon执行简单变量计算
    @Benchmark
    public void fluxonVariableCalculation(Blackhole blackhole) {
        blackhole.consume(Fluxon.eval(fluxonExpressions.get("variableCalculation"), fluxonEnv));
    }
    
    // JEXL执行简单变量计算
    @Benchmark
    public void jexlVariableCalculation(Blackhole blackhole) {
        blackhole.consume(jexlEngine.createExpression(jexlExpressions.get("variableCalculation")).evaluate(jexlContext));
    }
    
    // Fluxon执行混合测试
    @Benchmark
    public void fluxonMixed(Blackhole blackhole) {
        blackhole.consume(Fluxon.eval(fluxonExpressions.get("simpleArithmetic"), fluxonEnv));
        blackhole.consume(Fluxon.eval(fluxonExpressions.get("conditional"), fluxonEnv));
        blackhole.consume(Fluxon.eval(fluxonExpressions.get("booleanLogic"), fluxonEnv));
    }
    
    // JEXL执行混合测试
    @Benchmark
    public void jexlMixed(Blackhole blackhole) {
        blackhole.consume(jexlEngine.createExpression(jexlExpressions.get("simpleArithmetic")).evaluate(jexlContext));
        blackhole.consume(jexlEngine.createExpression(jexlExpressions.get("conditional")).evaluate(jexlContext));
        blackhole.consume(jexlEngine.createExpression(jexlExpressions.get("booleanLogic")).evaluate(jexlContext));
    }
    
    // 主方法，用于运行基准测试
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(SimpleFluxonJexlBenchmark.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}