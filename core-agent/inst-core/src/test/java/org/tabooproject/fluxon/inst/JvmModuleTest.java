package org.tabooproject.fluxon.inst;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.inst.function.FunctionJvm;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * fs:jvm 模块测试。
 * 测试脚本 API：inject/restore/injections。
 */
class JvmModuleTest {

    @BeforeEach
    void setUp() {
        // 注册 fs:jvm 模块
        FunctionJvm.init(FluxonRuntime.getInstance());
        // 清理上一次测试的状态
        InjectionRegistry.getInstance().clear();
        CallbackDispatcher.clearAll();
    }

    @AfterEach
    void tearDown() {
        InjectionRegistry.getInstance().clear();
        CallbackDispatcher.clearAll();
    }

    @Test
    void testImportJvmModule() {
        String script = 
            "import 'fs:jvm'\n" +
            "jvm()";
        
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Object result = Fluxon.eval(script, env);
        
        assertNotNull(result);
        assertSame(FunctionJvm.INSTANCE, result);
    }

    @Test
    void testInjectWithLambdaDefaultsBefore() {
        String script = 
            "import 'fs:jvm'\n" +
            "let id = jvm()::inject('com.example.Foo::bar', 'before', |self, arg| {})\n" +
            "&id";
        
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Object result = Fluxon.eval(script, env);
        
        assertNotNull(result);
        assertTrue(result instanceof String);
        String id = (String) result;
        assertTrue(id.startsWith("inj_"), "ID should start with inj_: " + id);
        
        // 验证注册成功
        assertTrue(InjectionRegistry.getInstance().hasInjectionsForClass("com/example/Foo"));
        assertTrue(CallbackDispatcher.hasCallback(id));
        
        // 验证类型是 BEFORE
        List<InjectionSpec> specs = InjectionRegistry.getInstance().getSpecsForClass("com/example/Foo");
        assertEquals(1, specs.size());
        assertEquals(InjectionType.BEFORE, specs.get(0).getType());
    }

    @Test
    void testInjectWithReplaceType() {
        String script = 
            "import 'fs:jvm'\n" +
            "let id = jvm()::inject('com.example.Foo::bar', 'replace', |self, arg| { return 'replaced' })\n" +
            "&id";
        
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Object result = Fluxon.eval(script, env);
        
        assertNotNull(result);
        String id = (String) result;
        
        // 验证类型是 REPLACE
        List<InjectionSpec> specs = InjectionRegistry.getInstance().getSpecsForClass("com/example/Foo");
        assertEquals(1, specs.size());
        assertEquals(InjectionType.REPLACE, specs.get(0).getType());
    }

    @Test
    void testInjectWithDescriptor() {
        String script = 
            "import 'fs:jvm'\n" +
            "jvm()::inject('com.example.Foo::bar(Ljava/lang/String;)V', 'before', |self, arg| {})";
        
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Fluxon.eval(script, env);
        
        List<InjectionSpec> specs = InjectionRegistry.getInstance().getSpecsForClass("com/example/Foo");
        assertEquals(1, specs.size());
        assertEquals("bar", specs.get(0).getMethodName());
        assertEquals("(Ljava/lang/String;)V", specs.get(0).getMethodDescriptor());
    }

    @Test
    void testRestoreById() {
        String script = 
            "import 'fs:jvm'\n" +
            "let id = jvm()::inject('com.example.Foo::bar', 'before', |self| {})\n" +
            "let before = jvm()::injections()::size()\n" +
            "jvm()::restore(&id)\n" +
            "let after = jvm()::injections()::size()\n" +
            "&before + ',' + &after";
        
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        String result = String.valueOf(Fluxon.eval(script, env));
        
        assertEquals("1,0", result);
    }

    @Test
    void testRestoreByTarget() {
        String script = 
            "import 'fs:jvm'\n" +
            "jvm()::inject('com.example.Foo::bar', 'before', |self| {})\n" +
            "jvm()::inject('com.example.Foo::baz', 'before', |self| {})\n" +
            "let before = jvm()::injections()::size()\n" +
            "jvm()::restore('com.example.Foo::bar')\n" +
            "let after = jvm()::injections()::size()\n" +
            "&before + ',' + &after";
        
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        String result = String.valueOf(Fluxon.eval(script, env));
        
        assertEquals("2,1", result);
    }

    @Test
    void testListInjections() {
        String script = 
            "import 'fs:jvm'\n" +
            "jvm()::inject('com.example.Foo::bar', 'before', |self| {})\n" +
            "jvm()::inject('com.example.Foo::baz', 'replace', |self| { return null })\n" +
            "jvm()::injections()";
        
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) Fluxon.eval(script, env);
        
        assertEquals(2, result.size());
        
        for (Map<String, Object> item : result) {
            assertTrue(item.containsKey("id"));
            assertTrue(item.containsKey("target"));
            assertTrue(item.containsKey("type"));
            assertTrue(((String) item.get("id")).startsWith("inj_"));
            assertTrue(((String) item.get("target")).startsWith("com.example.Foo::"));
            String type = (String) item.get("type");
            assertTrue("before".equals(type) || "replace".equals(type));
        }
    }

    @Test
    void testMultipleInjectionsOnSameMethod() {
        String script = 
            "import 'fs:jvm'\n" +
            "let id1 = jvm()::inject('com.example.Foo::bar', 'before', |self| {})\n" +
            "let id2 = jvm()::inject('com.example.Foo::bar', 'before', |self| {})\n" +
            "[&id1, &id2]";
        
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) Fluxon.eval(script, env);
        
        assertEquals(2, result.size());
        assertNotEquals(result.get(0), result.get(1), "Each injection should have unique ID");
        
        List<InjectionSpec> specs = InjectionRegistry.getInstance().getSpecsForClass("com/example/Foo");
        assertEquals(2, specs.size());
    }

    @Test
    void testInvalidTargetFormat() {
        String script = 
            "import 'fs:jvm'\n" +
            "jvm()::inject('invalid-target-no-separator', 'before', |self| {})";
        
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        
        assertThrows(Exception.class, () -> Fluxon.eval(script, env));
    }

    @Test
    void testInvalidHandlerType() {
        String script = 
            "import 'fs:jvm'\n" +
            "jvm()::inject('com.example.Foo::bar', 'before', 'not-a-function')";
        
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        
        assertThrows(Exception.class, () -> Fluxon.eval(script, env));
    }

    @Test
    void testInjectWithAfterType() {
        String script = 
            "import 'fs:jvm'\n" +
            "let id = jvm()::inject('com.example.Foo::bar', 'after', |self, arg, result| { return result })\n" +
            "&id";
        
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Object result = Fluxon.eval(script, env);
        
        assertNotNull(result);
        String idw = (String) result;
        
        // 验证类型是 AFTER
        List<InjectionSpec> specs = InjectionRegistry.getInstance().getSpecsForClass("com/example/Foo");
        assertEquals(1, specs.size());
        assertEquals(InjectionType.AFTER, specs.get(0).getType());
    }

    @Test
    void testAfterTypeInInjectionsList() {
        String script = 
            "import 'fs:jvm'\n" +
            "jvm()::inject('com.example.Foo::bar', 'after', |self, result| { return result })\n" +
            "jvm()::injections()";
        
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) Fluxon.eval(script, env);
        
        assertEquals(1, result.size());
        Map<String, Object> item = result.get(0);
        assertEquals("after", item.get("type"));
    }

    @Test
    void testInvalidInjectionType() {
        String script = 
            "import 'fs:jvm'\n" +
            "jvm()::inject('com.example.Foo::bar', 'invalid', |self| {})";
        
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        
        assertThrows(Exception.class, () -> Fluxon.eval(script, env));
    }
}
