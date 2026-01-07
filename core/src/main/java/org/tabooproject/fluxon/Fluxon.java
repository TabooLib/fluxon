package org.tabooproject.fluxon;

import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeGenerator;
import org.tabooproject.fluxon.interpreter.bytecode.DefaultBytecodeGenerator;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

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
     * @return 解析后的脚本对象
     */
    public static ParsedScript parse(String source) {
        return parse(source, FluxonRuntime.getInstance().newEnvironment());
    }

    /**
     * 解析 Fluxon 源代码
     *
     * @param source Fluxon 源代码
     * @param env    环境对象（用于获取预定义的变量和函数）
     * @return 解析后的脚本对象
     */
    public static ParsedScript parse(String source, Environment env) {
        return parse(new CompilationContext(source), env);
    }

    /**
     * 解析 Fluxon 源代码
     *
     * @param context 编译上下文
     * @return 解析后的脚本对象
     */
    public static ParsedScript parse(CompilationContext context) {
        return parse(context, FluxonRuntime.getInstance().newEnvironment());
    }

    /**
     * 解析 Fluxon 源代码
     *
     * @param context 编译上下文
     * @param env     环境对象（用于获取预定义的变量和函数）
     * @return 解析后的脚本对象
     */
    public static ParsedScript parse(CompilationContext context, Environment env) {
        List<ParseResult> results = doParse(env, context);
        Integer rootLocalVarCount = context.getAttribute("rootLocalVariableCount");
        return new ParsedScript(results, rootLocalVarCount != null ? rootLocalVarCount : 0, context);
    }

    /**
     * 内部解析方法
     */
    static List<ParseResult> doParse(Environment env, CompilationContext context) {
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.process(context);
        context.setAttribute("tokens", tokens);
        Parser parser = new Parser();
        parser.defineRootVariables(env.getRootVariables());
        parser.defineUserFunction(env.getUserFunctions());
        return parser.process(context);
    }

    /**
     * 解释执行 Fluxon 源代码
     *
     * @param source Fluxon 源代码
     * @return 执行结果
     */
    public static Object eval(String source) {
        return parse(source).eval();
    }

    /**
     * 解释执行 Fluxon 源代码
     * 使用特定环境执行
     */
    public static Object eval(String source, Environment env) {
        return parse(source, env).eval(env);
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
        return doCompile(env, new CompilationContext(source), className, classLoader);
    }

    /**
     * 使用指定的编译上下文编译 Fluxon 源代码
     *
     * @param env       环境对象
     * @param context   编译上下文
     * @param className 类名
     * @return 编译结果
     */
    public static CompileResult compile(Environment env, CompilationContext context, String className) {
        return doCompile(env, context, className, Fluxon.class.getClassLoader());
    }

    /**
     * 使用指定的编译上下文编译 Fluxon 源代码
     *
     * @param env         环境对象
     * @param context     编译上下文
     * @param className   类名
     * @param classLoader 类加载器
     * @return 编译结果
     */
    public static CompileResult compile(Environment env, CompilationContext context, String className, ClassLoader classLoader) {
        return doCompile(env, context, className, classLoader);
    }

    /**
     * 内部编译方法
     */
    private static CompileResult doCompile(Environment env, CompilationContext context, String className, ClassLoader classLoader) {
        BytecodeGenerator generator = new DefaultBytecodeGenerator();
        generator.setSourceContext(context.getSource(), context.getFileName());
        for (ParseResult result : doParse(env, context)) {
            if (result instanceof Statement) {
                generator.addScriptBody((Statement) result);
            } else if (result instanceof Definition) {
                generator.addScriptDefinition((Definition) result);
            }
        }
        Integer rootLocalVarCount = context.getAttribute("rootLocalVariableCount");
        if (rootLocalVarCount != null) {
            generator.setRootLocalVariableCount(rootLocalVarCount);
        }
        List<byte[]> bytecode = generator.generateClassBytecode(className, classLoader);
        return new CompileResult(context.getSource(), className, generator, bytecode);
    }
}
