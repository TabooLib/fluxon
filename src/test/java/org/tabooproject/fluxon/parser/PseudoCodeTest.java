package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;

import java.util.List;

/**
 * 伪代码生成测试
 */
public class PseudoCodeTest {
    
    @Test
    public void testPseudoCodeGeneration() {
        // 测试函数定义
        testPseudoCode("def factorial(n) = if &n <= 1 then 1 else &n * factorial(&n - 1)");

        // 测试异步函数定义
        testPseudoCode("async def loadUser(id) = await fetch(\"users/${id}\")");
        
        // 测试 when 表达式
        testPseudoCode("def describe(num) = when { &num % 2 == 0 -> \"even\"; &num < 0 -> \"negative odd\"; else -> \"positive odd\" }");
        
        // 测试无括号函数调用
        testPseudoCode("print checkGrade 85");
        
        // 测试未加引号标识符自动转为字符串
        testPseudoCode("player head");
        
        // 测试复杂表达式
        testPseudoCode("if if true then 1 else 0 then \"true\" else \"false\"");
    }
    
    private void testPseudoCode(String source) {
        System.out.println("源代码: " + source);
        
        // 词法分析
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        
        // 语法分析
        Parser parser = new Parser(tokens);
        List<ParseResult> results = parser.parse();
        
        // 输出解析结果
        System.out.println("解析结果:");
        for (ParseResult result : results) {
            System.out.println(result);
        }
        
        // 输出伪代码
        System.out.println("伪代码:");
        for (ParseResult result : results) {
            System.out.println(result.toPseudoCode(0));
        }
        
        System.out.println("\n-----------------------------------\n");
    }
}