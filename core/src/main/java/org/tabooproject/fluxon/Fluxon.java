package org.tabooproject.fluxon;

import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeGenerator;
import org.tabooproject.fluxon.interpreter.bytecode.DefaultBytecodeGenerator;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Fluxon 语言的主入口类
 * 提供编译和执行 Fluxon 代码的 API
 */
public class Fluxon {

    /**
     * 解析 Fluxon 源代码
     *
     * @param source Fluxon 源代码
     * @return 解析结果列表
     */
    public static List<ParseResult> parse(String source) {
        return parse(source, new Environment());
    }

    /**
     * 在特定环境中解析 Fluxon 源代码
     *
     * @param source Fluxon 源代码
     * @return 解析结果列表
     */
    public static List<ParseResult> parse(String source, Environment env) {
        CompilationContext context = new CompilationContext(source);
        // 词法分析
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.process(context);
        context.setAttribute("tokens", tokens);
        // 语法分析
        Parser parser = new Parser();
        // 传入上下文符号
        for (Map.Entry<String, Function> entry : env.getFunctions().entrySet()) {
            parser.defineFunction(entry.getKey(), SymbolFunction.of(entry.getValue()));
        }
        parser.defineVariables(env.getValues().keySet());
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
     * 解释执行 Fluxon 源代码
     * 使用特定环境执行
     */
    public static Object eval(String source, Environment env) {
        // 使用解释器直接执行
        List<ParseResult> parseResults = parse(source, env);
        Interpreter interpreter = new Interpreter(env);
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

    /**
     * 将 Fluxon 源代码编译为字节码
     *
     * @param source Fluxon 源代码
     * @param className 类名
     * @return 编译结果
     */
    public static CompileResult compile(String source, String className) {
        // 创建字节码生成器
        BytecodeGenerator generator = new DefaultBytecodeGenerator();
        // 解析源代码
        for (ParseResult result : parse(source)) {
            if (result instanceof Statement) {
                generator.addScriptBody((Statement) result);
            } else if (result instanceof Definition) {
                generator.addScriptDefinition((Definition) result);
            }
        }
        // 生成字节码
        List<byte[]> bytecode = generator.generateClassBytecode(className);
        return new CompileResult(source, className, generator, bytecode);
    }
}