package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;

import java.util.List;

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

    private void testPseudoCode(String source) {
        System.out.println("[Parse]: ");
        System.out.println(source);
        // 创建编译上下文
        CompilationContext context = new CompilationContext(source);
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        context.setAttribute("tokens", tokens);
        Parser parser = new Parser();
        try {
            List<ParseResult> results = parser.process(context);
            System.out.println("[Structure]:");
            for (ParseResult result : results) {
                System.out.println(result);
            }
            System.out.println("[PseudoCode]:");
            for (ParseResult result : results) {
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
}