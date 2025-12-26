package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.runtime.reflection.util.PolymorphicInlineCache;
import org.tabooproject.fluxon.type.TestChild;
import org.tabooproject.fluxon.type.TestGrandChild;
import org.tabooproject.fluxon.type.TestParent;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 继承场景测试
 * 测试子类对象访问父类成员、方法重写、多态等场景
 *
 * @author sky
 */
public class InheritanceTest extends MemberAccessTestBase {

    // ========== 字段遮蔽测试辅助类 ==========

    public static class ShadowParent {
        public String value = "parent";
    }

    public static class ShadowChild extends ShadowParent {
        public String value = "child";
    }

    // ========== 父类字段访问 ==========

    @Test
    public void testParentPublicField() {
        assertEquals("parent-field", interpret("&obj.parentField", new TestChild()));
    }

    @Test
    public void testParentProtectedFieldFromChild() {
        // 通过子类访问继承的 protected 字段（已转为 public getter）
        assertEquals("protected-value", interpret("&obj.getProtectedValue()", new TestChild()));
    }

    @Test
    public void testChildOwnField() {
        assertEquals("child-field", interpret("&obj.childField", new TestChild()));
    }

    @Test
    public void testGrandChildOwnField() {
        assertEquals("grandchild-field", interpret("&obj.grandChildField", new TestGrandChild()));
    }

    // ========== 继承链字段访问 ==========

    @Test
    public void testGrandChildAccessParentField() {
        assertEquals("parent-field", interpret("&obj.parentField", new TestGrandChild()));
    }

    @Test
    public void testGrandChildAccessChildField() {
        assertEquals("child-field", interpret("&obj.childField", new TestGrandChild()));
    }

    // ========== 父类方法访问 ==========

    @Test
    public void testParentMethod() {
        assertEquals("parent-method", interpret("&obj.parentMethod()", new TestChild()));
    }

    @Test
    public void testParentMethodWithArgs() {
        assertEquals("parent:hello", interpret("&obj.parentMethodWithArg('hello')", new TestChild()));
    }

    // ========== 方法重写 ==========

    @Test
    public void testOverriddenMethod() {
        // 子类重写了父类方法，应该调用子类版本
        assertEquals("child-override", interpret("&obj.overridableMethod()", new TestChild()));
    }

    @Test
    public void testParentOverridableMethod() {
        // 父类对象调用原始方法
        assertEquals("parent-overridable", interpret("&obj.overridableMethod()", new TestParent()));
    }

    @Test
    public void testGrandChildOverride() {
        // 孙子类再次重写
        assertEquals("grandchild-override", interpret("&obj.overridableMethod()", new TestGrandChild()));
    }

    // ========== 子类独有方法 ==========

    @Test
    public void testChildOnlyMethod() {
        assertEquals("child-only", interpret("&obj.childOnlyMethod()", new TestChild()));
    }

    @Test
    public void testGrandChildOnlyMethod() {
        assertEquals("grandchild-only", interpret("&obj.grandChildOnlyMethod()", new TestGrandChild()));
    }

    // ========== 方法重载与继承 ==========

    @Test
    public void testInheritedOverloadNoArgs() {
        assertEquals("parent-overload:0", interpret("&obj.overloadedMethod()", new TestChild()));
    }

    @Test
    public void testInheritedOverloadOneArg() {
        assertEquals("parent-overload:1:test", interpret("&obj.overloadedMethod('test')", new TestChild()));
    }

    @Test
    public void testChildAddedOverload() {
        // 子类添加了新的重载版本
        assertEquals("child-overload:2:a:b", interpret("&obj.overloadedMethod('a', 'b')", new TestChild()));
    }

    // ========== 链式调用与继承 ==========

    @Test
    public void testChainedInheritedMethod() {
        assertEquals("parent-field", interpret("&obj.getSelf().parentField", new TestChild()));
    }

    @Test
    public void testChainedOverriddenMethod() {
        assertEquals("child-override", interpret("&obj.getSelf().overridableMethod()", new TestChild()));
    }

    @Test
    public void testChainedChildMethod() {
        assertEquals("child-only", interpret("&obj.getSelf().childOnlyMethod()", new TestChild()));
    }

    // ========== super 调用相关 ==========

    @Test
    public void testCallSuperMethod() {
        // 通过子类方法间接调用父类实现
        assertEquals("super:parent-overridable", interpret("&obj.callSuperMethod()", new TestChild()));
    }

    // ========== 多态场景 ==========

    @Test
    public void testPolymorphicFieldAccess() {
        // 父类引用指向子类对象，访问字段
        TestParent parent = new TestChild();
        assertEquals("parent-field", interpret("&obj.parentField", parent));
    }

    @Test
    public void testPolymorphicMethodCall() {
        // 父类引用指向子类对象，调用重写方法
        TestParent parent = new TestChild();
        assertEquals("child-override", interpret("&obj.overridableMethod()", parent));
    }

    @Test
    public void testPolymorphicGrandChild() {
        TestParent parent = new TestGrandChild();
        assertEquals("grandchild-override", interpret("&obj.overridableMethod()", parent));
    }

    // ========== 编译模式继承测试 ==========

    @Test
    public void testCompiledParentField() throws Exception {
        assertEquals("parent-field", compile("&obj.parentField", new TestChild()));
    }

    @Test
    public void testCompiledOverriddenMethod() throws Exception {
        assertEquals("child-override", compile("&obj.overridableMethod()", new TestChild()));
    }

    @Test
    public void testCompiledChildOnlyMethod() throws Exception {
        assertEquals("child-only", compile("&obj.childOnlyMethod()", new TestChild()));
    }

    @Test
    public void testCompiledInheritedOverload() throws Exception {
        assertEquals("parent-overload:1:x", compile("&obj.overloadedMethod('x')", new TestChild()));
    }

    @Test
    public void testCompiledPolymorphic() throws Exception {
        TestParent parent = new TestGrandChild();
        assertEquals("grandchild-override", compile("&obj.overridableMethod()", parent));
    }

    // ========== 一致性测试 ==========

    @Test
    public void testInheritanceConsistency() throws Exception {
        TestChild child = new TestChild();
        String[] sources = {
            "&obj.parentField",
            "&obj.childField",
            "&obj.parentMethod()",
            "&obj.overridableMethod()",
            "&obj.childOnlyMethod()",
            "&obj.overloadedMethod()",
            "&obj.overloadedMethod('test')"
        };
        
        for (String source : sources) {
            Object interpretResult = interpret(source, child);
            Object compileResult = compile(source, child);
            assertEquals(interpretResult, compileResult, "Mismatch for: " + source);
        }
    }

    @Test
    public void testGrandChildConsistency() throws Exception {
        TestGrandChild grandChild = new TestGrandChild();
        String[] sources = {
            "&obj.parentField",
            "&obj.childField",
            "&obj.grandChildField",
            "&obj.overridableMethod()",
            "&obj.grandChildOnlyMethod()"
        };
        
        for (String source : sources) {
            Object interpretResult = interpret(source, grandChild);
            Object compileResult = compile(source, grandChild);
            assertEquals(interpretResult, compileResult, "Mismatch for: " + source);
        }
    }

    // ========== 循环中的继承方法调用 ==========

    @Test
    public void testInheritedMethodInLoop() {
        String source =
            "result = ''; " +
            "i = 0; " +
            "while (&i < 3) { " +
            "  result = &result + &obj.overridableMethod(); " +
            "  i = &i + 1; " +
            "}; " +
            "&result";
        assertEquals("child-overridechild-overridechild-override", interpret(source, new TestChild()));
    }

    // ========== 作为参数传递 ==========

    @Test
    public void testInheritedFieldAsArg() {
        assertEquals("parent-fieldparent-field", 
            interpret("&obj.concat(&obj.parentField, &obj.parentField)", new TestChild()));
    }

    @Test
    public void testMixedFieldsAsArgs() {
        assertEquals("parent-fieldchild-field",
            interpret("&obj.concat(&obj.parentField, &obj.childField)", new TestChild()));
    }

    // ========== 编译模式字段遮蔽测试 ==========

    @Test
    public void testCompiledFieldShadowingUsesExactClass() throws Exception {
        PolymorphicInlineCache.clear();
        String source = "&obj.value";
        String className = generateClassName();

        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);

        Environment compileEnv = FluxonRuntime.getInstance().newEnvironment();
        compileEnv.defineRootVariable("obj", new ShadowParent());

        CompileResult compileResult = Fluxon.compile(compileEnv, ctx, className);
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase script = (RuntimeScriptBase) scriptClass.newInstance();

        Environment parentEnv = FluxonRuntime.getInstance().newEnvironment();
        parentEnv.defineRootVariable("obj", new ShadowParent());
        Object parentResult = script.eval(parentEnv);
        assertEquals("parent", parentResult);

        Environment childEnv = FluxonRuntime.getInstance().newEnvironment();
        childEnv.defineRootVariable("obj", new ShadowChild());
        Object childResult = script.eval(childEnv);
        assertEquals("child", childResult);
    }
}
