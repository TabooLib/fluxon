package org.tabooproject.fluxon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.ast.expression.ListExpr;
import org.tabooproject.fluxon.ast.expression.MapExpr;
import org.tabooproject.fluxon.ast.expression.RangeExpr;
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
 * 集合与字面量测试
 */
public class CollectionTest {
    
    @Test
    @DisplayName("测试列表字面量")
    void testListLiteral() {
        String source = """
            def test() {
                val numbers = [1, 2, 3, 4, 5]
                return numbers
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
    @DisplayName("测试字典字面量")
    void testMapLiteral() {
        String source = """
            def test() {
                val user = { name: "Bob", age: 25 }
                return user
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
    @DisplayName("测试范围表达式")
    void testRangeExpression() {
        String source = """
            def test() {
                val range = 1..10
                return range
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
    @DisplayName("测试列表操作")
    void testListOperations() {
        String source = """
            def test() {
                val numbers = [1, 2, 3, 4, 5]
                val first = numbers[0]
                val last = numbers[numbers.size() - 1]
                return first + last
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
    @DisplayName("测试字典操作")
    void testMapOperations() {
        String source = """
            def test() {
                val user = { name: "Bob", age: 25 }
                val name = user["name"]
                val age = user["age"]
                return name + " is " + age + " years old"
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