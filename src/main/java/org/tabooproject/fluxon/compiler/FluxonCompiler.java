package org.tabooproject.fluxon.compiler;

import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.ast.statement.Program;
import org.tabooproject.fluxon.parser.TypeAwareParser;

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

        // 2. 语法分析：将词法单元序列转换为AST
        TypeAwareParser parser = new TypeAwareParser(tokens, context);
        Program program = parser.parse();
        context.setAttribute("ast", program);
        
        // 后续步骤：语义分析、优化和代码生成
        
        // 暂时返回空字节码，后续实现优化和代码生成
        return new byte[0];
    }
}