package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonRuntimeTest;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.util.PseudoCodeFormatter;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 伪代码生成测试
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class PseudoCodeTest {

    @BeforeEach
    public void BeforeEach() {
        FluxonRuntimeTest.registerTestFunctions();
    }

    @Test
    public void testDef() {
        // 测试函数定义
        testPseudoCode("def factorial(n) = {\n" +
                "    if &n <= 1 then 1 else &n * factorial &n - 1\n" +
                "}\n" +
                "a = 10 && 1\n" +
                "factorial(&a)");
    }

    @Test
    public void testAsyncDef() {
        // 测试异步函数定义
        testPseudoCode("async def loadUser(id) = await fetch \"users/${id}\"");
    }

    @Test
    public void testWhen() {
        // 测试 when 表达式
        testPseudoCode("a = 0" +
                "def describe(num) = when {\n" +
                "    &a -> \"specific\"\n" +
                "    &num % 2 == 0 -> \"even\"\n" +
                "    &num < 0 -> \"negative odd\"\n" +
                "    else -> \"positive odd\"\n" +
                "}");
    }

    @Test
    public void testCall() {
        // 测试无括号函数调用
        // 测试未加引号标识符自动转为字符串
        testPseudoCode("print checkGrade 85 - 1\n" +
                "print head\n" +
                "player head\n" +
                "player head to player hand");
    }

    @Test
    public void testIf() {
        // 测试复杂表达式
        testPseudoCode("if if true then 1 else 0 then true else false");
    }

    @Test
    public void testComplex() {
        // 测试混合
        testPseudoCode("def factorial(n) = if &n <= 1 then 1 else &n * factorial &n - 1\n" +
                "async def loadUser(id) = await fetch \"users/${id}\"\n" +
                "a = 0\n" +
                "def describe(num) = when {\n" +
                "    &a -> \"specific\"\n" +
                "    &num % 2 == 0 -> \"even\"\n" +
                "    &num < 0 -> \"negative odd\"\n" +
                "    else -> \"positive odd\"\n" +
                "}\n" +
                "print checkGrade 85 - 1\n" +
                "print head\n" +
                "player head\n" +
                "player head to player hand\n" +
                "if if true then 1 else 0 then true else false");
    }

    @Test
    public void testWhile() {
        // 测试 while
        testPseudoCode("i = 10\n" +
                "while i > 0 {\n" +
                "    print &i ?: 0\n" +
                "}");
    }

    @Test
    public void testList() {
        // 测试 List 和 Map 字面量
        testPseudoCode("print(1..10)\n" +
                "print([1,2,3,4,5])\n" +
                "print([a:1,b:2,c:3])");
    }

    private void testPseudoCode(String source) {
        System.out.println("[Parse]: ");
        System.out.println(source);
        try {
            AtomicReference<List<ParseResult>> results = new AtomicReference<>();
            // 创建编译上下文
            CompilationContext context = new CompilationContext(source);
            Lexer lexer = new Lexer();
            List<Token> tokens = lexer.process(context);
            context.setAttribute("tokens", tokens);
            Parser parser = new Parser();
            results.set(parser.process(context));
            System.out.println("[Structure]:");
            for (ParseResult result : results.get()) {
                System.out.println(result);
            }
            System.out.println("[PseudoCode]:");
            for (ParseResult result : results.get()) {
                System.out.println(PseudoCodeFormatter.format(result.toPseudoCode()));
            }
            System.out.println("----------------------------------");
        } catch (ParseException ex) {
            System.out.println("[PseudoCode]:");
            for (ParseResult result : ex.getResults()) {
                System.out.println(PseudoCodeFormatter.format(result.toPseudoCode()));
            }
            throw ex;
        }
    }

    /**
     * 测量函数执行时间
     *
     * @param runnable 要执行的函数
     * @return 执行时间（毫秒）
     */
    private double measureTimeMillis(Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        long end = System.nanoTime();
        return (end - start) / 1_000_000.0; // 纳秒转毫秒
    }
}