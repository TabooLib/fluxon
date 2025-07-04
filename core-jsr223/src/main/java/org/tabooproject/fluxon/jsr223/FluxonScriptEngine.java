package org.tabooproject.fluxon.jsr223;

import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.runtime.Function;

import javax.script.*;
import java.io.*;
import java.util.Map;

/**
 * Fluxon 脚本引擎
 * 实现 JSR-223 接口
 */
public class FluxonScriptEngine implements ScriptEngine {
    
    private final FluxonScriptEngineFactory factory;
    private final Bindings globalScope;
    private Bindings engineScope;
    private ScriptContext context;
    
    /**
     * 构造函数
     * 
     * @param factory 脚本引擎工厂
     */
    public FluxonScriptEngine(FluxonScriptEngineFactory factory) {
        this.factory = factory;
        this.globalScope = new FluxonBindings();
        this.engineScope = new FluxonBindings();
        this.context = new SimpleScriptContext();
        this.context.setBindings(engineScope, ScriptContext.ENGINE_SCOPE);
        this.context.setBindings(globalScope, ScriptContext.GLOBAL_SCOPE);
    }
    
    @Override
    public Object eval(String script) throws ScriptException {
        return eval(script, context);
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        return eval(readFully(reader), context);
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        try {
            // 将上下文中的变量注入到 Fluxon 环境
            Interpreter interpreter = createInterpreterWithBindings(context);
            // 解析并执行脚本
            Object result = interpreter.execute(Fluxon.parse(script, interpreter.getEnvironment()));
            // 从 Fluxon 环境中提取变量回到上下文
            extractVariablesFromEnvironment(interpreter.getEnvironment(), context);
            return result;
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        return eval(readFully(reader), context);
    }

    @Override
    public Object eval(String script, Bindings bindings) throws ScriptException {
        ScriptContext ctx = new SimpleScriptContext();
        ctx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        ctx.setBindings(globalScope, ScriptContext.GLOBAL_SCOPE);
        return eval(script, ctx);
    }

    @Override
    public Object eval(Reader reader, Bindings bindings) throws ScriptException {
        return eval(readFully(reader), bindings);
    }

    @Override
    public void put(String key, Object value) {
        getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
    }

    @Override
    public Object get(String key) {
        return getBindings(ScriptContext.ENGINE_SCOPE).get(key);
    }

    @Override
    public Bindings getBindings(int scope) {
        return context.getBindings(scope);
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        context.setBindings(bindings, scope);
        if (scope == ScriptContext.ENGINE_SCOPE) {
            this.engineScope = bindings;
        }
    }

    @Override
    public Bindings createBindings() {
        return new FluxonBindings();
    }

    @Override
    public ScriptContext getContext() {
        return context;
    }

    @Override
    public void setContext(ScriptContext context) {
        this.context = context;
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }
    
    /**
     * 创建带有绑定变量的解释器
     * 
     * @param context 脚本上下文
     * @return 初始化的解释器
     */
    private Interpreter createInterpreterWithBindings(ScriptContext context) {
        Interpreter interpreter = new Interpreter();
        Environment env = interpreter.getEnvironment();
        // 注入 ENGINE_SCOPE 变量
        Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        if (engineBindings != null) {
            for (Map.Entry<String, Object> entry : engineBindings.entrySet()) {
                if (entry.getValue() instanceof Function) {
                    env.defineFunction(entry.getKey(), (Function) entry.getValue());
                } else {
                    env.defineVariable(entry.getKey(), entry.getValue());
                }
            }
        }
        // 注入 GLOBAL_SCOPE 变量
        Bindings globalBindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);
        if (globalBindings != null) {
            for (Map.Entry<String, Object> entry : globalBindings.entrySet()) {
                if (entry.getValue() instanceof Function) {
                    env.defineFunction(entry.getKey(), (Function) entry.getValue());
                } else {
                    env.defineVariable(entry.getKey(), entry.getValue());
                }
            }
        }
        return interpreter;
    }
    
    /**
     * 从 Fluxon 环境中提取变量到 ScriptContext
     * 
     * @param env Fluxon 环境
     * @param context 脚本上下文
     */
    private void extractVariablesFromEnvironment(Environment env, ScriptContext context) {
        // 更新 ENGINE_SCOPE 中的变量
        Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        if (engineBindings != null) {
            engineBindings.putAll(env.getVariables());
            engineBindings.putAll(env.getFunctions());
        }
    }
    
    /**
     * 完整读取 Reader 中的内容
     * 
     * @param reader 要读取的 Reader
     * @return 读取的字符串
     * @throws ScriptException 如果读取失败
     */
    private String readFully(Reader reader) throws ScriptException {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }
} 