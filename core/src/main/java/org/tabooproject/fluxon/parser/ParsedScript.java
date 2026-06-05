package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.ParameterInfo;
import org.tabooproject.fluxon.compiler.analysis.ScriptAnalysis;
import org.tabooproject.fluxon.compiler.analysis.ScriptAnalysisKeys;
import org.tabooproject.fluxon.interpreter.Interpreter;


import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 解析后的脚本
 * 封装解析结果和相关元数据，提供执行和环境创建功能
 */
public class ParsedScript {

    private final List<ParseResult> results;
    private final int rootLocalVariableCount;
    private final CompilationContext context;
    private final ScriptAnalysis scriptAnalysis;

    public ParsedScript(List<ParseResult> results, int rootLocalVariableCount, CompilationContext context) {
        this(results, rootLocalVariableCount, context, context != null ? context.getScriptAnalysis() : null);
    }

    public ParsedScript(List<ParseResult> results, int rootLocalVariableCount, CompilationContext context, ScriptAnalysis scriptAnalysis) {
        this.results = results;
        this.rootLocalVariableCount = rootLocalVariableCount;
        this.context = context;
        this.scriptAnalysis = scriptAnalysis;
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
     * 脚本体内实际读取的根变量名（编译期收集）。
     *
     * @return 根变量名集合
     */
    public ScriptAnalysis getScriptAnalysis() {
        return scriptAnalysis;
    }

    public Set<String> getReferencedRootVariableNames() {
        if (scriptAnalysis != null) {
            return scriptAnalysis.getStringSet(ScriptAnalysisKeys.REFERENCED_ROOT_VARIABLES);
        }
        return context != null ? context.getReferencedRootVariableNames() : Collections.emptySet();
    }

    /**
     * 获取参数索引
     *
     * @param name 参数名
     * @return 参数索引，如果不存在则返回 -1
     */
    public int getParameterIndex(String name) {
        return context.getParameterIndex(name);
    }

    /**
     * 获取所有参数名与索引的映射
     */
    public Map<String, Integer> getParameterIndices() {
        Map<String, Integer> indices = new LinkedHashMap<>();
        for (Map.Entry<String, ParameterInfo> entry : context.getParameters().entrySet()) {
            indices.put(entry.getKey(), entry.getValue().getIndex());
        }
        return indices;
    }

    /**
     * 创建适配此脚本的新环境
     * 自动初始化根层级局部变量数组（包含参数和脚本临时变量）
     * 并设置参数信息，使 env.setParameter() 可用
     */
    public Environment newEnvironment() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        if (rootLocalVariableCount > 0) {
            env.initializeRootLocalVariables(rootLocalVariableCount);
        }
        if (!context.getParameters().isEmpty()) {
            env.setParameters(context.getParameters());
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
     * 示例: script.eval(env -> env.setParameter("x", 5.0))
     */
    public Object eval(Consumer<Environment> consumer) {
        Environment environment = newEnvironment();
        consumer.accept(environment);
        return eval(environment);
    }

    /**
     * 使用指定环境执行脚本
     * 线程安全：每次执行创建独立的 Interpreter 结果数组
     */
    public Object eval(Environment env) {
        if (rootLocalVariableCount > 0) {
            env.initializeRootLocalVariables(rootLocalVariableCount);
        }
        Interpreter interpreter = new Interpreter(env);
        Map<Integer, Type> varTypes = context.getAttribute("variableTypes");
        if (varTypes != null) {
            interpreter.setVariableTypes(varTypes);
        }
        Map<String, Type> rootTypes = context.getRootVariableTypes();
        if (!rootTypes.isEmpty()) {
            interpreter.setRootVariableTypes(rootTypes);
        }
        try {
            Object result = interpreter.execute(results);
            if (interpreter.hasReturn) {
                Object rv = interpreter.returnValue;
                interpreter.hasReturn = false;
                interpreter.returnValue = null;
                return rv;
            }
            return result;
        } finally {
            interpreter.getPool().clearIdleContexts();
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
        Object result = interpreter.execute(results);
        if (interpreter.hasReturn) {
            Object rv = interpreter.returnValue;
            interpreter.hasReturn = false;
            interpreter.returnValue = null;
            return rv;
        }
        return result;
    }
}
