package org.tabooproject.fluxon.jsr223;

import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.parser.SourceExcerpt;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.FunctionContextPool;
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError;

import javax.script.*;
import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fluxon 脚本引擎
 * 实现 JSR-223 接口，支持编译和函数调用
 */
public class FluxonScriptEngine implements ScriptEngine, Compilable, Invocable {

    // 编译缓存属性名
    public static final String COMPILE_CACHE_ENABLED = "fluxon.compile.cache";
    // 执行成本限制属性名
    public static final String COST_LIMIT = "fluxon.cost.limit";
    public static final String COST_PER_STEP = "fluxon.cost.perStep";

    private final FluxonScriptEngineFactory factory;
    private final Bindings globalScope;
    private Bindings engineScope;
    private ScriptContext context;

    // 编译缓存：源码 -> 编译结果（使用源码本身作为 key 避免哈希碰撞）
    private final ConcurrentHashMap<String, CompileResult> compileCache = new ConcurrentHashMap<>();
    // 线程本地的执行环境（用于 Invocable，保证线程安全）
    private final ThreadLocal<Environment> threadLocalEnvironment = new ThreadLocal<>();
    
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
            // 应用执行成本限制
            applyCostLimit(interpreter, context);
            // 解析并执行脚本
            Object result = interpreter.execute(Fluxon.parse(script, interpreter.getEnvironment()));
            // 保存环境供 Invocable 使用（线程隔离）
            this.threadLocalEnvironment.set(interpreter.getEnvironment());
            // 从 Fluxon 环境中提取变量回到上下文
            BindingsHelper.extractToContext(interpreter.getEnvironment(), context);
            return result;
        } catch (FluxonRuntimeError e) {
            throw toScriptException(e);
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
        Interpreter interpreter = new Interpreter(FluxonRuntime.getInstance().newEnvironment());
        Environment env = interpreter.getEnvironment();
        // 配置输出流重定向
        configureIOStreams(env, context);
        // 注入变量（GLOBAL_SCOPE 优先级低，ENGINE_SCOPE 优先级高）
        BindingsHelper.injectFromContext(env, context);
        return interpreter;
    }

    /**
     * 配置输出流重定向
     * 将 ScriptContext 的 Writer 转换为 Environment 可用的 PrintStream
     */
    private void configureIOStreams(Environment env, ScriptContext context) {
        // 标准输出重定向
        Writer writer = context.getWriter();
        if (writer != null) {
            env.setOut(new PrintStream(new WriterOutputStream(writer), true));
        }
        // 错误输出重定向
        Writer errorWriter = context.getErrorWriter();
        if (errorWriter != null) {
            env.setErr(new PrintStream(new WriterOutputStream(errorWriter), true));
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
        try {
            char[] buffer = new char[4096];
            StringBuilder sb = new StringBuilder();
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    // ==================== Compilable 接口实现 ====================

    @Override
    public CompiledScript compile(String script) throws ScriptException {
        try {
            // 检查是否启用编译缓存
            if (isCacheEnabled()) {
                CompileResult cached = compileCache.get(script);
                if (cached != null) {
                    return new FluxonCompiledScript(this, cached);
                }
                // 编译并缓存（使用脚本内容本身作为 key，避免哈希碰撞）
                FluxonCompiledScript compiled = new FluxonCompiledScript(this, script);
                compileCache.put(script, compiled.getCompileResult());
                return compiled;
            }
            return new FluxonCompiledScript(this, script);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public CompiledScript compile(Reader reader) throws ScriptException {
        return compile(readFully(reader));
    }

    /**
     * 检查是否启用编译缓存
     */
    private boolean isCacheEnabled() {
        Object value = get(COMPILE_CACHE_ENABLED);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    /**
     * 清空编译缓存
     */
    public void clearCompileCache() {
        compileCache.clear();
    }

    /**
     * 获取编译缓存大小
     */
    public int getCompileCacheSize() {
        return compileCache.size();
    }

    // ==================== Invocable 接口实现 ====================

    @Override
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        return invokeMethod(null, name, args);
    }

    @Override
    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
        Environment env = threadLocalEnvironment.get();
        if (env == null) {
            throw new IllegalStateException("No script has been executed yet in this thread. Call eval() first.");
        }
        Function function = env.getFunctionOrNull(name);
        if (function == null) {
            throw new NoSuchMethodException("Function not found: " + name);
        }
        try {
            Object[] arguments = args != null ? args : new Object[0];
            FunctionContextPool pool = FunctionContextPool.local();
            try (FunctionContext<?> ctx = pool.borrow(function, thiz, arguments, env)) {
                return function.call(ctx);
            }
        } catch (FluxonRuntimeError e) {
            throw toScriptException(e);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getInterface(Class<T> clazz) {
        return getInterface(null, clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getInterface(Object thiz, Class<T> clazz) {
        if (clazz == null || !clazz.isInterface()) {
            throw new IllegalArgumentException("Class must be a non-null interface");
        }
        Environment env = threadLocalEnvironment.get();
        if (env == null) {
            throw new IllegalStateException("No script has been executed yet in this thread. Call eval() first.");
        }
        // 使用动态代理将接口方法映射到 Fluxon 函数
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class<?>[]{clazz},
                new FluxonInvocationHandler(this, thiz, env)
        );
    }

    /**
     * 动态代理处理器，将接口方法调用转发到 Fluxon 函数
     */
    private static class FluxonInvocationHandler implements InvocationHandler {
        private final FluxonScriptEngine engine;
        private final Object thiz;
        private final Environment env;

        FluxonInvocationHandler(FluxonScriptEngine engine, Object thiz, Environment env) {
            this.engine = engine;
            this.thiz = thiz;
            this.env = env;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            // 处理 Object 类的基本方法
            if ("toString".equals(methodName) && (args == null || args.length == 0)) {
                return "FluxonProxy[" + method.getDeclaringClass().getName() + "]";
            }
            if ("hashCode".equals(methodName) && (args == null || args.length == 0)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName) && args != null && args.length == 1) {
                return proxy == args[0];
            }
            // 查找并调用 Fluxon 函数
            Function function = env.getFunctionOrNull(methodName);
            if (function == null) {
                throw new NoSuchMethodException("Function not found: " + methodName);
            }
            Object[] arguments = args != null ? args : new Object[0];
            FunctionContextPool pool = FunctionContextPool.local();
            try (FunctionContext<?> ctx = pool.borrow(function, thiz, arguments, env)) {
                return function.call(ctx);
            }
        }
    }

    /**
     * 获取当前线程最近一次执行的环境
     * 用于高级场景下直接访问 Fluxon 运行时
     * 注意：返回的是线程本地的环境，不同线程返回不同的环境
     */
    public Environment getLastEnvironment() {
        return threadLocalEnvironment.get();
    }

    /**
     * 清除当前线程的环境缓存
     * 建议在线程池场景下，任务结束时调用以避免内存泄漏
     */
    public void clearThreadLocalEnvironment() {
        threadLocalEnvironment.remove();
    }

    // ==================== 执行成本限制 ====================

    /**
     * 应用执行成本限制到解释器
     */
    private void applyCostLimit(Interpreter interpreter, ScriptContext context) {
        Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        if (bindings == null) {
            return;
        }
        // 设置成本限制
        Object limitValue = bindings.get(COST_LIMIT);
        if (limitValue instanceof Number) {
            long limit = ((Number) limitValue).longValue();
            if (limit > 0) {
                interpreter.setCostLimit(limit);
            }
        }
        // 设置每步成本
        Object perStepValue = bindings.get(COST_PER_STEP);
        if (perStepValue instanceof Number) {
            long perStep = ((Number) perStepValue).longValue();
            if (perStep > 0) {
                interpreter.setCostPerStep(perStep);
            }
        }
    }

    // ==================== 错误处理 ====================

    /**
     * 将 FluxonRuntimeError 转换为 ScriptException，保留行列号信息
     */
    private ScriptException toScriptException(FluxonRuntimeError e) {
        SourceExcerpt excerpt = e.getSourceExcerpt();
        if (excerpt != null) {
            return new ScriptException(
                    e.getMessage(),
                    null,  // fileName 已包含在消息中
                    excerpt.getLine(),
                    excerpt.getColumn()
            );
        }
        return new ScriptException(e);
    }
}