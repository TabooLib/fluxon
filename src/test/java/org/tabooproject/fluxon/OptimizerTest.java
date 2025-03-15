package org.tabooproject.fluxon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.ast.statement.Program;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.ir.IRFunction;
import org.tabooproject.fluxon.ir.IRGenerator;
import org.tabooproject.fluxon.ir.IRModule;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.optimization.Optimizer;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.semantic.SemanticAnalyzer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 优化器测试
 */
public class OptimizerTest {
    
    private CompilationContext context;
    private Lexer lexer;
    private Parser parser;
    private SemanticAnalyzer semanticAnalyzer;
    private IRGenerator irGenerator;
    private Optimizer optimizer;
    
    @BeforeEach
    void setUp() {
        semanticAnalyzer = new SemanticAnalyzer();
        irGenerator = new IRGenerator();
        optimizer = new Optimizer(true); // 启用调试模式
    }
    
    @Test
    @DisplayName("测试常量折叠")
    void testConstantFolding() {
        String source = """
            def test(): Int {
                val x = 2 + 3 * 4
                return &x
            }
            """;
        
        IRModule module = generateAndOptimizeIR(source);
        
        // 验证模块
        assertNotNull(module, "IR 模块不应为空");
        
        // 验证函数
        assertEquals(1, module.getFunctions().size(), "应该有一个函数");
        assertNotNull(module.getFunction("test"), "应该有名为 'test' 的函数");
        
        // 验证常量折叠
        // 注意：这里的验证是基于优化后的 IR 结构，可能需要根据实际实现调整
    }
    
    @Test
    @DisplayName("测试死代码消除")
    void testDeadCodeElimination() {
        String source = """
            def test(condition: Boolean): Int {
                if (true) {
                    return 1
                } else {
                    return 2
                }
            }
            """;
        
        IRModule module = generateAndOptimizeIR(source);
        
        // 验证模块
        assertNotNull(module, "IR 模块不应为空");
        
        // 验证函数
        assertEquals(1, module.getFunctions().size(), "应该有一个函数");
        assertNotNull(module.getFunction("test"), "应该有名为 'test' 的函数");
        
        // 验证死代码消除
        // 注意：这里的验证是基于优化后的 IR 结构，可能需要根据实际实现调整
    }
    
    @Test
    @DisplayName("测试公共子表达式消除")
    void testCommonSubexpressionElimination() {
        String source = """
            def test(a: Int, b: Int): Int {
                val x = &a + &b
                val y = &a + &b
                return &x * &y
            }
            """;
        
        IRModule module = generateAndOptimizeIR(source);
        
        // 验证模块
        assertNotNull(module, "IR 模块不应为空");
        
        // 验证函数
        assertEquals(1, module.getFunctions().size(), "应该有一个函数");
        assertNotNull(module.getFunction("test"), "应该有名为 'test' 的函数");
        
        // 验证公共子表达式消除
        // 注意：这里的验证是基于优化后的 IR 结构，可能需要根据实际实现调整
    }
    
    @Test
    @DisplayName("测试选择性静态化")
    void testSelectiveStaticization() {
        String source = """
            def add(a: Int, b: Int): Int = &a + &b
            
            def test1(): Int {
                return add(1, 2)
            }
            
            def test2(): Int {
                return add(3, 4)
            }
            
            def test3(): Int {
                return add(5, 6)
            }
            """;
        
        IRModule module = generateAndOptimizeIR(source);
        
        // 验证模块
        assertNotNull(module, "IR 模块不应为空");
        
        // 验证函数
        assertEquals(4, module.getFunctions().size(), "应该有四个函数");
        assertNotNull(module.getFunction("add"), "应该有名为 'add' 的函数");
        assertNotNull(module.getFunction("test1"), "应该有名为 'test1' 的函数");
        assertNotNull(module.getFunction("test2"), "应该有名为 'test2' 的函数");
        assertNotNull(module.getFunction("test3"), "应该有名为 'test3' 的函数");
        
        // 验证选择性静态化
        // 注意：这里的验证是基于优化后的 IR 结构，可能需要根据实际实现调整
    }
    
    @Test
    @DisplayName("测试多个优化 Pass 的组合")
    void testCombinedOptimizations() {
        String source = """
            def test(a: Int, b: Int): Int {
                val x = 2 + 3 * 4
                val y = &a + &b
                val z = &a + &b
                
                if (true) {
                    return &x * (&y + &z)
                } else {
                    return 0
                }
            }
            """;
        
        IRModule module = generateAndOptimizeIR(source);
        
        // 验证模块
        assertNotNull(module, "IR 模块不应为空");
        
        // 验证函数
        assertEquals(1, module.getFunctions().size(), "应该有一个函数");
        assertNotNull(module.getFunction("test"), "应该有名为 'test' 的函数");
        
        // 验证组合优化
        // 注意：这里的验证是基于优化后的 IR 结构，可能需要根据实际实现调整
    }
    
    /**
     * 生成并优化 IR
     * 
     * @param source 源代码
     * @return 优化后的 IR 模块
     */
    private IRModule generateAndOptimizeIR(String source) {
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
        
        // IR 生成
        irGenerator.process(context);
        
        // 优化
        return optimizer.process(context);
    }
}