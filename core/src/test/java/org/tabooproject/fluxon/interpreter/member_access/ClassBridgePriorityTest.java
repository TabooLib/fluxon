package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.java.ClassBridge;
import org.tabooproject.fluxon.runtime.java.ExportBytecodeGenerator;
import org.tabooproject.fluxon.runtime.java.ExportRegistry;
import org.tabooproject.fluxon.type.TestObject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClassBridge 优先级测试
 * 测试当类已注册 ClassBridge 时，. 操作符的行为
 *
 * @author sky
 */
public class ClassBridgePriorityTest extends MemberAccessTestBase {

    @BeforeEach
    public void setup() {
        // 注册 ClassBridge
        ExportRegistry registry = new ExportRegistry(FluxonRuntime.getInstance());
        registry.registerClass(TestObject.class);
    }

    // ========== ClassBridge 存在性验证 ==========

    @Test
    public void testClassBridgeGenerated() {
        ClassBridge bridge = ExportBytecodeGenerator.getClassBridge(TestObject.class);
        assertNotNull(bridge, "ClassBridge should be generated for TestObject");
    }

    @Test
    public void testClassBridgeSupportsExportedMethods() {
        ClassBridge bridge = ExportBytecodeGenerator.getClassBridge(TestObject.class);
        assertTrue(bridge.supportsMethod("bridgedAdd"), "Should support bridgedAdd");
        assertTrue(bridge.supportsMethod("bridgedMethod"), "Should support bridgedMethod");
        assertTrue(bridge.supportsMethod("bridgedMethodWithArg"), "Should support bridgedMethodWithArg");
    }

    // ========== @Export 方法通过 . 调用 ==========

    @Test
    public void testBridgedMethodNoArgs() {
        assertEquals("bridged-result", interpret("&obj.bridgedMethod()"));
    }

    @Test
    public void testBridgedMethodWithArg() {
        assertEquals("bridged:test", interpret("&obj.bridgedMethodWithArg('test')"));
    }

    @Test
    public void testBridgedAddMethod() {
        // bridgedAdd 返回 a + b + 1000
        assertEquals(1030, interpret("&obj.bridgedAdd(10, 20)"));
    }

    // ========== 非 @Export 方法回退到反射 ==========

    @Test
    public void testNonExportedMethodFallsBackToReflection() {
        // getName() 没有 @Export 注解，应该走反射
        assertEquals("test-object", interpret("&obj.getName()"));
    }

    @Test
    public void testNonExportedFieldFallsBackToReflection() {
        // 字段访问应该走反射
        assertEquals("public-value", interpret("&obj.publicField"));
    }

    // ========== 编译模式下的 ClassBridge ==========

    @Test
    public void testCompiledBridgedMethod() throws Exception {
        assertEquals("bridged-result", compile("&obj.bridgedMethod()"));
    }

    @Test
    public void testCompiledBridgedAdd() throws Exception {
        assertEquals(1015, compile("&obj.bridgedAdd(5, 10)")); // 5 + 10 + 1000
    }

    @Test
    public void testCompiledNonExportedMethod() throws Exception {
        assertEquals("test-object", compile("&obj.getName()"));
    }

    // ========== 混合使用 ==========

    @Test
    public void testMixedBridgeAndReflection() {
        // 在同一表达式中同时使用 bridge 方法和反射方法
        String source = "&obj.bridgedAdd(5, 5) + &obj.add(5, 5)";
        // bridgedAdd: 5 + 5 + 1000 = 1010
        // add: 5 + 5 = 10
        assertEquals(1020, interpret(source));
    }

    @Test
    public void testChainWithBridgedMethod() {
        // 链式调用中包含 bridge 方法
        assertEquals("bridged:hello", interpret("&obj.getSelf().bridgedMethodWithArg('hello')"));
    }

    // ========== 一致性测试 ==========

    @Test
    public void testBridgeConsistency() throws Exception {
        String source = "&obj.bridgedAdd(100, 200)";
        Object interpretResult = interpret(source);
        Object compileResult = compile(source);
        assertEquals(interpretResult, compileResult);
        assertEquals(1300, interpretResult); // 100 + 200 + 1000
    }
}
