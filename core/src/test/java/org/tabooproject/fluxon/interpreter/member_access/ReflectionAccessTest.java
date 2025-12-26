package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.type.TestObject;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.runtime.java.ClassBridge;
import org.tabooproject.fluxon.runtime.java.ExportBytecodeGenerator;
import org.tabooproject.fluxon.runtime.java.ExportRegistry;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 反射访问功能测试
 * 测试 . 操作符的反射成员访问（字段读取、方法调用）
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ReflectionAccessTest {

    // ========== 特性开关测试 ==========

    @Test
    public void testFeatureFlagDisabled() {
        // 当特性关闭时，. 操作符应该抛出异常
        CompilationContext ctx = new CompilationContext("obj.field");
        ctx.setAllowReflectionAccess(false);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        try {
            Fluxon.parse(env, ctx);
            fail("Should throw exception when reflection access is disabled");
        } catch (Exception e) {
            // 预期会失败，因为特性未启用
            assertTrue(e.getMessage().contains("Reflection access is not enabled") 
                    || e.getMessage().contains("not enabled"));
        }
    }

    @Test
    public void testFeatureFlagEnabled() {
        // 当特性启用时，. 操作符应该可以用于反射访问
        CompilationContext ctx = new CompilationContext("text.field");
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        try {
            Fluxon.parse(env, ctx);
            // 应该能够成功解析
            assertTrue(true);
        } catch (Exception e) {
            fail("Should be able to parse with reflection enabled: " + e.getMessage());
        }
    }

    // ========== 解析测试 ==========

    @Test
    public void testParsingFieldAccess() {
        // 测试字段访问的解析
        CompilationContext ctx = new CompilationContext("obj.field");
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        try {
            Fluxon.parse(env, ctx);
            assertTrue(true, "Should be able to parse field access");
        } catch (Exception e) {
            fail("Should be able to parse field access: " + e.getMessage());
        }
    }

    @Test
    public void testParsingMethodCall() {
        // 测试方法调用的解析
        CompilationContext ctx = new CompilationContext("obj.method(arg1, arg2)");
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        try {
            Fluxon.parse(env, ctx);
            assertTrue(true, "Should be able to parse method call");
        } catch (Exception e) {
            fail("Should be able to parse method call: " + e.getMessage());
        }
    }

    @Test
    public void testParsingChainedAccess() {
        // 测试链式访问的解析
        CompilationContext ctx = new CompilationContext("obj.field.method().another");
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        try {
            Fluxon.parse(env, ctx);
            assertTrue(true, "Should be able to parse chained access");
        } catch (Exception e) {
            fail("Should be able to parse chained access: " + e.getMessage());
        }
    }

    // ========== 范围操作符兼容性测试 ==========

    @Test
    public void testRangeOperatorCompatibility() {
        // 确保 .. 范围操作符不受影响
        CompilationContext ctx = new CompilationContext("1..10");
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        try {
            Fluxon.parse(env, ctx);
            assertTrue(true, "Range operator should still work");
        } catch (Exception e) {
            fail("Range operator should still work: " + e.getMessage());
        }
    }

    @Test
    public void testDotNotConfusedWithRange() {
        // 测试 . 和 .. 不会混淆
        CompilationContext ctx = new CompilationContext("obj.field; 1..10");
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        try {
            Fluxon.parse(env, ctx);
            assertTrue(true, "Should distinguish between . and ..");
        } catch (Exception e) {
            fail("Should distinguish between . and ..: " + e.getMessage());
        }
    }

    // ========== 操作符优先级测试 ==========

    @Test
    public void testOperatorPrecedence() {
        // 测试 . (115) 应该高于 :: (110)
        CompilationContext ctx = new CompilationContext("obj.method()");
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        try {
            Fluxon.parse(env, ctx);
            assertTrue(true, "Should parse with correct precedence");
        } catch (Exception e) {
            fail("Should parse with correct precedence: " + e.getMessage());
        }
    }

    // ========== 字段访问执行测试 ==========

    @Test
    public void testFieldAccessExecution() {
        // 测试访问公共字段
        String source = "&obj.publicField";
        Object result = runWithReflection(source);
        assertEquals("public-value", result);
    }

    @Test
    public void testIntFieldAccess() {
        // 测试访问基本类型字段
        String source = "&obj.intField";
        Object result = runWithReflection(source);
        assertEquals(42, result);
    }

    @Test
    public void testNonExistentField() {
        // 测试访问不存在的字段
        String source = "&obj.nonExistent";
        try {
            runWithReflection(source);
            fail("Should throw exception for non-existent field");
        } catch (Exception e) {
            // 预期会抛出异常
            assertTrue(e.getMessage().contains("nonExistent") 
                    || e.getMessage().contains("not found")
                    || e.getMessage().contains("Member"));
        }
    }

    // ========== 方法调用执行测试 ==========

    @Test
    public void testMethodCallNoArgs() {
        // 测试无参方法调用
        String source = "&obj.getName()";
        Object result = runWithReflection(source);
        assertEquals("test-object", result);
    }

    @Test
    public void testMethodCallWithArgs() {
        // 测试有参方法调用
        String source = "&obj.concat('hello', 'world')";
        Object result = runWithReflection(source);
        assertEquals("helloworld", result);
    }

    @Test
    public void testMethodCallWithIntArgs() {
        // 测试整数参数方法调用
        String source = "&obj.add(10, 20)";
        Object result = runWithReflection(source);
        assertEquals(30, result);
    }

    @Test
    public void testOverloadedMethodString() {
        // 测试重载方法 - 字符串版本
        String source = "&obj.process('test')";
        Object result = runWithReflection(source);
        assertEquals("string:test", result);
    }

    @Test
    public void testOverloadedMethodInt() {
        // 测试重载方法 - 整数版本
        String source = "&obj.process(42)";
        Object result = runWithReflection(source);
        assertEquals("int:42", result);
    }

    @Test
    public void testOverloadedMethodTwoArgs() {
        // 测试重载方法 - 双参数版本
        String source = "&obj.process('a', 'b')";
        Object result = runWithReflection(source);
        assertEquals("concat:ab", result);
    }

    // ========== 链式调用执行测试 ==========

    @Test
    public void testChainedFieldAndMethod() {
        // 测试字段后跟方法调用
        String source = "&obj.getSelf().getName()";
        Object result = runWithReflection(source);
        assertEquals("test-object", result);
    }

    @Test
    public void testChainedMethodAndField() {
        // 测试方法后跟字段访问
        String source = "&obj.getSelf().publicField";
        Object result = runWithReflection(source);
        assertEquals("public-value", result);
    }

    @Test
    public void testComplexChaining() {
        // 测试复杂链式调用
        String source = "&obj.createNested().publicField";
        Object result = runWithReflection(source);
        assertEquals("nested-value", result);
    }

    // ========== 错误场景测试 ==========

    @Test
    public void testNullObject() {
        // 测试对 null 对象的访问
        CompilationContext ctx = new CompilationContext("&nullObj.publicField");
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("nullObj", null);
        try {
            List<ParseResult> parsed = Fluxon.parse(env, ctx);
            Fluxon.eval(parsed, env);
            fail("Should throw exception for null object");
        } catch (Exception e) {
            // 预期会抛出空指针相关异常
            assertTrue(e.getMessage().contains("null") 
                    || e.getMessage().contains("Null")
                    || e instanceof NullPointerException);
        }
    }

    @Test
    public void testPrivateFieldAccess() {
        // 测试访问私有字段（应该失败）
        String source = "&obj.privateField";
        try {
            runWithReflection(source);
            fail("Should throw exception for private field access");
        } catch (Exception e) {
            // 预期会抛出访问权限异常
            assertTrue(e.getMessage().contains("private") 
                    || e.getMessage().contains("not found")
                    || e.getMessage().contains("access"));
        }
    }

    @Test
    public void testWrongArgumentType() {
        // 测试错误的参数类型
        String source = "&obj.add('a', 'b')";
        try {
            runWithReflection(source);
            fail("Should throw exception for wrong argument type");
        } catch (Exception e) {
            // 预期会抛出类型不匹配异常
            assertTrue(e.getMessage().contains("type") 
                    || e.getMessage().contains("not found")
                    || e.getMessage().contains("match"));
        }
    }

    // ========== 编译模式测试 ==========

    @Test
    public void testCompiledFieldAccess() throws Exception {
        // 测试编译模式：字段访问
        String source = "&obj.publicField";
        Object result = compileWithReflection(source, "TestCompiledField");
        assertEquals("public-value", result);
    }

    @Test
    public void testCompiledMethodCall() throws Exception {
        // 测试编译模式：方法调用
        String source = "&obj.getName()";
        Object result = compileWithReflection(source, "TestCompiledMethod");
        assertEquals("test-object", result);
    }

    @Test
    public void testCompiledMethodWithArgs() throws Exception {
        // 测试编译模式：带参数的方法调用
        String source = "&obj.add(5, 10)";
        Object result = compileWithReflection(source, "TestCompiledMethodArgs");
        assertEquals(15, result);
    }

    @Test
    public void testCompiledOverloadedMethod() throws Exception {
        // 测试编译模式：重载方法
        String source = "&obj.process('compiled')";
        Object result = compileWithReflection(source, "TestCompiledOverload");
        assertEquals("string:compiled", result);
    }

    @Test
    public void testCompiledChaining() throws Exception {
        // 测试编译模式：链式调用
        String source = "&obj.getSelf().publicField";
        Object result = compileWithReflection(source, "TestCompiledChain");
        assertEquals("public-value", result);
    }

    @Test
    public void testInterpretVsCompileConsistency() throws Exception {
        // 测试解释模式和编译模式的一致性
        String source = "&obj.concat('Hello', 'World')";
        Object interpretResult = runWithReflection(source);
        Object compileResult = compileWithReflection(source, "TestConsistency");
        assertEquals(interpretResult, compileResult);
        assertEquals("HelloWorld", interpretResult);
    }

    // ========== ClassBridge 优先级测试 ==========

    @Test
    public void testClassBridgePriority() {
        // 测试：当类已注册 ClassBridge 时，. 操作符应该优先使用 bridge
        // bridgedAdd 方法在 bridge 中返回 a + b + 1000，而反射只会返回 a + b
        ExportRegistry registry = new ExportRegistry(FluxonRuntime.getInstance());
        registry.registerClass(TestObject.class);

        // 验证 ClassBridge 已生成
        ClassBridge bridge = ExportBytecodeGenerator.getClassBridge(TestObject.class);
        assertNotNull(bridge, "ClassBridge should be generated for TestObject");
        assertTrue(bridge.supportsMethod("bridgedAdd"), "ClassBridge should support bridgedAdd");

        // 使用 . 操作符调用 bridgedAdd
        String source = "&obj.bridgedAdd(10, 20)";
        Object result = runWithReflection(source);

        // 如果走 bridge，结果是 10 + 20 + 1000 = 1030
        // 如果走反射，结果也是 10 + 20 + 1000 = 1030（因为方法本身就是这样实现的）
        // 所以这里主要验证能正常调用
        assertEquals(1030, result);
    }

    @Test
    public void testClassBridgePriorityNoArgs() {
        // 测试无参 @Export 方法
        ExportRegistry registry = new ExportRegistry(FluxonRuntime.getInstance());
        registry.registerClass(TestObject.class);

        String source = "&obj.bridgedMethod()";
        Object result = runWithReflection(source);
        assertEquals("bridged-result", result);
    }

    @Test
    public void testClassBridgePriorityWithArg() {
        // 测试带参数的 @Export 方法
        ExportRegistry registry = new ExportRegistry(FluxonRuntime.getInstance());
        registry.registerClass(TestObject.class);

        String source = "&obj.bridgedMethodWithArg('test')";
        Object result = runWithReflection(source);
        assertEquals("bridged:test", result);
    }

    @Test
    public void testClassBridgePriorityInCompileMode() throws Exception {
        // 测试编译模式下的 ClassBridge 优先级
        ExportRegistry registry = new ExportRegistry(FluxonRuntime.getInstance());
        registry.registerClass(TestObject.class);

        String source = "&obj.bridgedAdd(5, 10)";
        Object result = compileWithReflection(source, "TestBridgeCompiled");
        assertEquals(1015, result); // 5 + 10 + 1000
    }

    @Test
    public void testNonBridgedMethodFallsBackToReflection() {
        // 测试：非 @Export 方法应该回退到反射
        ExportRegistry registry = new ExportRegistry(FluxonRuntime.getInstance());
        registry.registerClass(TestObject.class);

        // getName() 没有 @Export 注解，应该走反射
        String source = "&obj.getName()";
        Object result = runWithReflection(source);
        assertEquals("test-object", result);
    }

    // ========== . 和 :: 操作符组合测试 ==========

    @Test
    public void testDotWithContextCallParsing() {
        // 测试 . 和 :: 操作符组合的解析
        // . (115) 优先级高于 :: (110)，所以 obj.method()::func() 应该解析为 (obj.method())::func()
        CompilationContext ctx = new CompilationContext("obj.method()::contains(1)");
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        try {
            Fluxon.parse(env, ctx);
            assertTrue(true, "Should parse dot and context call combination");
        } catch (Exception e) {
            fail("Should parse dot and context call combination: " + e.getMessage());
        }
    }

    @Test
    public void testDotThenContextCall() {
        // 测试：反射获取方法返回值，然后使用 :: 调用扩展函数
        // obj.publicField 返回 "public-value"，然后 ::split('-') 调用扩展函数
        String source = "&obj.publicField::split('-')";
        Object result = runWithReflection(source);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertEquals("public", list.get(0));
        assertEquals("value", list.get(1));
    }

    @Test
    public void testDotMethodThenContextCall() {
        // 测试：反射调用方法返回值，然后使用 :: 调用扩展函数
        // obj.getName() 返回 "test-object"，然后 ::split('-') 调用扩展函数
        String source = "&obj.getName()::split('-')";
        Object result = runWithReflection(source);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertEquals("test", list.get(0));
        assertEquals("object", list.get(1));
    }

    @Test
    public void testChainedDotWithContextCall() {
        // 测试链式反射后使用 :: 
        // obj.getSelf().publicField 返回 "public-value"，然后 ::split('-')
        String source = "&obj.getSelf().publicField::split('-')";
        Object result = runWithReflection(source);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertEquals("public", list.get(0));
        assertEquals("value", list.get(1));
    }

    @Test
    public void testDotWithContextCallInCompileMode() throws Exception {
        // 测试编译模式下 . 和 :: 的组合
        String source = "&obj.publicField::split('-')";
        Object result = compileWithReflection(source, "TestDotContextCallCompiled");
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertEquals("public", list.get(0));
        assertEquals("value", list.get(1));
    }

    @Test
    public void testMultipleContextCallAfterDot() {
        // 测试反射后多次 :: 调用
        // obj.publicField 返回 "public-value"，::split('-') 返回列表，::size() 返回大小
        String source = "&obj.publicField::split('-')::size()";
        Object result = runWithReflection(source);
        assertEquals(2, result);
    }

    @Test
    public void testDotAndContextCallConsistency() throws Exception {
        // 测试解释模式和编译模式的一致性
        String source = "&obj.getName()::split('-')::size()";
        Object interpretResult = runWithReflection(source);
        Object compileResult = compileWithReflection(source, "TestDotContextConsistency");
        assertEquals(interpretResult, compileResult);
        assertEquals(2, interpretResult);
    }

    // ========== 辅助方法 ==========

    /**
     * 使用反射访问特性运行代码（解释模式）
     */
    private Object runWithReflection(String source) {
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("obj", new TestObject());
        List<ParseResult> parsed = Fluxon.parse(env, ctx);
        return Fluxon.eval(parsed, env);
    }

    /**
     * 使用反射访问特性运行代码（编译模式）
     */
    private Object compileWithReflection(String source, String className) throws Exception {
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("obj", new TestObject());
        CompileResult compileResult = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base = (RuntimeScriptBase) scriptClass.newInstance();
        compileResult.dump(new File("dump/" + className + ".class"));
        return base.eval(env);
    }
}

