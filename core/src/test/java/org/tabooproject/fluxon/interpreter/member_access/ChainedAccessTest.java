package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.type.TestObject;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 链式调用测试
 * 测试 . 操作符链式调用、与 :: 操作符组合、深层嵌套等场景
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ChainedAccessTest extends MemberAccessTestBase {

    // ========== 基本链式调用 ==========

    @Test
    public void testMethodThenField() throws Exception {
        assertEquals("public-value", interpretAndCompile("&obj.getSelf().publicField"));
    }

    @Test
    public void testMethodThenMethod() throws Exception {
        assertEquals("test-object", interpretAndCompile("&obj.getSelf().getName()"));
    }

    @Test
    public void testFieldThenMethod() throws Exception {
        TestObject obj = new TestObject();
        obj.nested = new TestObject();
        assertEquals("test-object", interpretAndCompile("&obj.nested.getName()", obj));
    }

    @Test
    public void testFieldThenField() throws Exception {
        TestObject obj = new TestObject();
        obj.nested = new TestObject();
        obj.nested.publicField = "nested-public";
        assertEquals("nested-public", interpretAndCompile("&obj.nested.publicField", obj));
    }

    // ========== 多层链式调用 ==========

    @Test
    public void testThreeLevelChain() throws Exception {
        assertEquals("test-object", interpretAndCompile("&obj.getSelf().getSelf().getName()"));
    }

    @Test
    public void testFourLevelChain() throws Exception {
        assertEquals("public-value", interpretAndCompile("&obj.getSelf().getSelf().getSelf().publicField"));
    }

    @Test
    public void testDeepNestedField() throws Exception {
        assertEquals("level1-value", interpretAndCompile("&obj.getLevel1().publicField"));
    }

    @Test
    public void testDeepNestedTwoLevels() throws Exception {
        assertEquals("level2-value", interpretAndCompile("&obj.getLevel1().nested.publicField"));
    }

    @Test
    public void testDeepNestedThreeLevels() throws Exception {
        assertEquals("level3-value", interpretAndCompile("&obj.getLevel1().nested.nested.publicField"));
    }

    // ========== 方法链改变状态 ==========

    @Test
    public void testMutateChain() throws Exception {
        assertEquals("new-value", interpretAndCompile("&obj.mutate('new-value').publicField"));
    }

    @Test
    public void testMultipleMutateChain() throws Exception {
        assertEquals("final", interpretAndCompile("&obj.mutate('first').mutate('second').mutate('final').publicField"));
    }

    // ========== . 和 :: 操作符组合 ==========

    @Test
    public void testDotThenContextCall() throws Exception {
        Object result = interpretAndCompile("&obj.publicField::split('-')");
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertEquals("public", list.get(0));
        assertEquals("value", list.get(1));
    }

    @Test
    public void testMethodThenContextCall() throws Exception {
        Object result = interpretAndCompile("&obj.getName()::split('-')");
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertEquals("test", list.get(0));
        assertEquals("object", list.get(1));
    }

    @Test
    public void testChainThenContextCall() throws Exception {
        Object result = interpretAndCompile("&obj.getSelf().publicField::split('-')");
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
    }

    @Test
    public void testMultipleContextCallAfterDot() throws Exception {
        assertEquals(2, interpretAndCompile("&obj.publicField::split('-')::size()"));
    }

    @Test
    public void testChainWithMultipleContextCalls() throws Exception {
        assertEquals(2, interpretAndCompile("&obj.getName()::split('-')::size()"));
    }

    // ========== 链式调用与表达式 ==========

    @Test
    public void testChainResultInArithmetic() throws Exception {
        assertEquals(84, interpretAndCompile("&obj.getSelf().intField + &obj.getSelf().intField"));
    }

    @Test
    public void testChainResultAsCondition() throws Exception {
        assertEquals("yes", interpretAndCompile("if &obj.getSelf().booleanField { 'yes' } else { 'no' }"));
    }

    @Test
    public void testChainResultAsFunctionArg() throws Exception {
        assertEquals("public-valuepublic-value", 
            interpretAndCompile("&obj.concat(&obj.getSelf().publicField, &obj.getSelf().publicField)"));
    }

    // ========== 链式调用在循环中 ==========

    @Test
    public void testChainInLoop() throws Exception {
        String source = 
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 3) { " +
            "  sum = &sum + &obj.getSelf().intField; " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        assertEquals(126, interpretAndCompile(source)); // 42 * 3
    }

    @Test
    public void testChainMethodCallInLoop() throws Exception {
        String source = 
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 5) { " +
            "  sum = &sum + &obj.getSelf().add(1, 2); " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        assertEquals(15, interpretAndCompile(source)); // 3 * 5
    }

    // ========== 混合场景 ==========

    @Test
    public void testComplexChainWithMethodArgs() throws Exception {
        // 链式调用结果作为另一个链式方法的参数
        assertEquals("public-valuepublic-value", 
            interpretAndCompile("&obj.getSelf().concat(&obj.publicField, &obj.getSelf().publicField)"));
    }

    @Test
    public void testChainWithIntermediateExtension() throws Exception {
        // 在链中间使用 :: 扩展函数
        Object result = interpretAndCompile("&obj.getName()::split('-')");
        assertTrue(result instanceof List);
        assertEquals(2, ((List<?>) result).size());
    }

    // ========== 返回 null 的链式调用 ==========

    @Test
    public void testChainReturningNull() throws Exception {
        assertNull(interpretAndCompile("&obj.getSelf().getNullValue()"));
    }

    @Test
    public void testChainWithNullField() throws Exception {
        assertNull(interpretAndCompile("&obj.nested")); // nested 默认为 null
    }

    // ========== 返回新对象的链式调用 ==========

    @Test
    public void testCreateNestedChain() throws Exception {
        assertEquals("nested-value", interpretAndCompile("&obj.createNested().publicField"));
    }

    // ========== 链式调用中的类型变化 ==========

    @Test
    public void testChainStringToList() throws Exception {
        Object result = interpretAndCompile("&obj.getName()::split('-')");
        assertTrue(result instanceof List);
    }

    @Test
    public void testChainListToInt() throws Exception {
        assertEquals(2, interpretAndCompile("&obj.getName()::split('-')::size()"));
    }

    // ========== 范围操作符兼容性 ==========

    @Test
    public void testDotNotConfusedWithRange() throws Exception {
        // 确保 . 和 .. 不混淆
        // 解析并执行验证
        interpretAndCompile("&obj.publicField; 1..10");
    }

    @Test
    public void testRangeAfterChain() throws Exception {
        // 链式调用后使用范围
        String source = "x = &obj.intField; 1..&x";
        Object result = interpretAndCompile(source);
        assertTrue(result instanceof List);
        assertEquals(42, ((List<?>) result).size());
    }
}
