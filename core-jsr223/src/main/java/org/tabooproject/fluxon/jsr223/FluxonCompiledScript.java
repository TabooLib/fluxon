package org.tabooproject.fluxon.jsr223;

import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fluxon 预编译脚本
 * 封装编译后的字节码，支持多次执行
 */
public class FluxonCompiledScript extends CompiledScript {

    // 类名计数器，用于生成唯一类名
    private static final AtomicLong CLASS_COUNTER = new AtomicLong(0);

    private final FluxonScriptEngine engine;
    private final CompileResult compileResult;
    private final RuntimeScriptBase scriptInstance;  // 缓存实例，直接复用

    /**
     * 编译脚本
     *
     * @param engine 脚本引擎
     * @param script 源代码
     */
    FluxonCompiledScript(FluxonScriptEngine engine, String script) {
        this.engine = engine;
        FluxonClassLoader classLoader = new FluxonClassLoader(Thread.currentThread().getContextClassLoader());
        // 编译生成字节码
        String className = generateClassName();
        Environment compileEnv = BindingsHelper.createEnvironment(engine.getContext());
        this.compileResult = Fluxon.compile(script, className, compileEnv);
        // 加载类并创建实例（只做一次）
        Class<?> scriptClass = compileResult.defineClass(classLoader);
        this.scriptInstance = createInstance(scriptClass);
    }

    /**
     * 使用已有的编译结果创建 CompiledScript
     * 用于编译缓存场景
     *
     * @param engine        脚本引擎
     * @param compileResult 编译结果
     */
    FluxonCompiledScript(FluxonScriptEngine engine, CompileResult compileResult) {
        this.engine = engine;
        FluxonClassLoader classLoader = new FluxonClassLoader(Thread.currentThread().getContextClassLoader());
        this.compileResult = compileResult;
        Class<?> scriptClass = compileResult.defineClass(classLoader);
        this.scriptInstance = createInstance(scriptClass);
    }

    /**
     * 创建脚本实例
     */
    private RuntimeScriptBase createInstance(Class<?> scriptClass) {
        try {
            RuntimeScriptBase instance = (RuntimeScriptBase) scriptClass.getDeclaredConstructor().newInstance();
            instance.setCommandData(compileResult.getCommandDataArray());
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create script instance", e);
        }
    }

    @Override
    public Object eval(ScriptContext context) throws ScriptException {
        try {
            // 创建带有绑定变量的环境
            Environment env = BindingsHelper.createEnvironment(context);
            // 直接复用实例执行
            Object result = scriptInstance.eval(env);
            // 提取变量回上下文
            BindingsHelper.extractToContext(env, context);
            return result;
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    /**
     * 快速执行（跳过变量回写）
     * 适用于不需要获取脚本中定义的变量的场景
     *
     * @param context 脚本上下文
     * @return 执行结果
     * @throws ScriptException 执行异常
     */
    public Object evalFast(ScriptContext context) throws ScriptException {
        try {
            Environment env = BindingsHelper.createEnvironment(context);
            return scriptInstance.eval(env);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    /**
     * 使用引擎默认上下文快速执行
     */
    public Object evalFast() throws ScriptException {
        return evalFast(engine.getContext());
    }

    @Override
    public ScriptEngine getEngine() {
        return engine;
    }

    /**
     * 获取编译结果
     */
    public CompileResult getCompileResult() {
        return compileResult;
    }

    /**
     * 生成唯一类名
     */
    private static String generateClassName() {
        return "FluxonScript$" + CLASS_COUNTER.incrementAndGet();
    }
}
