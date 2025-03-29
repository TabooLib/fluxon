package org.tabooproject.fluxon;

import org.tabooproject.fluxon.compiler.FluxonCompiler;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Fluxon 语言的主入口类
 * 提供编译和执行 Fluxon 代码的 API
 */
public class Fluxon {
    
    /**
     * 编译 Fluxon 源代码
     * 
     * @param source Fluxon 源代码
     * @return 编译后的字节码
     */
    public static byte[] compile(String source) {
        FluxonCompiler compiler = new FluxonCompiler();
        return compiler.compile(source);
    }
    
    /**
     * 编译 Fluxon 源文件
     * 
     * @param file Fluxon 源文件
     * @return 编译后的字节码
     * @throws IOException 如果文件读取失败
     */
    public static byte[] compileFile(File file) throws IOException {
        String source = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return compile(source);
    }
    
    /**
     * 解析 Fluxon 源代码
     * 
     * @param source Fluxon 源代码
     * @return 解析结果列表
     */
    public static List<ParseResult> parse(String source) {
        CompilationContext context = new CompilationContext(source);
        
        // 词法分析
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.process(context);
        context.setAttribute("tokens", tokens);
        
        // 语法分析
        Parser parser = new Parser();
        return parser.process(context);
    }
    
    /**
     * 解释执行 Fluxon 源代码
     * 
     * @param source Fluxon 源代码
     * @return 执行结果
     */
    public static Object eval(String source) {
        // 使用解释器直接执行
        List<ParseResult> parseResults = parse(source);
        Interpreter interpreter = new Interpreter();
        return interpreter.execute(parseResults);
    }
    
    /**
     * 执行 Fluxon 源文件
     * 
     * @param file Fluxon 源文件
     * @return 执行结果
     * @throws IOException 如果文件读取失败
     */
    public static Object evalFile(File file) throws IOException {
        String source = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return eval(source);
    }
}