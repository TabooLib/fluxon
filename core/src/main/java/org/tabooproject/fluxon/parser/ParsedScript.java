package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.ReturnValue;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.List;
import java.util.function.Consumer;

/**
 * 解析后的脚本
 * 封装解析结果和相关元数据，提供执行和环境创建功能
 */
public class ParsedScript {

    private final List<ParseResult> results;
    private final int rootLocalVariableCount;
    private final CompilationContext context;

    public ParsedScript(List<ParseResult> results, int rootLocalVariableCount, CompilationContext context) {
        this.results = results;
        this.rootLocalVariableCount = rootLocalVariableCount;
        this.context = context;
    }

    /**
     * 获取解析结果列表
     */
    public List<ParseResult> getResults() {
        return results;
    }

    /**
     * 获取根层级局部变量数量（_ 前缀变量）
     */
    public int getRootLocalVariableCount() {
        return rootLocalVariableCount;
    }

    /**
     * 获取编译上下文
     */
    public CompilationContext getContext() {
        return context;
    }

    /**
     * 创建适配此脚本的新环境
     * 自动初始化根层级局部变量数组
     */
    public Environment newEnvironment() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        if (rootLocalVariableCount > 0) {
            env.initializeRootLocalVariables(rootLocalVariableCount);
        }
        return env;
    }

    /**
     * 使用新环境执行脚本
     */
    public Object eval() {
        return eval(newEnvironment());
    }

    /**
     * 使用新环境执行脚本
     */
    public Object eval(Consumer<Environment> consumer) {
        Environment environment = newEnvironment();
        consumer.accept(environment);
        return eval(environment);
    }

    /**
     * 使用指定环境执行脚本
     * 会自动初始化根层级局部变量
     */
    public Object eval(Environment env) {
        if (rootLocalVariableCount > 0) {
            env.initializeRootLocalVariables(rootLocalVariableCount);
        }
        Interpreter interpreter = new Interpreter(env);
        try {
            return interpreter.execute(results);
        } catch (ReturnValue ex) {
            return ex.getValue();
        }
    }

    /**
     * 使用指定解释器执行脚本
     * 允许自定义解释器配置（如 costLimit）
     */
    public Object eval(Interpreter interpreter) {
        Environment env = interpreter.getEnvironment();
        if (rootLocalVariableCount > 0) {
            env.initializeRootLocalVariables(rootLocalVariableCount);
        }
        try {
            return interpreter.execute(results);
        } catch (ReturnValue ex) {
            return ex.getValue();
        }
    }
}
