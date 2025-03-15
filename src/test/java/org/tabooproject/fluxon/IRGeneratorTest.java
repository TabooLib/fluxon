package org.tabooproject.fluxon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.ast.statement.Program;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.ir.IRGenerator;
import org.tabooproject.fluxon.ir.IRModule;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.semantic.SemanticAnalyzer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IR 生成器测试
 */
public class IRGeneratorTest {
    
    private CompilationContext context;
    private Lexer lexer;
    private Parser parser;
    private SemanticAnalyzer semanticAnalyzer;
    private IRGenerator irGenerator;
    
    @BeforeEach
    void setUp() {
        semanticAnalyzer = new SemanticAnalyzer();
        irGenerator = new IRGenerator();
    }
    
    @Test
    @DisplayName("测试简单函数生成")
    void testSimpleFunctionGeneration() {
        String source = """
            def add(a: Int, b: Int): Int = &a + &b
            """;
        
        IRModule module = generateIR(source);
        
        // 验证模块
        assertNotNull(module, "IR 模块不应为空");
        assertEquals("test", module.getName(), "模块名称应为 'test'");
        
        // 验证函数
        assertEquals(1, module.getFunctions().size(), "应该有一个函数");
        assertNotNull(module.getFunction("add"), "应该有名为 'add' 的函数");
    }
    
    @Test
    @DisplayName("测试变量声明和使用")
    void testVariableDeclarationAndUsage() {
        String source = """
            def test(): Int {
                val x = 5
                val y = &x + 3
                return &y
            }
            """;
        
        IRModule module = generateIR(source);
        
        // 验证模块
        assertNotNull(module, "IR 模块不应为空");
        
        // 验证函数
        assertEquals(1, module.getFunctions().size(), "应该有一个函数");
        assertNotNull(module.getFunction("test"), "应该有名为 'test' 的函数");
    }
    
    @Test
    @DisplayName("测试条件表达式")
    void testConditionalExpression() {
        String source = """
            def max(a: Int, b: Int): Int {
                if (&a > &b) {
                    return &a
                } else {
                    return &b
                }
            }
            """;
        
        IRModule module = generateIR(source);
        
        // 验证模块
        assertNotNull(module, "IR 模块不应为空");
        
        // 验证函数
        assertEquals(1, module.getFunctions().size(), "应该有一个函数");
        assertNotNull(module.getFunction("max"), "应该有名为 'max' 的函数");
    }
    
    @Test
    @DisplayName("测试函数调用")
    void testFunctionCall() {
        String source = """
            def add(a: Int, b: Int): Int = &a + &b
            def test(): Int {
                return add(5, 3)
            }
            """;
        
        IRModule module = generateIR(source);
        
        // 验证模块
        assertNotNull(module, "IR 模块不应为空");
        
        // 验证函数
        assertEquals(2, module.getFunctions().size(), "应该有两个函数");
        assertNotNull(module.getFunction("add"), "应该有名为 'add' 的函数");
        assertNotNull(module.getFunction("test"), "应该有名为 'test' 的函数");
    }
    
    @Test
    @DisplayName("测试全局变量")
    void testGlobalVariable() {
        String source = """
            val PI = 3.14159
            def calculateArea(radius: Float): Float = &PI * &radius * &radius
            """;
        
        IRModule module = generateIR(source);
        
        // 验证模块
        assertNotNull(module, "IR 模块不应为空");
        
        // 验证全局变量
        assertNotNull(module.getGlobal("PI"), "应该有名为 'PI' 的全局变量");
        
        // 验证函数
        assertEquals(1, module.getFunctions().size(), "应该有一个函数");
        assertNotNull(module.getFunction("calculateArea"), "应该有名为 'calculateArea' 的函数");
    }
    
    /**
     * 生成 IR
     * 
     * @param source 源代码
     * @return IR 模块
     */
    private IRModule generateIR(String source) {
        // 创建编译上下文
        context = new CompilationContext(source);
        context.setSourceName("test");

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
        
        // IR 生成
        return irGenerator.process(context);
    }
}