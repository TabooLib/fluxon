package org.tabooproject.fluxon.inst;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.inst.function.FunctionJvm;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字节码注入测试。
 * <p>
 * 分为两部分：
 * 1. 回调分发测试（无需 Agent）- 模拟注入字节码调用 CallbackDispatcher
 * 2. 真实注入测试（需要 Agent）- 验证完整的字节码注入流程
 */
class BytecodeInjectionTest {

    @BeforeAll
    static void initModule() {
        FunctionJvm.init(FluxonRuntime.getInstance());
    }

    @BeforeEach
    void setUp() {
        InjectionRegistry.getInstance().clear();
        CallbackDispatcher.clearAll();
    }

    @AfterEach
    void tearDown() {
        InjectionRegistry.getInstance().clear();
        CallbackDispatcher.clearAll();
    }

    static boolean isAgentAvailable() {
        return InstrumentationBridge.isAvailable();
    }

    private static Object evalWithReflection(String source, Environment env) {
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        List<ParseResult> results = Fluxon.parse(env, ctx);
        return Fluxon.eval(results, env);
    }

    // ==================== 回调分发测试 ====================

    @Test
    @DisplayName("实例方法 - 字符串拼接")
    void dispatchInstanceMethod_StringConcat() {
        String script =
                "import 'fs:jvm'\n" +
                "jvm()::inject('test.Foo::bar', |self, name| {\n" +
                "    return 'Hello, ' + &name + '!'\n" +
                "})";

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        String id = (String) Fluxon.eval(script, env);

        Object result = CallbackDispatcher.dispatchReplace(id, new Object[]{new Object(), "World"});
        assertEquals("Hello, World!", result);
    }

    @Test
    @DisplayName("实例方法 - 多参数算术")
    void dispatchInstanceMethod_MultipleArgs() {
        String script =
                "import 'fs:jvm'\n" +
                "jvm()::inject('test.Math::calc', [\n" +
                "    type: 'replace',\n" +
                "    handler: |self, a, b, c| { return (&a + &b) * &c }\n" +
                "])";

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        String id = (String) Fluxon.eval(script, env);

        Object result = CallbackDispatcher.dispatchReplace(id, new Object[]{new Object(), 2, 3, 4});
        assertEquals(20, ((Number) result).intValue());
    }

    @Test
    @DisplayName("静态方法 - 无 self")
    void dispatchStaticMethod() {
        String script =
                "import 'fs:jvm'\n" +
                "jvm()::inject('test.Utils::format', |text| {\n" +
                "    return '[' + &text + ']'\n" +
                "})";

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        String id = (String) Fluxon.eval(script, env);

        Object result = CallbackDispatcher.dispatchReplace(id, new Object[]{"test"});
        assertEquals("[test]", result);
    }

    @Test
    @DisplayName("Before - 返回 null 继续执行")
    void dispatchBefore_Proceed() {
        String script =
                "import 'fs:jvm'\n" +
                "jvm()::inject('test.Service::process', |self, data| { return null })";

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        String id = (String) Fluxon.eval(script, env);

        boolean shouldProceed = CallbackDispatcher.dispatchBefore(id, new Object[]{new Object(), "data"});
        assertTrue(shouldProceed);
    }

    @Test
    @DisplayName("Before - 返回 SKIP 跳过")
    void dispatchBefore_Skip() {
        String script =
                "import 'fs:jvm'\n" +
                "jvm()::inject('test.Service::process', |self, data| { return '__FLUXON_SKIP__' })";

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        String id = (String) Fluxon.eval(script, env);

        Object result = CallbackDispatcher.dispatch(id, new Object[]{new Object(), "data"});
        assertEquals("__FLUXON_SKIP__", result);
    }

    @Test
    @DisplayName("回调访问 self")
    void dispatchCallback_AccessSelf() {
        String script =
                "import 'fs:jvm'\n" +
                "jvm()::inject('test.Entity::getName', |self| { return &self })";

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        String id = (String) Fluxon.eval(script, env);

        String mockThis = "MockEntity";
        Object result = CallbackDispatcher.dispatchReplace(id, new Object[]{mockThis});
        assertSame(mockThis, result);
    }

    @Test
    @DisplayName("回调返回多种类型")
    void dispatchCallback_VariousReturnTypes() {
        // Boolean
        String id1 = (String) Fluxon.eval(
                "import 'fs:jvm'\njvm()::inject('test.Check::isValid', |self| { return true })",
                FluxonRuntime.getInstance().newEnvironment());
        assertEquals(true, CallbackDispatcher.dispatchReplace(id1, new Object[]{new Object()}));

        // Double
        String id2 = (String) Fluxon.eval(
                "import 'fs:jvm'\njvm()::inject('test.Math::getPi', |self| { return 3.14159 })",
                FluxonRuntime.getInstance().newEnvironment());
        assertEquals(3.14159, ((Number) CallbackDispatcher.dispatchReplace(id2, new Object[]{new Object()})).doubleValue(), 0.00001);

        // Null -> PROCEED
        String id3 = (String) Fluxon.eval(
                "import 'fs:jvm'\njvm()::inject('test.Factory::create', [type: 'replace', handler: |self| { return null }])",
                FluxonRuntime.getInstance().newEnvironment());
        assertEquals(CallbackDispatcher.PROCEED, CallbackDispatcher.dispatchReplace(id3, new Object[]{new Object()}));
    }

    @Test
    @DisplayName("未注册回调返回 PROCEED")
    void dispatchNonExistent_ReturnsProceed() {
        Object result = CallbackDispatcher.dispatch("non-existent-id", new Object[]{"arg"});
        assertSame(CallbackDispatcher.PROCEED, result);
    }

    @Test
    @DisplayName("注销后返回 PROCEED")
    void dispatchAfterUnregister_ReturnsProceed() {
        String script =
                "import 'fs:jvm'\n" +
                "jvm()::inject('test.Foo::bar', |self| { return 'injected' })";

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        String id = (String) Fluxon.eval(script, env);

        assertEquals("injected", CallbackDispatcher.dispatchReplace(id, new Object[]{new Object()}));
        CallbackDispatcher.unregister(id);
        assertSame(CallbackDispatcher.PROCEED, CallbackDispatcher.dispatch(id, new Object[]{new Object()}));
    }

    // ==================== 真实注入测试（需要 Agent）====================

    @Test
    @EnabledIf("isAgentAvailable")
    @DisplayName("Replace - 替换字符串返回值")
    void realInject_ReplaceString() {
        String script =
                "import 'fs:jvm'\n" +
                "jvm()::inject('org.tabooproject.fluxon.inst.BytecodeInjectionTest$TargetClass::sayHello', [\n" +
                "    type: 'replace',\n" +
                "    handler: |self, name| { return 'Replaced: ' + &name }\n" +
                "])";

        Fluxon.eval(script, FluxonRuntime.getInstance().newEnvironment());

        TargetClass target = new TargetClass();
        assertEquals("Replaced: Fluxon", target.sayHello("Fluxon"));
    }

    @Test
    @EnabledIf("isAgentAvailable")
    @DisplayName("Replace - 替换数值返回值")
    void realInject_ReplaceNumber() {
        String script =
                "import 'fs:jvm'\n" +
                "jvm()::inject('org.tabooproject.fluxon.inst.BytecodeInjectionTest$TargetClass::getValue', [\n" +
                "    type: 'replace',\n" +
                "    handler: |self| { return 999 }\n" +
                "])";

        String id = (String) Fluxon.eval(script, FluxonRuntime.getInstance().newEnvironment());

        TargetClass target = new TargetClass();
        assertEquals(999, target.getValue());

        InjectionRegistry.getInstance().unregister(id);
    }

    @Test
    @EnabledIf("isAgentAvailable")
    @DisplayName("Restore - 恢复原方法")
    void realInject_Restore() {
        TargetClass target = new TargetClass();
        int originalValue = target.getValue();

        String script =
                "import 'fs:jvm'\n" +
                "jvm()::inject('org.tabooproject.fluxon.inst.BytecodeInjectionTest$TargetClass::getValue', [\n" +
                "    type: 'replace',\n" +
                "    handler: |self| { return 888 }\n" +
                "])";

        String id = (String) Fluxon.eval(script, FluxonRuntime.getInstance().newEnvironment());
        assertEquals(888, target.getValue());

        Fluxon.eval("import 'fs:jvm'\njvm()::restore('" + id + "')", FluxonRuntime.getInstance().newEnvironment());
        assertEquals(originalValue, target.getValue());
    }

    @Test
    @EnabledIf("isAgentAvailable")
    @DisplayName("Before - 捕获调用")
    void realInject_BeforeCapture() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        List<String> captured = new ArrayList<>();
        env.assign("captured", captured, -1);

        String script =
                "import 'fs:jvm'\n" +
                "jvm()::inject('org.tabooproject.fluxon.inst.BytecodeInjectionTest$TargetClass::sayHello', |self, name| {\n" +
                "    &captured::add('before: ' + &name)\n" +
                "    return null\n" +
                "})";

        Fluxon.eval(script, env);

        TargetClass target = new TargetClass();
        String result = target.sayHello("Fluxon");

        assertEquals(1, captured.size());
        assertEquals("before: Fluxon", captured.get(0));
        assertEquals("Hello, Fluxon!", result);
    }

    @Test
    @EnabledIf("isAgentAvailable")
    @DisplayName("Replace - 回调中调用 self 方法")
    void realInject_CallSelfMethod() {
        String script =
                "import 'fs:jvm'\n" +
                "jvm()::inject('org.tabooproject.fluxon.inst.BytecodeInjectionTest$TargetClass::greet', [\n" +
                "    type: 'replace',\n" +
                "    handler: |self, name| {\n" +
                "        let value = &self.getValue()\n" +
                "        return 'Hello ' + &name + ', value=' + &value\n" +
                "    }\n" +
                "])";

        evalWithReflection(script, FluxonRuntime.getInstance().newEnvironment());

        TargetClass target = new TargetClass();
        assertEquals("Hello World, value=42", target.greet("World"));
    }

    @Test
    @EnabledIf("isAgentAvailable")
    @DisplayName("Replace - 修改环境状态")
    void realInject_ModifyEnvState() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        List<String> captured = new ArrayList<>();
        env.assign("captured", captured, -1);

        String script =
                "import 'fs:jvm'\n" +
                "jvm()::inject('org.tabooproject.fluxon.inst.BytecodeInjectionTest$TargetClass::add', [\n" +
                "    type: 'replace',\n" +
                "    handler: |self, a, b| {\n" +
                "        &captured::add('a=' + &a)\n" +
                "        &captured::add('b=' + &b)\n" +
                "        return &a + &b + 100\n" +
                "    }\n" +
                "])";

        Fluxon.eval(script, env);

        TargetClass target = new TargetClass();
        int result = target.add(3, 5);

        assertEquals(2, captured.size());
        assertEquals("a=3", captured.get(0));
        assertEquals("b=5", captured.get(1));
        assertEquals(108, result);
    }

    // ==================== 测试目标类 ====================

    public static class TargetClass {
        public String sayHello(String name) {
            return "Hello, " + name + "!";
        }

        public String greet(String name) {
            return "Greetings, " + name;
        }

        public int add(int a, int b) {
            return a + b;
        }

        public int getValue() {
            return 42;
        }
    }
}
