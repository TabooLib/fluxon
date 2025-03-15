package org.tabooproject.fluxon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
 * 语义分析器测试
 */
public class SemanticAnalyzerTest {
    
    private CompilationContext context;
    private Lexer lexer;
    private Parser parser;
    private SemanticAnalyzer semanticAnalyzer;
    
    @BeforeEach
    void setUp() {
        semanticAnalyzer = new SemanticAnalyzer();
    }
    
    @Test
    @DisplayName("测试变量声明和使用")
    void testVariableDeclarationAndUsage() {
        String source = """
            val x = 5
            val y = &x + 3
            """;
        
        List<SemanticError> errors = analyzeSource(source);
        
        // 不应该有错误
        assertTrue(errors.isEmpty(), "应该没有语义错误");
    }
    
    @Test
    @DisplayName("测试变量重复声明")
    void testDuplicateVariableDeclaration() {
        String source = """
            val x = 5
            val x = 10
            """;
        
        List<SemanticError> errors = analyzeSource(source);
        
        // 应该有一个错误：变量重复声明
        assertEquals(1, errors.size(), "应该有一个语义错误");
        assertTrue(errors.get(0).getMessage().contains("already defined"), 
                "错误消息应该包含 'already defined'");
    }
    
    @Test
    @DisplayName("测试未定义变量的使用")
    void testUndefinedVariableUsage() {
        String source = """
            val y = &x + 3
            """;
        
        List<SemanticError> errors = analyzeSource(source);
        
        // 应该有一个错误：未定义变量
        assertEquals(1, errors.size(), "应该有一个语义错误");
        assertTrue(errors.get(0).getMessage().contains("not defined"), 
                "错误消息应该包含 'not defined'");
    }
    
    @Test
    @DisplayName("测试类型不匹配")
    void testTypeMismatch() {
        String source = """
            val x = 5
            val y = &x + true
            """;
        
        List<SemanticError> errors = analyzeSource(source);
        
        // 应该有一个错误：类型不匹配
        assertEquals(1, errors.size(), "应该有一个语义错误");
        assertTrue(errors.get(0).getMessage().contains("cannot be applied"), 
                "错误消息应该包含 'cannot be applied'");
    }
    
    @Test
    @DisplayName("测试函数声明和调用")
    void testFunctionDeclarationAndCall() {
        String source = """
            def add(a, b) = &a + &b
            val result = add(5, 3)
            """;
        
        List<SemanticError> errors = analyzeSource(source);
        
        // 不应该有错误
        assertTrue(errors.isEmpty(), "应该没有语义错误");
    }
    
    @Test
    @DisplayName("测试函数参数数量不匹配")
    void testFunctionArgumentCountMismatch() {
        String source = """
            def add(a, b) = &a + &b
            val result = add(5)
            """;
        
        List<SemanticError> errors = analyzeSource(source);
        
        // 应该有一个错误：参数数量不匹配
        assertEquals(1, errors.size(), "应该有一个语义错误");
        assertTrue(errors.get(0).getMessage().contains("expects"), 
                "错误消息应该包含 'expects'");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        // 正确的代码
        """
        val x = 5
        val y = 10
        val z = &x + &y
        """,
        
        // 带类型注解的变量声明
        """
        val x: Int = 5
        val y: Int = 10
        val z = &x + &y
        """,
        
        // 带返回类型的函数声明
        """
        def add(a: Int, b: Int): Int = &a + &b
        val result = add(5, 3)
        """
    })
    @DisplayName("测试有效的代码")
    void testValidCode(String source) {
        List<SemanticError> errors = analyzeSource(source);
        
        // 不应该有错误
        assertTrue(errors.isEmpty(), "应该没有语义错误");
    }
    
    /**
     * 分析源代码并返回语义错误
     * 
     * @param source 源代码
     * @return 语义错误列表
     */
    private List<SemanticError> analyzeSource(String source) {
        // 创建编译上下文
        context = new CompilationContext(source);
        
        // 词法分析
        lexer = new Lexer(source);
        List<Token> tokens = lexer.process(context);
        context.setAttribute("tokens", tokens);
        
        // 语法分析
        parser = new Parser(tokens);
        Program ast = parser.process(context);
        context.setAttribute("ast", ast);
        
        // 语义分析
        semanticAnalyzer.process(context);
        
        return semanticAnalyzer.getErrors();
    }
}