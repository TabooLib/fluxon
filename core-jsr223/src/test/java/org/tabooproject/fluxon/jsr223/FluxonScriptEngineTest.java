package org.tabooproject.fluxon.jsr223;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.script.*;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FluxonScriptEngine 单元测试
 */
class FluxonScriptEngineTest {

    private ScriptEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FluxonScriptEngine(new FluxonScriptEngineFactory());
    }

    // ==================== 基础功能测试 ====================

    @Test
    void testBasicArithmetic() throws ScriptException {
        Object result = engine.eval("1 + 2 * 3");
        assertEquals(7, result);
    }

    @Test
    void testVariableBinding() throws ScriptException {
        engine.put("x", 10);
        engine.put("y", 20);
        Object result = engine.eval("&x + &y");
        assertEquals(30, result);
    }

    @Test
    void testScriptDefinedVariable() throws ScriptException {
        engine.put("x", 10);
        engine.put("y", 20);
        engine.eval("z = &x * &y");
        assertEquals(200, engine.get("z"));
    }

    @Test
    void testFunctionDefinition() throws ScriptException {
        engine.eval("def factorial(n) = {\n" +
                "    if &n <= 1 {\n" +
                "         1 \n" +
                "    } else {\n" +
                "         &n * factorial(&n - 1) \n" +
                "    };\n" +
                "}");
        engine.put("n", 5);
        Object result = engine.eval("factorial(&n)");
        assertEquals(120, result);
    }

    @Test
    void testCustomBindings() throws ScriptException {
        Bindings bindings = new FluxonBindings();
        bindings.put("a", 30);
        bindings.put("b", 40);
        Object result = engine.eval("&a * &b", bindings);
        assertEquals(1200, result);
    }

    // ==================== Compilable 接口测试 ====================

    @Test
    void testCompileAndEval() throws ScriptException {
        Compilable compilable = (Compilable) engine;
        CompiledScript compiled = compilable.compile("1 + 2 + 3");

        // 多次执行
        assertEquals(6, compiled.eval());
        assertEquals(6, compiled.eval());
        assertEquals(6, compiled.eval());
    }

    @Test
    void testCompiledScriptWithBindings() throws ScriptException {
        Compilable compilable = (Compilable) engine;
        // 先设置变量
        engine.put("x", 1);
        engine.put("y", 2);
        CompiledScript compiled = compilable.compile("&x + &y");

        // 执行 - 应该使用引擎默认上下文的绑定
        assertEquals(3, compiled.eval());

        // 修改绑定值后再次执行
        engine.put("x", 10);
        engine.put("y", 20);
        assertEquals(30, compiled.eval());
    }

    @Test
    void testCompileCache() throws ScriptException {
        engine.put(FluxonScriptEngine.COMPILE_CACHE_ENABLED, true);
        Compilable compilable = (Compilable) engine;

        String script = "1 + 2";
        CompiledScript c1 = compilable.compile(script);
        CompiledScript c2 = compilable.compile(script);

        // 两次编译应该使用缓存
        FluxonScriptEngine fluxonEngine = (FluxonScriptEngine) engine;
        assertEquals(1, fluxonEngine.getCompileCacheSize());

        assertEquals(3, c1.eval());
        assertEquals(3, c2.eval());
    }

    // ==================== Invocable 接口测试 ====================

    @Test
    void testInvokeFunction() throws ScriptException, NoSuchMethodException {
        engine.eval("def greet(name) = 'Hello, ' + &name");
        Invocable invocable = (Invocable) engine;
        Object result = invocable.invokeFunction("greet", "World");
        assertEquals("Hello, World", result);
    }

    @Test
    void testInvokeFunctionMultipleArgs() throws ScriptException, NoSuchMethodException {
        engine.eval("def add(a, b, c) = &a + &b + &c");
        Invocable invocable = (Invocable) engine;
        Object result = invocable.invokeFunction("add", 1, 2, 3);
        assertEquals(6, result);
    }

    @Test
    void testInvokeFunctionNotFound() throws ScriptException {
        engine.eval("1 + 1");  // 需要先执行一个脚本
        Invocable invocable = (Invocable) engine;
        assertThrows(NoSuchMethodException.class, () -> {
            invocable.invokeFunction("nonExistent");
        });
    }

    @Test
    void testInvokeFunctionBeforeEval() {
        Invocable invocable = (Invocable) engine;
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            invocable.invokeFunction("any");
        });
        assertTrue(ex.getMessage().contains("No script has been executed yet"));
    }

    // ==================== I/O 重定向测试 ====================

    @Test
    void testOutputRedirection() throws ScriptException {
        StringWriter writer = new StringWriter();
        SimpleScriptContext ctx = new SimpleScriptContext();
        ctx.setWriter(writer);
        ctx.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);

        engine.eval("print('Hello from Fluxon')", ctx);
        assertTrue(writer.toString().contains("Hello from Fluxon"));
    }

    // ==================== 执行成本限制测试 ====================

    @Test
    void testCostLimit() {
        engine.put(FluxonScriptEngine.COST_LIMIT, 5L);

        // 应该因为成本超限而失败
        assertThrows(ScriptException.class, () -> {
            engine.eval("x = 0; while &x < 100 { x = &x + 1 }");
        });
    }

    // ==================== 工厂测试 ====================

    @Test
    void testEngineFactory() {
        FluxonScriptEngineFactory factory = new FluxonScriptEngineFactory();
        assertEquals("fluxon", factory.getEngineName());
        assertEquals("Fluxon", factory.getLanguageName());
        assertTrue(factory.getExtensions().contains("flx"));
        assertTrue(factory.getExtensions().contains("fluxon"));
        assertTrue(factory.getExtensions().contains("fs"));
        assertTrue(factory.getMimeTypes().contains("application/x-fluxon"));
    }

    @Test
    void testGetEngineByName() {
        ScriptEngineManager manager = new ScriptEngineManager();
        // 注册工厂
        manager.registerEngineName("fluxon", new FluxonScriptEngineFactory());
        ScriptEngine eng = manager.getEngineByName("fluxon");
        assertNotNull(eng);
        assertTrue(eng instanceof FluxonScriptEngine);
    }

    // ==================== 错误处理测试 ====================

    @Test
    void testScriptExceptionWithLineInfo() {
        // 引用未定义的变量应该抛出异常
        ScriptException ex = assertThrows(ScriptException.class, () -> {
            engine.eval("&undefined_variable");
        });
        // 验证异常消息不为空
        assertNotNull(ex.getMessage());
    }

    // ==================== getInterface 测试 ====================

    /**
     * 用于测试 getInterface 的接口
     */
    interface Calculator {
        int add(int a, int b);
        int multiply(int a, int b);
    }

    @Test
    void testGetInterface() throws ScriptException {
        engine.eval("def add(a, b) = &a + &b; def multiply(a, b) = &a * &b");
        Invocable invocable = (Invocable) engine;
        Calculator calc = invocable.getInterface(Calculator.class);
        assertNotNull(calc);
        assertEquals(5, calc.add(2, 3));
        assertEquals(6, calc.multiply(2, 3));
    }

    @Test
    void testGetInterfaceNullClass() throws ScriptException {
        engine.eval("1 + 1");
        Invocable invocable = (Invocable) engine;
        assertThrows(IllegalArgumentException.class, () -> {
            invocable.getInterface(null);
        });
    }

    @Test
    void testGetInterfaceNonInterface() throws ScriptException {
        engine.eval("1 + 1");
        Invocable invocable = (Invocable) engine;
        assertThrows(IllegalArgumentException.class, () -> {
            invocable.getInterface(String.class);
        });
    }

    @Test
    void testGetInterfaceBeforeEval() {
        Invocable invocable = (Invocable) engine;
        assertThrows(IllegalStateException.class, () -> {
            invocable.getInterface(Runnable.class);
        });
    }

    @Test
    void testGetInterfaceMissingMethod() throws ScriptException {
        engine.eval("def add(a, b) = &a + &b");  // 没有定义 multiply
        Invocable invocable = (Invocable) engine;
        Calculator calc = invocable.getInterface(Calculator.class);
        assertNotNull(calc);
        assertEquals(5, calc.add(2, 3));
        // multiply 未定义，应该抛出异常（被 Proxy 包装为 UndeclaredThrowableException）
        Exception ex = assertThrows(Exception.class, () -> {
            calc.multiply(2, 3);
        });
        // 验证根本原因是 NoSuchMethodException
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof NoSuchMethodException, 
                "Expected NoSuchMethodException but got: " + cause.getClass().getName());
        assertTrue(cause.getMessage().contains("multiply"));
    }
}
