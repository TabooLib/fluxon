package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property Setter 测试
 * 测试 Kotlin 风格的属性赋值语法
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class PropertySetterTest extends MemberAccessTestBase {

    public static class MutableObject {
        public String name = "initial";
        public int count = 0;
        public MutableObject inner = null;

        public void setName(String name) {
            this.name = "setter:" + name;
        }

        public String getName() {
            return name;
        }
    }

    public static class SetterOnlyObject {
        private String value = "initial";

        public void setValue(String value) {
            this.value = "setter:" + value;
        }

        public String getValue() {
            return value;
        }
    }

    @Test
    public void testSimpleFieldAssignment() throws Exception {
        MutableObject obj = new MutableObject();
        interpret("&obj.count = 42", obj);
        assertEquals(42, obj.count);
    }

    @Test
    public void testSimpleFieldAssignmentCompile() throws Exception {
        MutableObject obj = new MutableObject();
        compile("&obj.count = 42", obj);
        assertEquals(42, obj.count);
    }

    @Test
    public void testSetterMethodAssignment() throws Exception {
        MutableObject obj = new MutableObject();
        interpret("&obj.name = 'test'", obj);
        assertEquals("setter:test", obj.name);
    }

    @Test
    public void testSetterMethodAssignmentCompile() throws Exception {
        MutableObject obj = new MutableObject();
        compile("&obj.name = 'test'", obj);
        assertEquals("setter:test", obj.name);
    }

    @Test
    public void testSetterOnlyObject() throws Exception {
        SetterOnlyObject obj = new SetterOnlyObject();
        interpret("&obj.value = 'new'", obj);
        assertEquals("setter:new", obj.getValue());
    }

    @Test
    public void testSetterOnlyObjectCompile() throws Exception {
        SetterOnlyObject obj = new SetterOnlyObject();
        compile("&obj.value = 'new'", obj);
        assertEquals("setter:new", obj.getValue());
    }

    @Test
    public void testCompoundAddAssignment() throws Exception {
        MutableObject obj = new MutableObject();
        obj.count = 10;
        interpret("&obj.count += 5", obj);
        assertEquals(15, obj.count);
    }

    @Test
    public void testCompoundAddAssignmentCompile() throws Exception {
        MutableObject obj = new MutableObject();
        obj.count = 10;
        compile("&obj.count += 5", obj);
        assertEquals(15, obj.count);
    }

    @Test
    public void testCompoundSubtractAssignment() throws Exception {
        MutableObject obj = new MutableObject();
        obj.count = 10;
        interpret("&obj.count -= 3", obj);
        assertEquals(7, obj.count);
    }

    @Test
    public void testCompoundMultiplyAssignment() throws Exception {
        MutableObject obj = new MutableObject();
        obj.count = 5;
        interpret("&obj.count *= 4", obj);
        assertEquals(20, obj.count);
    }

    @Test
    public void testCompoundDivideAssignment() throws Exception {
        MutableObject obj = new MutableObject();
        obj.count = 20;
        interpret("&obj.count /= 4", obj);
        assertEquals(5, obj.count);
    }

    @Test
    public void testCompoundModuloAssignment() throws Exception {
        MutableObject obj = new MutableObject();
        obj.count = 17;
        interpret("&obj.count %= 5", obj);
        assertEquals(2, obj.count);
    }

    @Test
    public void testSafeAssignmentOnNull() throws Exception {
        MutableObject obj = new MutableObject();
        obj.inner = null;
        interpret("&obj.inner?.count = 999", obj);
        assertNull(obj.inner);
    }

    @Test
    public void testSafeAssignmentOnNullCompile() throws Exception {
        MutableObject obj = new MutableObject();
        obj.inner = null;
        compile("&obj.inner?.count = 999", obj);
        assertNull(obj.inner);
    }

    @Test
    public void testSafeAssignmentOnNonNull() throws Exception {
        MutableObject obj = new MutableObject();
        obj.inner = new MutableObject();
        interpret("&obj.inner?.count = 999", obj);
        assertEquals(999, obj.inner.count);
    }

    @Test
    public void testSafeAssignmentOnNonNullCompile() throws Exception {
        MutableObject obj = new MutableObject();
        obj.inner = new MutableObject();
        compile("&obj.inner?.count = 999", obj);
        assertEquals(999, obj.inner.count);
    }

    @Test
    public void testChainedAssignment() throws Exception {
        MutableObject obj = new MutableObject();
        obj.inner = new MutableObject();
        interpret("&obj.inner.count = 123", obj);
        assertEquals(123, obj.inner.count);
    }

    @Test
    public void testChainedAssignmentCompile() throws Exception {
        MutableObject obj = new MutableObject();
        obj.inner = new MutableObject();
        compile("&obj.inner.count = 123", obj);
        assertEquals(123, obj.inner.count);
    }

    @Test
    public void testDeepChainedAssignment() throws Exception {
        MutableObject obj = new MutableObject();
        obj.inner = new MutableObject();
        obj.inner.inner = new MutableObject();
        interpret("&obj.inner.inner.name = 'deep'", obj);
        assertEquals("setter:deep", obj.inner.inner.name);
    }

    @Test
    public void testAssignmentWithExpression() throws Exception {
        MutableObject obj = new MutableObject();
        interpret("&obj.count = 10 + 20 * 2", obj);
        assertEquals(50, obj.count);
    }

    @Test
    public void testAssignmentWithExpressionCompile() throws Exception {
        MutableObject obj = new MutableObject();
        compile("&obj.count = 10 + 20 * 2", obj);
        assertEquals(50, obj.count);
    }

    @Test
    public void testMultipleAssignments() throws Exception {
        MutableObject obj = new MutableObject();
        interpret("&obj.count = 1; &obj.count = 2; &obj.count = 3", obj);
        assertEquals(3, obj.count);
    }

    @Test
    public void testAssignmentInLoop() throws Exception {
        MutableObject obj = new MutableObject();
        interpret("i = 0; while (&i < 5) { &obj.count += 10; i = &i + 1 }", obj);
        assertEquals(50, obj.count);
    }

    @Test
    public void testNullAssignmentThrowsException() {
        MutableObject obj = new MutableObject();
        obj.inner = null;
        assertThrows(NullPointerException.class, () -> {
            interpret("&obj.inner.count = 999", obj);
        });
    }
}
