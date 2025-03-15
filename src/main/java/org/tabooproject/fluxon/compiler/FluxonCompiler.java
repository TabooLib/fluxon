package org.tabooproject.fluxon.compiler;

import org.tabooproject.fluxon.ast.statement.Program;
import org.tabooproject.fluxon.ir.IRGenerator;
import org.tabooproject.fluxon.ir.IRModule;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.optimization.Optimizer;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.semantic.SemanticAnalyzer;
import org.tabooproject.fluxon.semantic.SemanticError;

import java.util.List;

/**
 * Fluxon 编译器主类
 * 协调各个编译阶段的执行
 */
public class FluxonCompiler {
    
    /**
     * 编译 Fluxon 源代码
     * 
     * @param source Fluxon 源代码
     * @return 编译后的字节码
     */
    public byte[] compile(String source) {
        // 创建编译上下文
        CompilationContext context = new CompilationContext(source);
        
        // 执行编译流程
        // 1. 词法分析：将源代码转换为词法单元序列
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.process(context);
        context.setAttribute("tokens", tokens);
        
        // 2. 语法分析：将词法单元序列转换为 AST
        Parser parser = new Parser(tokens);
        Program ast = parser.process(context);
        context.setAttribute("ast", ast);
        
        // 3. 语义分析：进行符号解析和类型检查
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(context.isStrictMode());
        semanticAnalyzer.process(context);
        
        // 检查是否有语义错误
        List<SemanticError> semanticErrors = semanticAnalyzer.getErrors();
        
        if (!semanticErrors.isEmpty()) {
            // 如果有语义错误，不继续编译
            return new byte[0];
        }
        
        // 4. IR 生成：将 AST 转换为中间表示
        IRGenerator irGenerator = new IRGenerator();
        IRModule irModule = irGenerator.process(context);
        
        // 5. 优化：对 IR 进行优化
        Optimizer optimizer = new Optimizer();
        IRModule optimizedModule = optimizer.process(context);
        
        // 暂时返回空字节码，后续实现优化和代码生成
        return new byte[0];
    }
}