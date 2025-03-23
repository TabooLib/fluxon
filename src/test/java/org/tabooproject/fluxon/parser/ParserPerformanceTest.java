package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parser 性能测试类
 */
public class ParserPerformanceTest {

    private static final int TEST_ITERATIONS = 10;
    private static final int LARGE_CODE_REPETITIONS = 1000;

    private Lexer lexer;
    private Parser parser;

    @BeforeEach
    public void setUp() {
        lexer = new Lexer();
        parser = new Parser();
    }

    /**
     * 解析源代码
     *
     * @param source 源代码
     * @return 解析结果列表
     */
    private List<ParseResult> parseSource(String source) {
        // 创建编译上下文
        CompilationContext context = new CompilationContext(source);
        List<Token> tokens = lexer.process(context);
        context.setAttribute("tokens", tokens);
        return parser.process(context);
    }

    /**
     * 测量执行时间
     *
     * @param runnable 要执行的代码
     * @return 执行时间（纳秒）
     */
    private long measureExecutionTime(Runnable runnable) {
        long startTime = System.nanoTime();
        runnable.run();
        return System.nanoTime() - startTime;
    }

    /**
     * 格式化执行时间
     *
     * @param nanos 纳秒
     * @return 格式化后的时间字符串
     */
    private String formatExecutionTime(long nanos) {
        if (nanos < 1_000) {
            return nanos + " ns";
        } else if (nanos < 1_000_000) {
            return String.format("%.2f µs", nanos / 1_000.0);
        } else if (nanos < 1_000_000_000) {
            return String.format("%.2f ms", nanos / 1_000_000.0);
        } else {
            return String.format("%.2f s", nanos / 1_000_000_000.0);
        }
    }

    /**
     * 测试简单函数定义的解析性能
     */
    @RepeatedTest(TEST_ITERATIONS)
    @DisplayName("简单函数定义解析性能测试")
    public void testSimpleFunctionDefinitionPerformance() {
        String source = "def factorial(n) = n";
        
        long executionTime = measureExecutionTime(() -> {
            List<ParseResult> results = parseSource(source);
            assertNotNull(results);
            assertTrue(results.size() > 0);
        });
        
        System.out.println("简单函数定义解析耗时: " + formatExecutionTime(executionTime));
    }

    /**
     * 测试复杂函数定义的解析性能
     */
    @RepeatedTest(TEST_ITERATIONS)
    @DisplayName("复杂函数定义解析性能测试")
    public void testComplexFunctionDefinitionPerformance() {
        String source = "def factorial(n) = if &n <= 1 then 1 else &n * factorial(&n - 1)";
        
        long executionTime = measureExecutionTime(() -> {
            List<ParseResult> results = parseSource(source);
            assertNotNull(results);
            assertTrue(results.size() > 0);
        });
        
        System.out.println("复杂函数定义解析耗时: " + formatExecutionTime(executionTime));
    }

    /**
     * 测试异步函数定义的解析性能
     */
    @RepeatedTest(TEST_ITERATIONS)
    @DisplayName("异步函数定义解析性能测试")
    public void testAsyncFunctionDefinitionPerformance() {
        String source = "async def loadUser(id) = await fetch(\"users/${id}\")";
        
        long executionTime = measureExecutionTime(() -> {
            List<ParseResult> results = parseSource(source);
            assertNotNull(results);
            assertTrue(results.size() > 0);
        });
        
        System.out.println("异步函数定义解析耗时: " + formatExecutionTime(executionTime));
    }

    /**
     * 测试 when 表达式的解析性能
     */
    @RepeatedTest(TEST_ITERATIONS)
    @DisplayName("When 表达式解析性能测试")
    public void testWhenExpressionPerformance() {
        String source = "def describe(num) = when { &num % 2 == 0 -> \"even\"; &num < 0 -> \"negative odd\"; else -> \"positive odd\" }";
        
        long executionTime = measureExecutionTime(() -> {
            List<ParseResult> results = parseSource(source);
            assertNotNull(results);
            assertTrue(results.size() > 0);
        });
        
        System.out.println("When 表达式解析耗时: " + formatExecutionTime(executionTime));
    }

    /**
     * 测试无括号函数调用的解析性能
     */
    @RepeatedTest(TEST_ITERATIONS)
    @DisplayName("无括号函数调用解析性能测试")
    public void testNoBracketFunctionCallPerformance() {
        String source = "print checkGrade 85";
        
        long executionTime = measureExecutionTime(() -> {
            List<ParseResult> results = parseSource(source);
            assertNotNull(results);
            assertTrue(results.size() > 0);
        });
        
        System.out.println("无括号函数调用解析耗时: " + formatExecutionTime(executionTime));
    }

    /**
     * 测试嵌套函数调用的解析性能
     */
    @RepeatedTest(TEST_ITERATIONS)
    @DisplayName("嵌套函数调用解析性能测试")
    public void testNestedFunctionCallPerformance() {
        String source = "player head to player hand";
        
        long executionTime = measureExecutionTime(() -> {
            List<ParseResult> results = parseSource(source);
            assertNotNull(results);
            assertTrue(results.size() > 0);
        });
        
        System.out.println("嵌套函数调用解析耗时: " + formatExecutionTime(executionTime));
    }

    /**
     * 测试大型代码的解析性能
     */
    @Test
    @DisplayName("大型代码解析性能测试")
    public void testLargeCodePerformance() {
        StringBuilder sourceBuilder = new StringBuilder();
        
        // 创建一个大型代码示例
        for (int i = 0; i < LARGE_CODE_REPETITIONS; i++) {
            sourceBuilder.append("def func").append(i).append("(n) = if &n <= 1 then 1 else &n * func").append(i > 0 ? i - 1 : i).append("(&n - 1)\n");
        }
        
        String source = sourceBuilder.toString();
        
        System.out.println("开始解析大型代码 (包含 " + LARGE_CODE_REPETITIONS + " 个函数定义)...");
        
        long executionTime = measureExecutionTime(() -> {
            List<ParseResult> results = parseSource(source);
            assertNotNull(results);
            assertTrue(results.size() > 0);
        });
        
        System.out.println("大型代码解析耗时: " + formatExecutionTime(executionTime));
        System.out.println("平均每个函数定义解析耗时: " + formatExecutionTime(executionTime / LARGE_CODE_REPETITIONS));
    }

    /**
     * 测试解析器在连续解析多个不同类型代码时的性能
     */
    @Test
    @DisplayName("混合代码解析性能测试")
    public void testMixedCodePerformance() {
        List<String> sources = new ArrayList<>();
        sources.add("def factorial(n) = if &n <= 1 then 1 else &n * factorial(&n - 1)");
        sources.add("async def loadUser(id) = await fetch(\"users/${id}\")");
        sources.add("def describe(num) = when { &num % 2 == 0 -> \"even\"; &num < 0 -> \"negative odd\"; else -> \"positive odd\" }");
        sources.add("print checkGrade 85");
        sources.add("player head to player hand");

        System.out.println("开始混合代码解析性能测试...");
        
        long totalExecutionTime = 0;
        
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            for (String source : sources) {
                long executionTime = measureExecutionTime(() -> {
                    List<ParseResult> results = parseSource(source);
                    assertNotNull(results);
                    assertTrue(results.size() > 0);
                });
                
                totalExecutionTime += executionTime;
            }
        }
        
        System.out.println("混合代码解析总耗时: " + formatExecutionTime(totalExecutionTime));
        System.out.println("平均每次解析耗时: " + formatExecutionTime(totalExecutionTime / (TEST_ITERATIONS * sources.size())));
    }
}