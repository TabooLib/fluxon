package org.tabooproject.fluxon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.ast.statement.AsyncFunctionDeclStmt;
import org.tabooproject.fluxon.ast.statement.AwaitStmt;
import org.tabooproject.fluxon.ast.statement.Program;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.semantic.SemanticAnalyzer;
import org.tabooproject.fluxon.semantic.SemanticError;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异步功能测试
 */
public class AsyncTest {
    
    @Test
    @DisplayName("测试异步函数声明")
    void testAsyncFunctionDeclaration() {
        String source = """
            async def fetchData() = {
                await http.get("https://api.com/data")
            }
            """;
        
        // 词法分析
        CompilationContext context = new CompilationContext(source);
        context.setSourceName("test");
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.process(context);
        context.setAttribute("tokens", tokens);
        
        // 语法分析
        Parser parser = new Parser(tokens);
        Program ast = parser.process(context);
        context.setAttribute("ast", ast);
        
        // 检查 AST
        assertNotNull(ast, "AST 不应为空");
        assertEquals(1, ast.getStatements().size(), "应该有一个语句");
        assertTrue(ast.getStatements().get(0) instanceof AsyncFunctionDeclStmt, "应该是异步函数声明");
        
        AsyncFunctionDeclStmt asyncFunc = (AsyncFunctionDeclStmt) ast.getStatements().get(0);
        assertEquals("fetchData", asyncFunc.getName(), "函数名应该是 fetchData");
        assertEquals(0, asyncFunc.getParameters().size(), "应该没有参数");
        
        // 语义分析
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.process(context);
        
        List<SemanticError> errors = analyzer.getErrors();
        assertTrue(errors.isEmpty(), "不应该有语义错误");
    }
    
    @Test
    @DisplayName("测试 await 语句")
    void testAwaitStatement() {
        String source = """
            async def fetchData() = {
                val response = await http.get("https://api.com/data")
                return response
            }
            """;
        
        // 词法分析
        CompilationContext context = new CompilationContext(source);
        context.setSourceName("test");
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.process(context);
        context.setAttribute("tokens", tokens);
        
        // 语法分析
        Parser parser = new Parser(tokens);
        Program ast = parser.process(context);
        context.setAttribute("ast", ast);
        
        // 检查 AST
        assertNotNull(ast, "AST 不应为空");
        assertEquals(1, ast.getStatements().size(), "应该有一个语句");
        assertTrue(ast.getStatements().get(0) instanceof AsyncFunctionDeclStmt, "应该是异步函数声明");
        
        // 语义分析
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.process(context);
        
        List<SemanticError> errors = analyzer.getErrors();
        assertTrue(errors.isEmpty(), "不应该有语义错误");
    }
    
    @Test
    @DisplayName("测试异步函数类型检查")
    void testAsyncFunctionTypeChecking() {
        String source = """
            async def fetchData(): String = {
                val response = await http.get("https://api.com/data")
                return response
            }
            """;
        
        // 词法分析
        CompilationContext context = new CompilationContext(source);
        context.setSourceName("test");
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.process(context);
        context.setAttribute("tokens", tokens);
        
        // 语法分析
        Parser parser = new Parser(tokens);
        Program ast = parser.process(context);
        context.setAttribute("ast", ast);
        
        // 检查 AST
        assertNotNull(ast, "AST 不应为空");
        assertEquals(1, ast.getStatements().size(), "应该有一个语句");
        assertTrue(ast.getStatements().get(0) instanceof AsyncFunctionDeclStmt, "应该是异步函数声明");
        
        AsyncFunctionDeclStmt asyncFunc = (AsyncFunctionDeclStmt) ast.getStatements().get(0);
        assertEquals("String", asyncFunc.getReturnType(), "返回类型应该是 String");
        
        // 语义分析
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.process(context);
        
        List<SemanticError> errors = analyzer.getErrors();
        assertTrue(errors.isEmpty(), "不应该有语义错误");
    }
    
    @Test
    @DisplayName("测试非异步函数中的 await 语句")
    void testAwaitInNonAsyncFunction() {
        String source = """
            def fetchData() = {
                val response = await http.get("https://api.com/data")
                return response
            }
            """;
        
        // 词法分析
        CompilationContext context = new CompilationContext(source);
        context.setSourceName("test");
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.process(context);
        context.setAttribute("tokens", tokens);
        
        // 语法分析
        Parser parser = new Parser(tokens);
        Program ast = parser.process(context);
        context.setAttribute("ast", ast);
        
        // 语义分析
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.process(context);
        
        List<SemanticError> errors = analyzer.getErrors();
        assertFalse(errors.isEmpty(), "应该有语义错误");
        
        boolean hasAwaitError = errors.stream()
                .anyMatch(error -> error.getMessage().contains("await"));
        
        assertTrue(hasAwaitError, "应该有关于 await 的错误");
    }
}