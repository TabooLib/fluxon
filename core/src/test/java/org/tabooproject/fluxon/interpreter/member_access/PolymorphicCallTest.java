package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.type.TestObject;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多态调用测试
 * 测试同一 CallSite 处理不同实例、不同类型的场景
 *
 * @author sky
 */
public class PolymorphicCallTest extends MemberAccessTestBase {

    // ========== 同类不同实例 ==========

    @Test
    public void testSameClassDifferentInstances() {
        TestObject obj1 = new TestObject();
        obj1.publicField = "first";
        TestObject obj2 = new TestObject();
        obj2.publicField = "second";

        String source = "&obj1.publicField + '-' + &obj2.publicField";
        Object result = interpret(source, env -> {
            env.defineRootVariable("obj1", obj1);
            env.defineRootVariable("obj2", obj2);
        });
        assertEquals("first-second", result);
    }

    @Test
    public void testSameMethodDifferentInstances() {
        TestObject obj1 = new TestObject();
        TestObject obj2 = new TestObject();

        String source = "&obj1.getName() + '-' + &obj2.getName()";
        Object result = interpret(source, env -> {
            env.defineRootVariable("obj1", obj1);
            env.defineRootVariable("obj2", obj2);
        });
        assertEquals("test-object-test-object", result);
    }

    // ========== 循环中的多态调用 ==========

    @Test
    public void testPolymorphicCallInLoop() {
        List<TestObject> objects = Arrays.asList(
            new TestObject(),
            new TestObject(),
            new TestObject()
        );

        String source = 
            "count = 0; " +
            "for o in &objects { " +
            "  count = &count + 1; " +
            "}; " +
            "&count";

        Object result = interpret(source, env -> {
            env.defineRootVariable("objects", objects);
        });
        assertEquals(3, result);
    }

    @Test
    public void testPolymorphicMethodCallInLoop() {
        List<TestObject> objects = Arrays.asList(
            new TestObject(),
            new TestObject(),
            new TestObject()
        );

        String source = 
            "results = []; " +
            "for o in &objects { " +
            "  results = &results + [&o.getName()]; " +
            "}; " +
            "&results::size()";

        Object result = interpret(source, env -> {
            env.defineRootVariable("objects", objects);
        });
        assertEquals(3, result);
    }

    @Test
    public void testPolymorphicFieldAccessInLoop() {
        TestObject obj1 = new TestObject();
        obj1.intField = 10;
        TestObject obj2 = new TestObject();
        obj2.intField = 20;
        TestObject obj3 = new TestObject();
        obj3.intField = 30;

        List<TestObject> objects = Arrays.asList(obj1, obj2, obj3);

        String source = 
            "sum = 0; " +
            "for o in &objects { " +
            "  sum = &sum + &o.intField; " +
            "}; " +
            "&sum";

        Object result = interpret(source, env -> {
            env.defineRootVariable("objects", objects);
        });
        assertEquals(60, result); // 10 + 20 + 30
    }

    // ========== 编译模式多态调用 ==========

    @Test
    public void testCompiledPolymorphicInstances() throws Exception {
        TestObject obj1 = new TestObject();
        obj1.publicField = "alpha";
        TestObject obj2 = new TestObject();
        obj2.publicField = "beta";

        String source = "&obj1.publicField + '-' + &obj2.publicField";
        Object result = compile(source, env -> {
            env.defineRootVariable("obj1", obj1);
            env.defineRootVariable("obj2", obj2);
        });
        assertEquals("alpha-beta", result);
    }

    @Test
    public void testCompiledPolymorphicLoop() throws Exception {
        List<TestObject> objects = Arrays.asList(
            new TestObject(),
            new TestObject(),
            new TestObject(),
            new TestObject(),
            new TestObject()
        );

        String source = 
            "count = 0; " +
            "for o in &objects { " +
            "  count = &count + 1; " +
            "}; " +
            "&count";

        Object result = compile(source, env -> {
            env.defineRootVariable("objects", objects);
        });
        assertEquals(5, result);
    }

    // ========== 一致性测试 ==========

    @Test
    public void testPolymorphicConsistency() throws Exception {
        TestObject obj1 = new TestObject();
        obj1.intField = 100;
        TestObject obj2 = new TestObject();
        obj2.intField = 200;

        String source = "&obj1.intField + &obj2.intField";

        Object interpretResult = interpret(source, env -> {
            env.defineRootVariable("obj1", obj1);
            env.defineRootVariable("obj2", obj2);
        });

        Object compileResult = compile(source, env -> {
            env.defineRootVariable("obj1", obj1);
            env.defineRootVariable("obj2", obj2);
        });

        assertEquals(interpretResult, compileResult);
        assertEquals(300, interpretResult);
    }

    // ========== 不同字段值的实例 ==========

    @Test
    public void testInstancesWithDifferentFieldValues() {
        TestObject obj1 = new TestObject();
        obj1.booleanField = true;
        TestObject obj2 = new TestObject();
        obj2.booleanField = false;

        String source = "if &obj1.booleanField { 'first-true' } else { 'first-false' }";
        Object result1 = interpret(source, env -> {
            env.defineRootVariable("obj1", obj1);
            env.defineRootVariable("obj2", obj2);
        });
        assertEquals("first-true", result1);

        String source2 = "if &obj2.booleanField { 'second-true' } else { 'second-false' }";
        Object result2 = interpret(source2, env -> {
            env.defineRootVariable("obj1", obj1);
            env.defineRootVariable("obj2", obj2);
        });
        assertEquals("second-false", result2);
    }

    // ========== 嵌套对象的多态访问 ==========

    @Test
    public void testPolymorphicNestedAccess() {
        TestObject obj1 = new TestObject();
        obj1.nested = new TestObject();
        obj1.nested.publicField = "nested1";

        TestObject obj2 = new TestObject();
        obj2.nested = new TestObject();
        obj2.nested.publicField = "nested2";

        String source = "&obj1.nested.publicField + '-' + &obj2.nested.publicField";
        Object result = interpret(source, env -> {
            env.defineRootVariable("obj1", obj1);
            env.defineRootVariable("obj2", obj2);
        });
        assertEquals("nested1-nested2", result);
    }
}
