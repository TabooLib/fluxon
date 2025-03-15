package org.tabooproject.fluxon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.ast.expression.ElvisExpr;
import org.tabooproject.fluxon.ast.expression.SafeAccessExpr;
import org.tabooproject.fluxon.ast.statement.Program;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.ir.IRModule;
import org.tabooproject.fluxon.ir.IRGenerator;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.semantic.SemanticAnalyzer;
import org.tabooproject.fluxon.semantic.SemanticError;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 空安全和 Elvis 操作符测试
 */
public class NullSafetyTest {
    
    @Test
    @DisplayName("测试空安全访问")
    void testSafeAccess() {
        String source = """
            def test(user) {
                val name = user?.name
                return name
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
        
        // 语义分析
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.process(context);
        
        List<SemanticError> errors = analyzer.getErrors();
        assertTrue(errors.isEmpty(), "不应该有语义错误");
        
        // IR 生成
        IRGenerator irGenerator = new IRGenerator();
        IRModule module = irGenerator.process(context);
        
        // 检查 IR 模块
        assertNotNull(module, "IR 模块不应为空");
        assertEquals(1, module.getFunctions().size(), "应该有一个函数");
        assertNotNull(module.getFunction("test"), "应该有名为 test 的函数");
    }
    
    @Test
    @DisplayName("测试 Elvis 操作符")
    void testElvisOperator() {
        String source = """
            def test(name) {
                val displayName = name ?: "Guest"
                return displayName
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
        
        // 语义分析
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.process(context);
        
        List<SemanticError> errors = analyzer.getErrors();
        assertTrue(errors.isEmpty(), "不应该有语义错误");
        
        // IR 生成
        IRGenerator irGenerator = new IRGenerator();
        IRModule module = irGenerator.process(context);
        
        // 检查 IR 模块
        assertNotNull(module, "IR 模块不应为空");
        assertEquals(1, module.getFunctions().size(), "应该有一个函数");
        assertNotNull(module.getFunction("test"), "应该有名为 test 的函数");
    }
    
    @Test
    @DisplayName("测试链式空安全访问")
    void testChainedSafeAccess() {
        String source = """
            def test(user) {
                val city = user?.address?.city
                return city
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
        
        // 语义分析
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.process(context);
        
        List<SemanticError> errors = analyzer.getErrors();
        assertTrue(errors.isEmpty(), "不应该有语义错误");
        
        // IR 生成
        IRGenerator irGenerator = new IRGenerator();
        IRModule module = irGenerator.process(context);
        
        // 检查 IR 模块
        assertNotNull(module, "IR 模块不应为空");
        assertEquals(1, module.getFunctions().size(), "应该有一个函数");
        assertNotNull(module.getFunction("test"), "应该有名为 test 的函数");
    }
    
    @Test
    @DisplayName("测试空安全访问和 Elvis 操作符组合")
    void testSafeAccessWithElvis() {
        String source = """
            def test(user) {
                val name = user?.name ?: "Unknown"
                return name
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
        
        // 语义分析
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.process(context);
        
        List<SemanticError> errors = analyzer.getErrors();
        assertTrue(errors.isEmpty(), "不应该有语义错误");
        
        // IR 生成
        IRGenerator irGenerator = new IRGenerator();
        IRModule module = irGenerator.process(context);
        
        // 检查 IR 模块
        assertNotNull(module, "IR 模块不应为空");
        assertEquals(1, module.getFunctions().size(), "应该有一个函数");
        assertNotNull(module.getFunction("test"), "应该有名为 test 的函数");
    }
}