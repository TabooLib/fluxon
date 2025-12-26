package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.type.TestObject;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数组和集合索引访问测试
 * 测试通过反射获取数组/集合后的索引访问
 *
 * @author sky
 */
public class ArrayAccessTest extends MemberAccessTestBase {

    // ========== 数组字段访问后索引 ==========

    @Test
    public void testArrayFieldFirstElement() {
        assertEquals("x", interpret("&obj.arrayField[0]"));
    }

    @Test
    public void testArrayFieldMiddleElement() {
        assertEquals("y", interpret("&obj.arrayField[1]"));
    }

    @Test
    public void testArrayFieldLastElement() {
        assertEquals("z", interpret("&obj.arrayField[2]"));
    }

    // ========== 数组方法返回值索引 ==========

    @Test
    public void testArrayMethodFirstElement() {
        assertEquals("arr1", interpret("&obj.getArray()[0]"));
    }

    @Test
    public void testArrayMethodMiddleElement() {
        assertEquals("arr2", interpret("&obj.getArray()[1]"));
    }

    @Test
    public void testArrayMethodLastElement() {
        assertEquals("arr3", interpret("&obj.getArray()[2]"));
    }

    // ========== 基本类型数组 ==========

    @Test
    public void testPrimitiveArrayFirstElement() {
        assertEquals(1, interpret("&obj.getPrimitiveArray()[0]"));
    }

    @Test
    public void testPrimitiveArrayMiddleElement() {
        assertEquals(3, interpret("&obj.getPrimitiveArray()[2]"));
    }

    @Test
    public void testPrimitiveArrayLastElement() {
        assertEquals(5, interpret("&obj.getPrimitiveArray()[4]"));
    }

    // ========== List 字段索引访问 ==========

    @Test
    public void testListFieldFirstElement() {
        assertEquals("a", interpret("&obj.listField[0]"));
    }

    @Test
    public void testListFieldMiddleElement() {
        assertEquals("b", interpret("&obj.listField[1]"));
    }

    @Test
    public void testListFieldLastElement() {
        assertEquals("c", interpret("&obj.listField[2]"));
    }

    // ========== List 方法返回值索引 ==========

    @Test
    public void testListMethodFirstElement() {
        assertEquals("item1", interpret("&obj.getList()[0]"));
    }

    @Test
    public void testListMethodMiddleElement() {
        assertEquals("item2", interpret("&obj.getList()[1]"));
    }

    @Test
    public void testListMethodLastElement() {
        assertEquals("item3", interpret("&obj.getList()[2]"));
    }

    // ========== 链式调用后索引 ==========

    @Test
    public void testChainedArrayAccess() {
        assertEquals("x", interpret("&obj.getSelf().arrayField[0]"));
    }

    @Test
    public void testChainedListAccess() {
        assertEquals("a", interpret("&obj.getSelf().listField[0]"));
    }

    @Test
    public void testChainedMethodArrayAccess() {
        assertEquals("arr1", interpret("&obj.getSelf().getArray()[0]"));
    }

    // ========== 索引访问后继续操作 ==========

    @Test
    public void testArrayElementWithExtension() {
        // 获取数组元素后调用扩展函数
        Object result = interpret("&obj.arrayField[0]::split('')");
        assertTrue(result instanceof List);
    }

    @Test
    public void testArrayElementAsMethodArg() {
        assertEquals("xy", interpret("&obj.concat(&obj.arrayField[0], &obj.arrayField[1])"));
    }

    // ========== 动态索引 ==========

    @Test
    public void testDynamicIndex() {
        String source = "idx = 1; &obj.arrayField[&idx]";
        assertEquals("y", interpret(source));
    }

    @Test
    public void testDynamicIndexFromMethod() {
        // 使用方法返回值作为索引
        String source = "idx = &obj.add(0, 1); &obj.arrayField[&idx]";
        assertEquals("y", interpret(source));
    }

    // ========== 循环中的索引访问 ==========

    @Test
    public void testArrayAccessInLoop() {
        String source =
            "result = ''; " +
            "i = 0; " +
            "while (&i < 3) { " +
            "  result = &result + &obj.arrayField[&i]; " +
            "  i = &i + 1; " +
            "}; " +
            "&result";
        assertEquals("xyz", interpret(source));
    }

    @Test
    public void testListAccessInLoop() {
        String source =
            "result = ''; " +
            "i = 0; " +
            "while (&i < 3) { " +
            "  result = &result + &obj.listField[&i]; " +
            "  i = &i + 1; " +
            "}; " +
            "&result";
        assertEquals("abc", interpret(source));
    }

    // ========== 多维数组/嵌套集合 ==========

    @Test
    public void testNestedArrayAccess() {
        // 通过嵌套对象访问数组
        TestObject obj = new TestObject();
        obj.nested = new TestObject();
        assertEquals("x", interpret("&obj.nested.arrayField[0]", obj));
    }

    // ========== 编译模式索引访问 ==========

    @Test
    public void testCompiledArrayAccess() throws Exception {
        assertEquals("x", compile("&obj.arrayField[0]"));
    }

    @Test
    public void testCompiledListAccess() throws Exception {
        assertEquals("a", compile("&obj.listField[0]"));
    }

    @Test
    public void testCompiledPrimitiveArrayAccess() throws Exception {
        assertEquals(1, compile("&obj.getPrimitiveArray()[0]"));
    }

    @Test
    public void testCompiledDynamicIndex() throws Exception {
        assertEquals("y", compile("idx = 1; &obj.arrayField[&idx]"));
    }

    // ========== 一致性测试 ==========

    @Test
    public void testArrayAccessConsistency() throws Exception {
        String source = "&obj.arrayField[1]";
        assertEquals(interpret(source), compile(source));
    }

    @Test
    public void testListAccessConsistency() throws Exception {
        String source = "&obj.listField[2]";
        assertEquals(interpret(source), compile(source));
    }

    @Test
    public void testChainedAccessConsistency() throws Exception {
        String source = "&obj.getSelf().getArray()[0]";
        assertEquals(interpret(source), compile(source));
    }

    // ========== 索引运算 ==========

    @Test
    public void testIndexArithmetic() {
        assertEquals("z", interpret("&obj.arrayField[1 + 1]"));
    }

    @Test
    public void testIndexFromVariable() {
        String source = "start = 0; offset = 2; &obj.arrayField[&start + &offset]";
        assertEquals("z", interpret(source));
    }

    // ========== 多次索引访问 ==========

    @Test
    public void testMultipleIndexAccesses() {
        String source = "&obj.arrayField[0] + &obj.arrayField[1] + &obj.arrayField[2]";
        assertEquals("xyz", interpret(source));
    }

    @Test
    public void testMixedArrayAndListAccess() {
        String source = "&obj.arrayField[0] + &obj.listField[0]";
        assertEquals("xa", interpret(source));
    }
}
