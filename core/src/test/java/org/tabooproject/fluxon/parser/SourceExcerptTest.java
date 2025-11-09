package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SourceExcerpt 测试
 */
class SourceExcerptTest {

    @Test
    void testBasicExcerpt() {
        String source = "def foo(x) {\n" +
                        "    return x + 1\n" +
                        "}";
        
        CompilationContext context = new CompilationContext(source);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.process(context);
        
        // 获取第一个 token (def)
        Token token = tokens.get(0);
        
        SourceExcerpt excerpt = SourceExcerpt.from(context, token);
        assertNotNull(excerpt);
        assertEquals(1, excerpt.getLine());
        assertEquals(1, excerpt.getColumn()); // Lexer 的列号从 1 开始
        
        String rendered = excerpt.render();
        assertNotNull(rendered);
        assertTrue(rendered.contains("def foo(x) {"));
        assertTrue(rendered.contains("^~~")); // 指示符
    }

    @Test
    void testMultiLineContext() {
        String source = "def foo(x) {\n" +
                        "    return x + 1\n" +
                        "}\n" +
                        "def bar(y) {\n" +
                        "    return y * 2\n" +
                        "}";
        
        CompilationContext context = new CompilationContext(source);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.process(context);
        
        // 找到第二个函数的 def token (应该在第4行)
        Token token = null;
        for (Token t : tokens) {
            if (t.getLine() == 4 && t.getLexeme().equals("def")) {
                token = t;
                break;
            }
        }
        
        assertNotNull(token);
        
        SourceExcerpt excerpt = SourceExcerpt.from(context, token, 1);
        assertNotNull(excerpt);
        
        String rendered = excerpt.render();
        assertNotNull(rendered);
        // 应该包含上下文行
        assertTrue(rendered.contains("def bar(y) {"));
    }

    @Test
    void testNullTokenReturnsNull() {
        String source = "def foo(x) { return x }";
        CompilationContext context = new CompilationContext(source);
        
        SourceExcerpt excerpt = SourceExcerpt.from(context, null);
        assertNull(excerpt);
    }

    @Test
    void testTokenWithoutLineInfoReturnsNull() {
        String source = "def foo(x) { return x }";
        CompilationContext context = new CompilationContext(source);
        
        // 创建一个没有行号的 token
        Token token = new Token(org.tabooproject.fluxon.lexer.TokenType.IDENTIFIER, "foo");
        
        SourceExcerpt excerpt = SourceExcerpt.from(context, token);
        assertNull(excerpt);
    }

    @Test
    void testFormatDiagnostic() {
        String source = "def foo(x) {\n" +
                        "    return x + 1\n" +
                        "}";
        
        CompilationContext context = new CompilationContext(source);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.process(context);
        
        Token token = tokens.get(0);
        
        SourceExcerpt excerpt = SourceExcerpt.from(context, token);
        assertNotNull(excerpt);
        
        String diagnostic = excerpt.formatDiagnostic("E0001", "Test error message");
        assertNotNull(diagnostic);
        assertTrue(diagnostic.contains("error[E0001]: Test error message"));
        assertTrue(diagnostic.contains("-->"));
        assertTrue(diagnostic.contains("1:1")); // 列号从 1 开始
        assertTrue(diagnostic.contains("def foo(x) {"));
        assertTrue(diagnostic.contains("^~~"));
    }

    @Test
    void testTabExpansion() {
        // 测试制表符展开
        String source = "def\tfoo(x) {\n" +
                        "\treturn x + 1\n" +
                        "}";
        
        CompilationContext context = new CompilationContext(source);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.process(context);
        
        Token token = tokens.get(0); // def
        
        SourceExcerpt excerpt = SourceExcerpt.from(context, token);
        assertNotNull(excerpt);
        
        String rendered = excerpt.render();
        // 制表符应该被展开为空格
        assertFalse(rendered.contains("\t"));
    }

    @Test
    void testParseExceptionWithExcerpt() {
        String source = "def foo(x) {\n" +
                        "    return x +\n" +  // 错误：表达式不完整
                        "}";
        
        CompilationContext context = new CompilationContext(source);
        
        try {
            // 先进行词法分析
            Lexer lexer = new Lexer();
            List<Token> tokens = lexer.process(context);
            context.setAttribute("tokens", tokens);
            
            // 再进行语法分析
            Parser parser = new Parser();
            parser.process(context);
            fail("Should have thrown ParseException");
        } catch (ParseException ex) {
            // 检查异常是否包含源码摘录
            SourceExcerpt excerpt = ex.getExcerpt();
            if (excerpt != null) {
                String formatted = ex.formatWithSource();
                assertNotNull(formatted);
                assertTrue(formatted.contains("error:"));
                assertTrue(formatted.contains("-->"));
            }
        }
    }

    @Test
    void testMultipleErrorsWithExcerpts() {
        String source = "def foo(x {\n" +          // 错误1: 缺少右括号
                        "    return x + 1\n" +
                        "}\n" +
                        "def bar(y) {\n" +
                        "    return y *\n" +        // 错误2: 表达式不完整
                        "}";
        
        CompilationContext context = new CompilationContext(source);
        
        try {
            // 先进行词法分析
            Lexer lexer = new Lexer();
            List<Token> tokens = lexer.process(context);
            context.setAttribute("tokens", tokens);
            
            // 再进行语法分析
            Parser parser = new Parser();
            parser.process(context);
            fail("Should have thrown MultipleParseException");
        } catch (MultipleParseException ex) {
            // 验证多错误格式化
            String diagnostics = ex.formatDiagnostics();
            System.out.println(diagnostics);
            assertNotNull(diagnostics);
            assertTrue(diagnostics.contains("Found 2 parse error(s)"));
            
            // 检查每个错误都有源码摘录（如果可用）
            for (ParseException error : ex.getExceptions()) {
                assertNotNull(error.getToken());
            }
        } catch (ParseException ex) {
            // 单个错误也可接受
            assertNotNull(ex.getExcerpt());
        }
    }

    @Test
    void testColumnPositionAccuracy() {
        String source = "def foo(x) { return x + 1 }";
        
        CompilationContext context = new CompilationContext(source);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.process(context);
        
        // 找到 'return' token
        Token returnToken = null;
        for (Token t : tokens) {
            if (t.getLexeme().equals("return")) {
                returnToken = t;
                break;
            }
        }
        
        assertNotNull(returnToken);
        
        SourceExcerpt excerpt = SourceExcerpt.from(context, returnToken);
        assertNotNull(excerpt);
        
        String rendered = excerpt.render();
        // 指示符应该对齐到 return 的位置
        assertTrue(rendered.contains("^~~~~~")); // return 是 6 个字符
    }
}
