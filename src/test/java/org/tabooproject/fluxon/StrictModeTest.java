package org.tabooproject.fluxon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.semantic.SemanticAnalyzer;
import org.tabooproject.fluxon.semantic.SemanticError;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Strict 模式测试
 */
public class StrictModeTest {
    
    @Test
    @DisplayName("测试非 Strict 模式下的代码")
    void testNonStrictMode() {
        String source = """
            def greet(name) = "Hello, ${name}!"
            greet world
            """;
        
        CompilationContext context = new CompilationContext(source);
        context.setSourceName("test");
        SemanticAnalyzer analyzer = new SemanticAnalyzer(false);
        analyzer.process(context);
        
        List<SemanticError> errors = analyzer.getErrors();
        assertTrue(errors.isEmpty(), "非 Strict 模式下不应该有错误");
    }
    
    @Test
    @DisplayName("测试 Strict 模式下的代码")
    void testStrictMode() {
        String source = """
            #!strict
            
            def greet(name) = "Hello, ${name}!"
            greet world
            """;
        
        CompilationContext context = new CompilationContext(source);
        context.setSourceName("test");
        assertTrue(context.isStrictMode(), "应该识别为 Strict 模式");
        
        SemanticAnalyzer analyzer = new SemanticAnalyzer(context.isStrictMode());
        analyzer.process(context);
        
        List<SemanticError> errors = analyzer.getErrors();
        assertFalse(errors.isEmpty(), "Strict 模式下应该有错误");
        
        // 检查错误类型
        boolean hasParameterTypeError = errors.stream()
                .anyMatch(error -> error.getMessage().contains("Parameter 'name' must have an explicit type"));
        
        boolean hasImplicitStringError = errors.stream()
                .anyMatch(error -> error.getMessage().contains("Implicit string conversion is not allowed"));
        
        assertTrue(hasParameterTypeError, "应该有参数类型错误");
        assertTrue(hasImplicitStringError, "应该有隐式字符串转换错误");
    }
    
    @Test
    @DisplayName("测试 Strict 模式下的正确代码")
    void testValidStrictModeCode() {
        String source = """
            #!strict
            
            def greet(name: String): String = "Hello, ${name}!"
            greet("world")
            """;
        
        CompilationContext context = new CompilationContext(source);
        context.setSourceName("test");
        assertTrue(context.isStrictMode(), "应该识别为 Strict 模式");
        
        SemanticAnalyzer analyzer = new SemanticAnalyzer(context.isStrictMode());
        analyzer.process(context);
        
        List<SemanticError> errors = analyzer.getErrors();
        assertTrue(errors.isEmpty(), "正确的 Strict 模式代码不应该有错误");
    }
    
    @Test
    @DisplayName("测试 Strict 模式下的变量声明")
    void testStrictModeVariableDeclaration() {
        String source = """
            #!strict
            
            val x = 5
            val y: Int = 10
            """;
        
        CompilationContext context = new CompilationContext(source);
        context.setSourceName("test");
        assertTrue(context.isStrictMode(), "应该识别为 Strict 模式");
        
        SemanticAnalyzer analyzer = new SemanticAnalyzer(context.isStrictMode());
        analyzer.process(context);
        
        List<SemanticError> errors = analyzer.getErrors();
        assertFalse(errors.isEmpty(), "Strict 模式下未标注类型的变量应该报错");
        
        boolean hasVariableTypeError = errors.stream()
                .anyMatch(error -> error.getMessage().contains("Variable 'x' must have an explicit type"));
        
        assertTrue(hasVariableTypeError, "应该有变量类型错误");
    }
    
    @Test
    @DisplayName("测试 Strict 模式下的异步函数")
    void testStrictModeAsyncFunction() {
        String source = """
            #!strict
            
            async def fetchData(): String {
                await http.get("https://api.com/data")
            }
            """;
        
        CompilationContext context = new CompilationContext(source);
        context.setSourceName("test");
        assertTrue(context.isStrictMode(), "应该识别为 Strict 模式");
        
        SemanticAnalyzer analyzer = new SemanticAnalyzer(context.isStrictMode());
        analyzer.process(context);
        
        // 注意：这个测试可能会失败，因为我们还没有完全实现异步函数的语义分析
        // 这里主要是测试 Strict 模式下的异步函数语法是否被正确解析
    }
}