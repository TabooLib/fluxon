package org.tabooproject.fluxon.parser;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Parser JMH 基准测试类
 * <p>
 * 运行方法：
 * 1. 在 IDE 中直接运行 main 方法
 * 2. 或者使用 Gradle 命令：./gradlew jmh
 * <p>
 * 注意：基准测试可能需要几分钟时间才能完成
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class ParserJmhBenchmark {

    private Lexer lexer;
    private Parser parser;

    // 测试用例
    private String simpleFunctionSource;
    private String complexFunctionSource;
    private String asyncFunctionSource;
    private String whenExpressionSource;
    private String noBracketFunctionCallSource;
    private String nestedFunctionCallSource;
    private String largeCodeSource;

    @Setup
    public void setup() {
        lexer = new Lexer();
        parser = new Parser();

        // 初始化测试用例
        simpleFunctionSource = "def factorial(n) = n";
        complexFunctionSource = "def factorial(n) = if &n <= 1 then 1 else &n * factorial(&n - 1)";
        asyncFunctionSource = "async def loadUser(id) = await fetch(\"users/${id}\")";
        whenExpressionSource = "def describe(num) = when { &num % 2 == 0 -> \"even\"; &num < 0 -> \"negative odd\"; else -> \"positive odd\" }";
        noBracketFunctionCallSource = "print checkGrade 85";
        nestedFunctionCallSource = "player head to player hand";

        // 创建大型代码示例
        StringBuilder largeCodeBuilder = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeCodeBuilder.append("def func").append(i).append("(n) = if &n <= 1 then 1 else &n * func").append(i > 0 ? i - 1 : i).append("(&n - 1)\n");
        }
        largeCodeSource = largeCodeBuilder.toString();
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

    @Benchmark
    public void simpleFunctionDefinition(Blackhole blackhole) {
        blackhole.consume(parseSource(simpleFunctionSource));
    }

    @Benchmark
    public void complexFunctionDefinition(Blackhole blackhole) {
        blackhole.consume(parseSource(complexFunctionSource));
    }

    @Benchmark
    public void asyncFunctionDefinition(Blackhole blackhole) {
        blackhole.consume(parseSource(asyncFunctionSource));
    }

    @Benchmark
    public void whenExpression(Blackhole blackhole) {
        blackhole.consume(parseSource(whenExpressionSource));
    }

    @Benchmark
    public void noBracketFunctionCall(Blackhole blackhole) {
        blackhole.consume(parseSource(noBracketFunctionCallSource));
    }

    @Benchmark
    public void nestedFunctionCall(Blackhole blackhole) {
        blackhole.consume(parseSource(nestedFunctionCallSource));
    }

    @Benchmark
    public void largeCode(Blackhole blackhole) {
        blackhole.consume(parseSource(largeCodeSource));
    }

    @Benchmark
    public void mixedCode(Blackhole blackhole) {
        List<String> sources = new ArrayList<>();
        sources.add(simpleFunctionSource);
        sources.add(complexFunctionSource);
        sources.add(asyncFunctionSource);
        sources.add(whenExpressionSource);
        sources.add(noBracketFunctionCallSource);
        sources.add(nestedFunctionCallSource);

        for (String source : sources) {
            blackhole.consume(parseSource(source));
        }
    }

    /**
     * 主方法，用于运行基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(ParserJmhBenchmark.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}