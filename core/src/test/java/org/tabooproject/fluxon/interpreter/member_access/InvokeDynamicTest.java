package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.type.TestObject;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.runtime.java.ClassBridge;
import org.tabooproject.fluxon.runtime.java.ExportBytecodeGenerator;
import org.tabooproject.fluxon.runtime.java.ExportRegistry;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * invokedynamic 字节码模式测试
 * 测试 ReflectionBootstrap 的行为：Bootstrap 执行、CallSite 缓存、多态调用
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class InvokeDynamicTest {

    // ========== Bootstrap Method 首次执行测试 ==========

    @Test
    public void testBootstrapFirstCall() throws Exception {
        // 测试首次调用时 Bootstrap Method 被执行
        // 编译模式下，invokedynamic 指令会触发 bootstrap
        String source = "&obj.getName()";
        Object result = compileWithReflection(source, "TestBootstrapFirst");
        assertEquals("test-object", result);
    }

    @Test
    public void testBootstrapFieldAccess() throws Exception {
        // 测试字段访问的 Bootstrap
        String source = "&obj.publicField";
        Object result = compileWithReflection(source, "TestBootstrapField");
        assertEquals("public-value", result);
    }

    @Test
    public void testBootstrapWithArguments() throws Exception {
        // 测试带参数方法调用的 Bootstrap
        String source = "&obj.add(100, 200)";
        Object result = compileWithReflection(source, "TestBootstrapArgs");
        assertEquals(300, result);
    }

    // ========== CallSite 缓存测试 ==========

    @Test
    public void testCallSiteCaching() throws Exception {
        // 测试 CallSite 缓存：同一编译脚本中多次调用同一方法
        // 第一次调用触发 bootstrap，后续调用直接走 CallSite
        String source = 
            "result = 0; " +
            "i = 0; " +
            "while (&i < 5) { " +
            "  result = &result + &obj.add(1, 2); " +
            "  i = &i + 1; " +
            "}; " +
            "&result";
        Object result = compileWithReflection(source, "TestCallSiteCaching");
        assertEquals(15, result); // 3 * 5 = 15
    }

    @Test
    public void testCallSiteCachingFieldAccess() throws Exception {
        // 测试字段访问的 CallSite 缓存
        String source = 
            "count = 0; " +
            "i = 0; " +
            "while (&i < 10) { " +
            "  &obj.intField; " + // 多次访问同一字段
            "  count = &count + 1; " +
            "  i = &i + 1; " +
            "}; " +
            "&count";
        Object result = compileWithReflection(source, "TestFieldCachingLoop");
        assertEquals(10, result);
    }

    @Test
    public void testMultipleCallSites() throws Exception {
        // 测试多个不同的 CallSite（不同方法调用）
        String source = 
            "a = &obj.add(10, 20); " +      // CallSite 1
            "b = &obj.getName(); " +         // CallSite 2
            "c = &obj.concat('x', 'y'); " +  // CallSite 3
            "d = &obj.intField; " +          // CallSite 4 (field)
            "&a";
        Object result = compileWithReflection(source, "TestMultipleCallSites");
        assertEquals(30, result);
    }

    // ========== 多态调用测试 ==========

    @Test
    public void testPolymorphicCallDifferentInstances() throws Exception {
        // 测试同一 CallSite 处理同一类的不同实例
        String source = 
            "a = &obj1.publicField; " +
            "b = &obj2.publicField; " +
            "&a + '-' + &b";
        
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        
        TestObject obj1 = new TestObject();
        obj1.publicField = "first";
        TestObject obj2 = new TestObject();
        obj2.publicField = "second";
        
        env.defineRootVariable("obj1", obj1);
        env.defineRootVariable("obj2", obj2);
        
        CompileResult compileResult = Fluxon.compile(env, ctx, "TestPolyInstances");
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base = (RuntimeScriptBase) scriptClass.newInstance();
        Object result = base.eval(env);
        
        assertEquals("first-second", result);
    }

    @Test
    public void testPolymorphicCallSameMethod() throws Exception {
        // 测试循环中同一 CallSite 处理不同实例
        String source = 
            "results = []; " +
            "for o in &objects { " +
            "  results = &results + [&o.getName()]; " +
            "}; " +
            "&results::size()";
        
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        
        // 创建多个 TestObject 实例
        List<TestObject> objects = Arrays.asList(new TestObject(), new TestObject(), new TestObject());
        env.defineRootVariable("objects", objects);
        
        CompileResult compileResult = Fluxon.compile(env, ctx, "TestPolySameMethod");
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base = (RuntimeScriptBase) scriptClass.newInstance();
        Object result = base.eval(env);
        
        assertEquals(3, result);
    }

    // ========== 解释器与编译器一致性测试 ==========

    @Test
    public void testInterpreterCompilerConsistencySimple() throws Exception {
        String source = "&obj.add(50, 100)";
        Object interpretResult = runWithReflection(source);
        Object compileResult = compileWithReflection(source, "TestConsistSimple");
        assertEquals(interpretResult, compileResult);
        assertEquals(150, interpretResult);
    }

    @Test
    public void testInterpreterCompilerConsistencyChained() throws Exception {
        String source = "&obj.getSelf().getSelf().getName()";
        Object interpretResult = runWithReflection(source);
        Object compileResult = compileWithReflection(source, "TestConsistChained");
        assertEquals(interpretResult, compileResult);
        assertEquals("test-object", interpretResult);
    }

    @Test
    public void testInterpreterCompilerConsistencyOverload() throws Exception {
        // 测试重载方法在两种模式下的一致性
        String source1 = "&obj.process('test')";
        String source2 = "&obj.process(42)";
        String source3 = "&obj.process('a', 'b')";
        
        Object i1 = runWithReflection(source1);
        Object c1 = compileWithReflection(source1, "TestConsistOverload1");
        assertEquals(i1, c1);
        assertEquals("string:test", i1);
        
        Object i2 = runWithReflection(source2);
        Object c2 = compileWithReflection(source2, "TestConsistOverload2");
        assertEquals(i2, c2);
        assertEquals("int:42", i2);
        
        Object i3 = runWithReflection(source3);
        Object c3 = compileWithReflection(source3, "TestConsistOverload3");
        assertEquals(i3, c3);
        assertEquals("concat:ab", i3);
    }

    @Test
    public void testInterpreterCompilerConsistencyLoop() throws Exception {
        String source = 
            "sum = 0; " +
            "i = 1; " +
            "while (&i <= 10) { " +
            "  sum = &sum + &obj.add(&i, &i); " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        
        Object interpretResult = runWithReflection(source);
        Object compileResult = compileWithReflection(source, "TestConsistLoop");
        assertEquals(interpretResult, compileResult);
        assertEquals(110, interpretResult); // 2*(1+2+...+10) = 2*55 = 110
    }

    // ========== ClassBridge 与 invokedynamic 集成测试 ==========

    @Test
    public void testClassBridgeWithInvokeDynamic() throws Exception {
        // 注册 ClassBridge
        ExportRegistry registry = new ExportRegistry(FluxonRuntime.getInstance());
        registry.registerClass(TestObject.class);
        
        ClassBridge bridge = ExportBytecodeGenerator.getClassBridge(TestObject.class);
        assertNotNull(bridge);
        
        // 使用 invokedynamic 调用 @Export 方法
        String source = "&obj.bridgedAdd(100, 200)";
        Object result = compileWithReflection(source, "TestBridgeIndy");
        assertEquals(1300, result); // 100 + 200 + 1000
    }

    @Test
    public void testMixedBridgeAndReflection() throws Exception {
        // 注册 ClassBridge
        ExportRegistry registry = new ExportRegistry(FluxonRuntime.getInstance());
        registry.registerClass(TestObject.class);
        
        // 同时使用 bridge 方法和反射方法
        String source = 
            "bridged = &obj.bridgedAdd(5, 5); " + // 通过 bridge: 5+5+1000=1010
            "reflect = &obj.add(5, 5); " +        // 通过反射: 5+5=10
            "&bridged + &reflect";
        
        Object result = compileWithReflection(source, "TestMixedBridgeReflect");
        assertEquals(1020, result); // 1010 + 10
    }

    // ========== 错误处理测试 ==========

    @Test
    public void testNullObjectInCompileMode() throws Exception {
        CompilationContext ctx = new CompilationContext("&nullObj.method()");
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("nullObj", null);
        
        CompileResult compileResult = Fluxon.compile(env, ctx, "TestNullCompile");
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base = (RuntimeScriptBase) scriptClass.newInstance();
        
        try {
            base.eval(env);
            fail("Should throw exception for null object");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("null") 
                    || e.getMessage().contains("Null")
                    || e.getCause() instanceof NullPointerException);
        }
    }

    @Test
    public void testNonExistentMethodInCompileMode() throws Exception {
        String source = "&obj.nonExistentMethod()";
        try {
            compileWithReflection(source, "TestNonExistMethod");
            fail("Should throw exception for non-existent method");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("nonExistentMethod") 
                    || e.getMessage().contains("not found")
                    || e.getMessage().contains("Member"));
        }
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
