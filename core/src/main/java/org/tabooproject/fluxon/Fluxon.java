package org.tabooproject.fluxon;

import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.ReturnValue;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeGenerator;
import org.tabooproject.fluxon.interpreter.bytecode.DefaultBytecodeGenerator;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

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
     * 解析 Fluxon 源代码
     *
     * @param source Fluxon 源代码
     * @return 解析结果列表
     */
    public static List<ParseResult> parse(String source) {
        return parse(source, FluxonRuntime.getInstance().newEnvironment());
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
        parser.defineFunction(env.getFunctions());
        parser.defineVariables(env.getVariables());
        parser.defineExtensionFunction(env.getExtensionFunctions());
        return parser.process(context);
    }

    /**
     * 解释执行 Fluxon 源代码
     *
     * @param source Fluxon 源代码
     * @return 执行结果
     */
    public static Object eval(String source) {
        return eval(parse(source));
    }

    /**
     * 解释执行 Fluxon 源代码
     * 使用特定环境执行
     */
    public static Object eval(String source, Environment env) {
        return eval(parse(source, env), env);
    }

    /**
     * 解释执行 Fluxon 源代码
     *
     * @param parseResults Fluxon 解析结果
     * @return 执行结果
     */
    public static Object eval(List<ParseResult> parseResults) {
        Interpreter interpreter = new Interpreter();
        try {
            return interpreter.execute(parseResults);
        } catch (ReturnValue ex) {
            return ex.getValue();
        }
    }

    /**
     * 解释执行 Fluxon 源代码
     * 使用特定环境执行
     *
     * @param parseResults Fluxon 解析结果
     * @return 执行结果
     */
    public static Object eval(List<ParseResult> parseResults, Environment env) {
        Interpreter interpreter = new Interpreter(env);
        try {
            return interpreter.execute(parseResults);
        } catch (ReturnValue ex) {
            return ex.getValue();
        }
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
     * @param source    Fluxon 源代码
     * @param className 类名
     * @return 编译结果
     */
    public static CompileResult compile(String source, String className) {
        return compile(source, className, FluxonRuntime.getInstance().newEnvironment());
    }

    /**
     * 将 Fluxon 源代码编译为字节码
     *
     * @param source    Fluxon 源代码
     * @param className 类名
     * @param env       环境对象
     * @return 编译结果
     */
    public static CompileResult compile(String source, String className, Environment env) {
        return compile(source, className, env, Fluxon.class.getClassLoader());
    }

    /**
     * 将 Fluxon 源代码编译为字节码
     *
     * @param source      Fluxon 源代码
     * @param className   类名
     * @param env         环境对象
     * @param classLoader 类加载器
     * @return 编译结果
     */
    public static CompileResult compile(String source, String className, Environment env, ClassLoader classLoader) {
        // 创建字节码生成器
        BytecodeGenerator generator = new DefaultBytecodeGenerator();
        // 解析源代码
        for (ParseResult result : parse(source, env)) {
            if (result instanceof Statement) {
                generator.addScriptBody((Statement) result);
            } else if (result instanceof Definition) {
                generator.addScriptDefinition((Definition) result);
            }
        }
        // 生成字节码
        List<byte[]> bytecode = generator.generateClassBytecode(className, classLoader);
        return new CompileResult(source, className, generator, bytecode);
    }
}