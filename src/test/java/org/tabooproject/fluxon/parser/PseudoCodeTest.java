package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 伪代码生成测试
 */
public class PseudoCodeTest {

    // 测试函数定义
    @Test
    public void testDef() {
        // @formatter:off
        testPseudoCode(
                """
                def factorial(n) = {
                    if &n <= 1 then 1 else &n * factorial &n - 1
                }
                a = 10 && 1
                factorial(&a)
                """
        );
        // @formatter:on
    }

    @Test
    public void testAsyncDef() {
        // 测试异步函数定义
        // @formatter:off
        testPseudoCode(
                """
                async def loadUser(id) = await fetch "users/${id}"
                """
        );
        // @formatter:on
    }

    @Test
    public void testWhen() {
        // 测试 when 表达式
        // @formatter:off
        testPseudoCode(
                """
                def describe(num) = when {
                    &a -> "specific"
                    &num % 2 == 0 -> "even"
                    &num < 0 -> "negative odd"
                    else -> "positive odd"
                }
                """
        );
        // @formatter:on
    }

    @Test
    public void testCall() {
        // 测试无括号函数调用
        // 测试未加引号标识符自动转为字符串
        // @formatter:off
        testPseudoCode(
                """
                print checkGrade 85 - 1
                print head
                player head
                player head to player hand
                """
        );
        // @formatter:on
    }

    @Test
    public void testIf() {
        // 测试复杂表达式
        // @formatter:off
        testPseudoCode(
                """
                if if true then 1 else 0 then true else false
                """
        );
        // @formatter:on
    }

    @Test
    public void testComplex() {
        // 测试混合
        // @formatter:off
        testPseudoCode(
                """
                def factorial(n) = if &n <= 1 then 1 else &n * factorial &n - 1
                async def loadUser(id) = await fetch "users/${id}"
                def describe(num) = when {
                    &a -> "specific"
                    &num % 2 == 0 -> "even"
                    &num < 0 -> "negative odd"
                    else -> "positive odd"
                }
                print checkGrade 85 - 1
                print head
                player head
                player head to player hand
                if if true then 1 else 0 then true else false
                """
        );
        // @formatter:on
    }

    @Test
    public void testWhile() {
        // 测试 while
        // @formatter:off
        testPseudoCode(
                """
                i = 10
                while i > 0 {
                    print &i ?: 0
                }
                """
        );
        // @formatter:on
    }

    @Test
    public void testList() {
        // 测试 List 和 Map 字面量
        // @formatter:off
        testPseudoCode(
                """
                print 1..10
                print [1,2,3,4,5]
                print [a:1,b:2,c:3]
                """
        );
        // @formatter:on
    }

    private void testPseudoCode(String source) {
        System.out.println("[Parse]: ");
        System.out.println(source);
        try {
            AtomicReference<List<ParseResult>> results = new AtomicReference<>();
            double time = measureTimeMillis(() -> {
                // 创建编译上下文
                CompilationContext context = new CompilationContext(source);
                Lexer lexer = new Lexer();
                List<Token> tokens = lexer.process(context);
                context.setAttribute("tokens", tokens);
                Parser parser = new Parser();
                results.set(parser.process(context));
            });
            System.out.println(time + "ms");
            System.out.println("[Structure]:");
            for (ParseResult result : results.get()) {
                System.out.println(result);
            }
            System.out.println("[PseudoCode]:");
            for (ParseResult result : results.get()) {
                System.out.println(result.toPseudoCode(0));
            }
            System.out.println("----------------------------------");
        } catch (ParseException ex) {
            System.out.println("[PseudoCode]:");
            for (ParseResult result : ex.getResults()) {
                System.out.println(result.toPseudoCode(0));
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