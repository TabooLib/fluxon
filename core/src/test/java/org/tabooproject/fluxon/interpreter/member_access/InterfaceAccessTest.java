package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.type.TestInterface;
import org.tabooproject.fluxon.type.TestInterfaceImpl;
import org.tabooproject.fluxon.type.TestSecondInterface;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 接口访问测试
 * 测试通过接口类型访问方法、多接口实现、默认方法等场景
 *
 * @author sky
 */
public class InterfaceAccessTest extends MemberAccessTestBase {

    // ========== 接口方法调用 ==========

    @Test
    public void testInterfaceMethodNoArgs() {
        assertEquals("interface-method", interpret("&obj.interfaceMethod()", new TestInterfaceImpl()));
    }

    @Test
    public void testInterfaceMethodWithArg() {
        assertEquals("interface:hello", interpret("&obj.interfaceMethodWithArg('hello')", new TestInterfaceImpl()));
    }

    @Test
    public void testInterfaceMethodReturningInt() {
        assertEquals(42, interpret("&obj.getInterfaceValue()", new TestInterfaceImpl()));
    }

    // ========== 通过接口类型引用 ==========

    @Test
    public void testInterfaceReference() {
        TestInterface iface = new TestInterfaceImpl();
        assertEquals("interface-method", interpret("&obj.interfaceMethod()", iface));
    }

    @Test
    public void testInterfaceReferenceWithArg() {
        TestInterface iface = new TestInterfaceImpl();
        assertEquals("interface:test", interpret("&obj.interfaceMethodWithArg('test')", iface));
    }

    // ========== 多接口实现 ==========

    @Test
    public void testFirstInterfaceMethod() {
        assertEquals("interface-method", interpret("&obj.interfaceMethod()", new TestInterfaceImpl()));
    }

    @Test
    public void testSecondInterfaceMethod() {
        assertEquals("second-interface", interpret("&obj.secondInterfaceMethod()", new TestInterfaceImpl()));
    }

    @Test
    public void testSecondInterfaceWithArg() {
        assertEquals("second:value", interpret("&obj.secondInterfaceWithArg('value')", new TestInterfaceImpl()));
    }

    @Test
    public void testSecondInterfaceReference() {
        TestSecondInterface iface = new TestInterfaceImpl();
        assertEquals("second-interface", interpret("&obj.secondInterfaceMethod()", iface));
    }

    // ========== 默认方法 ==========

    @Test
    public void testDefaultMethod() {
        assertEquals("default-method", interpret("&obj.defaultMethod()", new TestInterfaceImpl()));
    }

    @Test
    public void testDefaultMethodWithArg() {
        assertEquals("default:arg", interpret("&obj.defaultMethodWithArg('arg')", new TestInterfaceImpl()));
    }

    @Test
    public void testOverriddenDefaultMethod() {
        // 实现类重写了默认方法
        assertEquals("overridden-default", interpret("&obj.overriddenDefault()", new TestInterfaceImpl()));
    }

    // ========== 实现类独有方法 ==========

    @Test
    public void testImplOnlyMethod() {
        assertEquals("impl-only", interpret("&obj.implOnlyMethod()", new TestInterfaceImpl()));
    }

    @Test
    public void testImplField() {
        assertEquals("impl-field", interpret("&obj.implField", new TestInterfaceImpl()));
    }

    // ========== 接口常量 ==========

    @Test
    public void testInterfaceConstant() {
        assertEquals("INTERFACE_CONSTANT", interpret("&obj.INTERFACE_CONSTANT", new TestInterfaceImpl()));
    }

    // ========== 链式调用 ==========

    @Test
    public void testChainedInterfaceMethod() {
        assertEquals("interface-method", interpret("&obj.getSelf().interfaceMethod()", new TestInterfaceImpl()));
    }

    @Test
    public void testChainedMultiInterface() {
        assertEquals("second-interface", interpret("&obj.getSelf().secondInterfaceMethod()", new TestInterfaceImpl()));
    }

    @Test
    public void testChainedDefaultMethod() {
        assertEquals("default-method", interpret("&obj.getSelf().defaultMethod()", new TestInterfaceImpl()));
    }

    // ========== 接口方法作为表达式 ==========

    @Test
    public void testInterfaceMethodInArithmetic() {
        assertEquals(84, interpret("&obj.getInterfaceValue() + &obj.getInterfaceValue()", new TestInterfaceImpl()));
    }

    @Test
    public void testInterfaceMethodAsCondition() {
        String source = "if &obj.getInterfaceValue() > 0 { 'positive' } else { 'negative' }";
        assertEquals("positive", interpret(source, new TestInterfaceImpl()));
    }

    @Test
    public void testInterfaceMethodAsArg() {
        assertEquals("interface-methodinterface-method",
            interpret("&obj.concat(&obj.interfaceMethod(), &obj.interfaceMethod())", new TestInterfaceImpl()));
    }

    // ========== 循环中调用接口方法 ==========

    @Test
    public void testInterfaceMethodInLoop() {
        String source =
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 5) { " +
            "  sum = &sum + &obj.getInterfaceValue(); " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        assertEquals(210, interpret(source, new TestInterfaceImpl())); // 42 * 5
    }

    // ========== 编译模式接口测试 ==========

    @Test
    public void testCompiledInterfaceMethod() throws Exception {
        assertEquals("interface-method", compile("&obj.interfaceMethod()", new TestInterfaceImpl()));
    }

    @Test
    public void testCompiledInterfaceReference() throws Exception {
        TestInterface iface = new TestInterfaceImpl();
        assertEquals("interface:compiled", compile("&obj.interfaceMethodWithArg('compiled')", iface));
    }

    @Test
    public void testCompiledSecondInterface() throws Exception {
        assertEquals("second-interface", compile("&obj.secondInterfaceMethod()", new TestInterfaceImpl()));
    }

    @Test
    public void testCompiledDefaultMethod() throws Exception {
        assertEquals("default-method", compile("&obj.defaultMethod()", new TestInterfaceImpl()));
    }

    @Test
    public void testCompiledOverriddenDefault() throws Exception {
        assertEquals("overridden-default", compile("&obj.overriddenDefault()", new TestInterfaceImpl()));
    }

    // ========== 一致性测试 ==========

    @Test
    public void testInterfaceConsistency() throws Exception {
        TestInterfaceImpl impl = new TestInterfaceImpl();
        String[] sources = {
            "&obj.interfaceMethod()",
            "&obj.interfaceMethodWithArg('test')",
            "&obj.getInterfaceValue()",
            "&obj.secondInterfaceMethod()",
            "&obj.defaultMethod()",
            "&obj.overriddenDefault()",
            "&obj.implOnlyMethod()",
            "&obj.implField"
        };
        
        for (String source : sources) {
            Object interpretResult = interpret(source, impl);
            Object compileResult = compile(source, impl);
            assertEquals(interpretResult, compileResult, "Mismatch for: " + source);
        }
    }

    @Test
    public void testInterfaceReferenceConsistency() throws Exception {
        TestInterface iface = new TestInterfaceImpl();
        String[] sources = {
            "&obj.interfaceMethod()",
            "&obj.interfaceMethodWithArg('x')",
            "&obj.getInterfaceValue()",
            "&obj.defaultMethod()"
        };
        
        for (String source : sources) {
            Object interpretResult = interpret(source, iface);
            Object compileResult = compile(source, iface);
            assertEquals(interpretResult, compileResult, "Mismatch for: " + source);
        }
    }

    // ========== 扩展函数组合 ==========

    @Test
    public void testInterfaceMethodWithExtension() {
        Object result = interpret("&obj.interfaceMethod()::split('-')", new TestInterfaceImpl());
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertEquals("interface", list.get(0));
        assertEquals("method", list.get(1));
    }

    @Test
    public void testChainedInterfaceWithExtension() {
        assertEquals(2, interpret("&obj.interfaceMethod()::split('-')::size()", new TestInterfaceImpl()));
    }
}
