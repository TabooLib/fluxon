package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.type.TestGeneric;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 泛型方法测试
 * 测试泛型类、泛型方法、类型参数推断等场景
 *
 * @author sky
 */
public class GenericMethodTest extends MemberAccessTestBase {

    // ========== 泛型类字段访问 ==========

    @Test
    public void testGenericFieldString() {
        TestGeneric<String> gen = new TestGeneric<>();
        gen.value = "generic-value";
        assertEquals("generic-value", interpret("&obj.value", gen));
    }

    @Test
    public void testGenericFieldInteger() {
        TestGeneric<Integer> gen = new TestGeneric<>();
        gen.value = 42;
        assertEquals(42, interpret("&obj.value", gen));
    }

    @Test
    public void testGenericFieldList() {
        TestGeneric<List<String>> gen = new TestGeneric<>();
        gen.value = Arrays.asList("a", "b", "c");
        Object result = interpret("&obj.value", gen);
        assertTrue(result instanceof List);
        assertEquals(3, ((List<?>) result).size());
    }

    @Test
    public void testGenericFieldNull() {
        TestGeneric<String> gen = new TestGeneric<>();
        gen.value = null;
        assertNull(interpret("&obj.value", gen));
    }

    // ========== 泛型类方法调用 ==========

    @Test
    public void testGetValueString() {
        TestGeneric<String> gen = new TestGeneric<>();
        gen.value = "test";
        assertEquals("test", interpret("&obj.getValue()", gen));
    }

    @Test
    public void testGetValueInteger() {
        TestGeneric<Integer> gen = new TestGeneric<>();
        gen.value = 100;
        assertEquals(100, interpret("&obj.getValue()", gen));
    }

    @Test
    public void testSetAndGetValue() {
        TestGeneric<String> gen = new TestGeneric<>();
        interpret("&obj.setValue('new-value')", gen);
        assertEquals("new-value", gen.value);
    }

    @Test
    public void testTransformValue() {
        TestGeneric<String> gen = new TestGeneric<>();
        gen.value = "hello";
        assertEquals("HELLO", interpret("&obj.transformValue()", gen));
    }

    // ========== 泛型方法 ==========

    @Test
    public void testGenericMethodIdentity() {
        TestGeneric<String> gen = new TestGeneric<>();
        assertEquals("identity-test", interpret("&obj.identity('identity-test')", gen));
    }

    @Test
    public void testGenericMethodIdentityInt() {
        TestGeneric<String> gen = new TestGeneric<>();
        assertEquals(999, interpret("&obj.identity(999)", gen));
    }

    @Test
    public void testGenericMethodWrap() {
        TestGeneric<String> gen = new TestGeneric<>();
        Object result = interpret("&obj.wrap('wrapped')", gen);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        assertEquals("wrapped", list.get(0));
    }

    @Test
    public void testGenericMethodPair() {
        TestGeneric<String> gen = new TestGeneric<>();
        Object result = interpret("&obj.pair('key', 'value')", gen);
        assertTrue(result instanceof Map);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("value", map.get("key"));
    }

    @Test
    public void testGenericMethodPairMixedTypes() {
        TestGeneric<String> gen = new TestGeneric<>();
        Object result = interpret("&obj.pair('count', 42)", gen);
        assertTrue(result instanceof Map);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(42, map.get("count"));
    }

    // ========== 泛型返回类型 ==========

    @Test
    public void testGenericReturnList() {
        TestGeneric<String> gen = new TestGeneric<>();
        Object result = interpret("&obj.getList()", gen);
        assertTrue(result instanceof List);
    }

    @Test
    public void testGenericReturnMap() {
        TestGeneric<String> gen = new TestGeneric<>();
        Object result = interpret("&obj.getMap()", gen);
        assertTrue(result instanceof Map);
    }

    @Test
    public void testGenericCreateList() {
        TestGeneric<String> gen = new TestGeneric<>();
        Object result = interpret("&obj.createList('a', 'b', 'c')", gen);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    // ========== 泛型边界 ==========

    @Test
    public void testBoundedGenericNumber() {
        TestGeneric<String> gen = new TestGeneric<>();
        assertEquals(10.0, interpret("&obj.sumNumbers(3, 7)", gen));
    }

    @Test
    public void testBoundedGenericDouble() {
        TestGeneric<String> gen = new TestGeneric<>();
        assertEquals(5.5, interpret("&obj.sumNumbers(2.5, 3.0)", gen));
    }

    @Test
    public void testComparableMax() {
        TestGeneric<String> gen = new TestGeneric<>();
        assertEquals("z", interpret("&obj.max('a', 'z')", gen));
    }

    @Test
    public void testComparableMaxInt() {
        TestGeneric<String> gen = new TestGeneric<>();
        assertEquals(100, interpret("&obj.max(50, 100)", gen));
    }

    // ========== 嵌套泛型 ==========

    @Test
    public void testNestedGeneric() {
        TestGeneric<List<String>> gen = new TestGeneric<>();
        gen.value = Arrays.asList("nested", "list");
        Object result = interpret("&obj.getValue()", gen);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
    }

    @Test
    public void testNestedGenericChain() {
        TestGeneric<List<String>> gen = new TestGeneric<>();
        gen.value = Arrays.asList("a", "b", "c");
        assertEquals(3, interpret("&obj.getValue()::size()", gen));
    }

    // ========== 链式调用 ==========

    @Test
    public void testGenericChainedField() {
        TestGeneric<String> gen = new TestGeneric<>();
        gen.value = "chain-test";
        assertEquals("chain-test", interpret("&obj.getSelf().value", gen));
    }

    @Test
    public void testGenericChainedMethod() {
        TestGeneric<String> gen = new TestGeneric<>();
        gen.value = "chained";
        assertEquals("chained", interpret("&obj.getSelf().getValue()", gen));
    }

    @Test
    public void testGenericChainedTransform() {
        TestGeneric<String> gen = new TestGeneric<>();
        gen.value = "transform";
        assertEquals("TRANSFORM", interpret("&obj.getSelf().transformValue()", gen));
    }

    // ========== 循环中使用泛型 ==========

    @Test
    public void testGenericInLoop() {
        TestGeneric<Integer> gen = new TestGeneric<>();
        gen.value = 10;
        String source =
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 5) { " +
            "  sum = &sum + &obj.getValue(); " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        assertEquals(50, interpret(source, gen));
    }

    // ========== 编译模式泛型测试 ==========

    @Test
    public void testCompiledGenericField() throws Exception {
        TestGeneric<String> gen = new TestGeneric<>();
        gen.value = "compiled-generic";
        assertEquals("compiled-generic", compile("&obj.value", gen));
    }

    @Test
    public void testCompiledGenericMethod() throws Exception {
        TestGeneric<String> gen = new TestGeneric<>();
        gen.value = "compiled-method";
        assertEquals("compiled-method", compile("&obj.getValue()", gen));
    }

    @Test
    public void testCompiledGenericIdentity() throws Exception {
        TestGeneric<String> gen = new TestGeneric<>();
        assertEquals("identity-compiled", compile("&obj.identity('identity-compiled')", gen));
    }

    @Test
    public void testCompiledGenericWrap() throws Exception {
        TestGeneric<String> gen = new TestGeneric<>();
        Object result = compile("&obj.wrap('compiled-wrap')", gen);
        assertTrue(result instanceof List);
        assertEquals("compiled-wrap", ((List<?>) result).get(0));
    }

    @Test
    public void testCompiledGenericTransform() throws Exception {
        TestGeneric<String> gen = new TestGeneric<>();
        gen.value = "compile";
        assertEquals("COMPILE", compile("&obj.transformValue()", gen));
    }

    // ========== 一致性测试 ==========

    @Test
    public void testGenericConsistency() throws Exception {
        TestGeneric<String> gen = new TestGeneric<>();
        gen.value = "consistency";
        
        String[] sources = {
            "&obj.value",
            "&obj.getValue()",
            "&obj.transformValue()",
            "&obj.identity('test')",
            "&obj.wrap('item')",
            "&obj.getSelf().value"
        };
        
        for (String source : sources) {
            Object interpretResult = interpret(source, gen);
            Object compileResult = compile(source, gen);
            assertEquals(interpretResult, compileResult, "Mismatch for: " + source);
        }
    }

    @Test
    public void testGenericIntegerConsistency() throws Exception {
        TestGeneric<Integer> gen = new TestGeneric<>();
        gen.value = 42;
        
        Object interpretResult = interpret("&obj.getValue()", gen);
        Object compileResult = compile("&obj.getValue()", gen);
        assertEquals(interpretResult, compileResult);
        assertEquals(42, interpretResult);
    }

    // ========== 与扩展函数组合 ==========

    @Test
    public void testGenericWithExtension() {
        TestGeneric<String> gen = new TestGeneric<>();
        gen.value = "extend-test";
        Object result = interpret("&obj.getValue()::split('-')", gen);
        assertTrue(result instanceof List);
        assertEquals(2, ((List<?>) result).size());
    }

    @Test
    public void testGenericWrapWithExtension() {
        TestGeneric<String> gen = new TestGeneric<>();
        assertEquals(1, interpret("&obj.wrap('item')::size()", gen));
    }

    @Test
    public void testGenericCreateListWithExtension() {
        TestGeneric<String> gen = new TestGeneric<>();
        assertEquals(3, interpret("&obj.createList('a', 'b', 'c')::size()", gen));
    }
}
