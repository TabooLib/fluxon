package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.type.TestObject;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字段访问测试
 * 测试各种类型字段的读取：基本类型、包装类型、集合、数组、静态、final等
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class FieldAccessTest extends MemberAccessTestBase {

    // ========== 基本类型字段 ==========

    @Test
    public void testPublicStringField() throws Exception {
        assertEquals("public-value", interpretAndCompile("&obj.publicField"));
    }

    @Test
    public void testPublicIntField() throws Exception {
        assertEquals(42, interpretAndCompile("&obj.intField"));
    }

    @Test
    public void testPublicBooleanField() throws Exception {
        assertEquals(true, interpretAndCompile("&obj.booleanField"));
    }

    @Test
    public void testPublicDoubleField() throws Exception {
        assertEquals(3.14, interpretAndCompile("&obj.doubleField"));
    }

    @Test
    public void testPublicLongField() throws Exception {
        assertEquals(9999999999L, interpretAndCompile("&obj.longField"));
    }

    // ========== 包装类型字段 ==========

    @Test
    public void testBoxedIntegerField() throws Exception {
        assertEquals(100, interpretAndCompile("&obj.boxedIntField"));
    }

    // ========== null 字段 ==========

    @Test
    public void testNullableFieldWithNull() throws Exception {
        assertNull(interpretAndCompile("&obj.nullableField"));
    }

    @Test
    public void testNullableFieldWithValue() throws Exception {
        TestObject obj = new TestObject();
        obj.nullableField = "has-value";
        assertEquals("has-value", interpretAndCompile("&obj.nullableField", obj));
    }

    // ========== 集合类型字段 ==========

    @Test
    @SuppressWarnings("unchecked")
    public void testListField() throws Exception {
        Object result = interpretAndCompile("&obj.listField");
        assertTrue(result instanceof List);
        List<String> list = (List<String>) result;
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMapField() throws Exception {
        Object result = interpretAndCompile("&obj.mapField");
        assertTrue(result instanceof Map);
        Map<String, Integer> map = (Map<String, Integer>) result;
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
    }

    @Test
    public void testArrayField() throws Exception {
        Object result = interpretAndCompile("&obj.arrayField");
        assertTrue(result instanceof String[]);
        String[] arr = (String[]) result;
        assertEquals(3, arr.length);
        assertEquals("x", arr[0]);
        assertEquals("y", arr[1]);
        assertEquals("z", arr[2]);
    }

    // ========== final 字段 ==========

    @Test
    public void testFinalField() throws Exception {
        assertEquals("final-value", interpretAndCompile("&obj.finalField"));
    }

    // ========== 嵌套对象字段 ==========

    @Test
    public void testNestedFieldNull() throws Exception {
        assertNull(interpretAndCompile("&obj.nested"));
    }

    @Test
    public void testNestedFieldWithValue() throws Exception {
        TestObject obj = new TestObject();
        obj.nested = new TestObject();
        obj.nested.publicField = "nested-public";
        Object result = interpretAndCompile("&obj.nested", obj);
        assertTrue(result instanceof TestObject);
    }

    // ========== 字段访问后续操作 ==========

    @Test
    public void testFieldAccessWithExtensionFunction() throws Exception {
        // 获取字段后调用扩展函数
        Object result = interpretAndCompile("&obj.publicField::split('-')");
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertEquals("public", list.get(0));
        assertEquals("value", list.get(1));
    }

    @Test
    public void testListFieldSize() throws Exception {
        assertEquals(3, interpretAndCompile("&obj.listField::size()"));
    }

    @Test
    public void testArrayFieldLength() throws Exception {
        // 数组的 length 是特殊属性
        Object result = interpretAndCompile("&obj.arrayField");
        String[] arr = (String[]) result;
        assertEquals(3, arr.length);
    }

    // ========== 字段值修改后访问 ==========

    @Test
    public void testFieldAfterMutation() throws Exception {
        TestObject obj = new TestObject();
        obj.intField = 999;
        assertEquals(999, interpretAndCompile("&obj.intField", obj));
    }

    @Test
    public void testFieldAfterMultipleMutations() throws Exception {
        TestObject obj = new TestObject();
        obj.publicField = "first";
        assertEquals("first", interpret("&obj.publicField", obj));
        obj.publicField = "second";
        assertEquals("second", interpret("&obj.publicField", obj));
        obj.publicField = "third";
        assertEquals("third", interpret("&obj.publicField", obj));
    }

    // ========== 边界情况 ==========

    @Test
    public void testFieldWithSameNameAsMethod() throws Exception {
        // intField 是字段，不是方法
        assertEquals(42, interpretAndCompile("&obj.intField"));
    }

    @Test
    public void testMultipleFieldAccessInOneExpression() throws Exception {
        // 多个字段访问
        String source = "&obj.intField + &obj.intField";
        assertEquals(84, interpretAndCompile(source));
    }

    @Test
    public void testFieldAccessInLoop() throws Exception {
        String source = 
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 5) { " +
            "  sum = &sum + &obj.intField; " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        assertEquals(210, interpretAndCompile(source)); // 42 * 5
    }

    @Test
    public void testFieldAccessAsCondition() throws Exception {
        String source = "if &obj.booleanField { 'yes' } else { 'no' }";
        assertEquals("yes", interpretAndCompile(source));
    }

    @Test
    public void testFieldAccessAsFunctionArgument() throws Exception {
        // 将字段值作为方法参数
        String source = "&obj.concat(&obj.publicField, '-suffix')";
        assertEquals("public-value-suffix", interpretAndCompile(source));
    }
}
