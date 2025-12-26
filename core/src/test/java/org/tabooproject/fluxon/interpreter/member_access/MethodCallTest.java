package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.type.TestObject;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 方法调用测试
 * 测试各种方法调用场景：无参、有参、重载、可变参数、返回各种类型等
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class MethodCallTest extends MemberAccessTestBase {

    // ========== 无参方法 ==========

    @Test
    public void testNoArgsMethodReturningString() throws Exception {
        assertEquals("test-object", interpretAndCompile("&obj.getName()"));
    }

    @Test
    public void testNoArgsMethodReturningInt() throws Exception {
        assertEquals(100, interpretAndCompile("&obj.getNumber()"));
    }

    @Test
    public void testNoArgsMethodReturningBoolean() throws Exception {
        assertEquals(true, interpretAndCompile("&obj.isEnabled()"));
    }

    @Test
    public void testNoArgsMethodReturningDouble() throws Exception {
        assertEquals(3.14159, interpretAndCompile("&obj.getDouble()"));
    }

    @Test
    public void testNoArgsMethodReturningLong() throws Exception {
        assertEquals(Long.MAX_VALUE, interpretAndCompile("&obj.getLong()"));
    }

    @Test
    public void testNoArgsMethodReturningChar() throws Exception {
        assertEquals('X', interpretAndCompile("&obj.getChar()"));
    }

    @Test
    public void testNoArgsMethodReturningFloat() throws Exception {
        assertEquals(2.718f, interpretAndCompile("&obj.getFloat()"));
    }

    // ========== 返回 null ==========

    @Test
    public void testMethodReturningNull() throws Exception {
        assertNull(interpretAndCompile("&obj.getNullValue()"));
    }

    @Test
    public void testMethodReturningNullObject() throws Exception {
        assertNull(interpretAndCompile("&obj.getNull()"));
    }

    // ========== 返回 void ==========

    @Test
    public void testVoidMethod() throws Exception {
        // void 方法返回 null
        assertNull(interpretAndCompile("&obj.voidMethod()"));
    }

    @Test
    public void testVoidMethodWithArg() throws Exception {
        assertNull(interpretAndCompile("&obj.voidMethodWithArg('test')"));
    }

    // ========== 返回集合类型 ==========

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodReturningList() throws Exception {
        Object result = interpretAndCompile("&obj.getList()");
        assertTrue(result instanceof List);
        List<String> list = (List<String>) result;
        assertEquals(3, list.size());
        assertEquals("item1", list.get(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodReturningMap() throws Exception {
        Object result = interpretAndCompile("&obj.getMap()");
        assertTrue(result instanceof Map);
        Map<String, Integer> map = (Map<String, Integer>) result;
        assertEquals(1, map.get("key1"));
        assertEquals(2, map.get("key2"));
    }

    @Test
    public void testMethodReturningArray() throws Exception {
        Object result = interpretAndCompile("&obj.getArray()");
        assertTrue(result instanceof String[]);
        String[] arr = (String[]) result;
        assertEquals(3, arr.length);
    }

    @Test
    public void testMethodReturningPrimitiveArray() throws Exception {
        Object result = interpretAndCompile("&obj.getPrimitiveArray()");
        assertTrue(result instanceof int[]);
        int[] arr = (int[]) result;
        assertEquals(5, arr.length);
        assertEquals(1, arr[0]);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodReturningEmptyList() throws Exception {
        Object result = interpretAndCompile("&obj.getEmptyList()");
        assertTrue(result instanceof List);
        assertEquals(0, ((List<?>) result).size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodReturningEmptyMap() throws Exception {
        Object result = interpretAndCompile("&obj.getEmptyMap()");
        assertTrue(result instanceof Map);
        assertEquals(0, ((Map<?, ?>) result).size());
    }

    // ========== 有参方法 ==========

    @Test
    public void testMethodWithStringArgs() throws Exception {
        assertEquals("helloworld", interpretAndCompile("&obj.concat('hello', 'world')"));
    }

    @Test
    public void testMethodWithIntArgs() throws Exception {
        assertEquals(30, interpretAndCompile("&obj.add(10, 20)"));
    }

    @Test
    public void testMethodWithBooleanArg() throws Exception {
        assertEquals("boolean:true", interpretAndCompile("&obj.processBoolean(true)"));
        assertEquals("boolean:false", interpretAndCompile("&obj.processBoolean(false)"));
    }

    @Test
    public void testMethodWithDoubleArg() throws Exception {
        assertEquals("double:3.14", interpretAndCompile("&obj.processDouble(3.14)"));
    }

    @Test
    public void testMethodWithLongArg() throws Exception {
        assertEquals("long:9999999999", interpretAndCompile("&obj.processLong(9999999999)"));
    }

    @Test
    public void testMethodWithNullArg() throws Exception {
        assertEquals("null-check:is-null", interpretAndCompile("&obj.processNull(null)"));
    }

    @Test
    public void testMethodWithNonNullArg() throws Exception {
        assertEquals("null-check:not-null", interpretAndCompile("&obj.processNull('value')"));
    }

    // ========== 参数数量边界 ==========

    @Test
    public void testZeroArgs() throws Exception {
        assertEquals("no-args", interpretAndCompile("&obj.noArgs()"));
    }

    @Test
    public void testOneArg() throws Exception {
        assertEquals("one:a", interpretAndCompile("&obj.oneArg('a')"));
    }

    @Test
    public void testTwoArgs() throws Exception {
        assertEquals("two:a,b", interpretAndCompile("&obj.twoArgs('a', 'b')"));
    }

    @Test
    public void testThreeArgs() throws Exception {
        assertEquals("three:a,b,c", interpretAndCompile("&obj.threeArgs('a', 'b', 'c')"));
    }

    @Test
    public void testFiveArgs() throws Exception {
        assertEquals("five:a,b,c,d,e", interpretAndCompile("&obj.fiveArgs('a', 'b', 'c', 'd', 'e')"));
    }

    @Test
    public void testTenArgs() throws Exception {
        assertEquals("ten:abcdefghij", 
            interpretAndCompile("&obj.tenArgs('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j')"));
    }

    // ========== 混合参数类型 ==========

    @Test
    public void testMixedArgs() throws Exception {
        assertEquals("hello:42:true:3.14", interpretAndCompile("&obj.mixedArgs('hello', 42, true, 3.14)"));
    }

    // ========== 重载方法 ==========

    @Test
    public void testOverloadNoArgs() throws Exception {
        assertEquals("overload:0", interpretAndCompile("&obj.overload()"));
    }

    @Test
    public void testOverloadOneInt() throws Exception {
        assertEquals("overload:1:42", interpretAndCompile("&obj.overload(42)"));
    }

    @Test
    public void testOverloadOneString() throws Exception {
        assertEquals("overload:1:hello", interpretAndCompile("&obj.overload('hello')"));
    }

    @Test
    public void testOverloadTwoInts() throws Exception {
        assertEquals("overload:2:1,2", interpretAndCompile("&obj.overload(1, 2)"));
    }

    @Test
    public void testOverloadStringInt() throws Exception {
        assertEquals("overload:2:a:1", interpretAndCompile("&obj.overload('a', 1)"));
    }

    @Test
    public void testOverloadIntString() throws Exception {
        assertEquals("overload:2:1:b", interpretAndCompile("&obj.overload(1, 'b')"));
    }

    @Test
    public void testOverloadThreeInts() throws Exception {
        assertEquals("overload:3:1,2,3", interpretAndCompile("&obj.overload(1, 2, 3)"));
    }

    @Test
    public void testProcessOverloadString() throws Exception {
        assertEquals("string:test", interpretAndCompile("&obj.process('test')"));
    }

    @Test
    public void testProcessOverloadInt() throws Exception {
        assertEquals("int:42", interpretAndCompile("&obj.process(42)"));
    }

    @Test
    public void testProcessOverloadTwoStrings() throws Exception {
        assertEquals("concat:ab", interpretAndCompile("&obj.process('a', 'b')"));
    }

    // ========== 方法调用作为表达式 ==========

    @Test
    public void testMethodCallInArithmetic() throws Exception {
        assertEquals(200, interpretAndCompile("&obj.getNumber() + &obj.getNumber()"));
    }

    @Test
    public void testMethodCallAsCondition() throws Exception {
        assertEquals("yes", interpretAndCompile("if &obj.isEnabled() { 'yes' } else { 'no' }"));
    }

    @Test
    public void testMethodCallAsFunctionArgument() throws Exception {
        // 方法返回值作为另一个方法的参数
        assertEquals("test-objecttest-object", interpretAndCompile("&obj.concat(&obj.getName(), &obj.getName())"));
    }

    @Test
    public void testMethodCallInLoop() throws Exception {
        String source = 
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 3) { " +
            "  sum = &sum + &obj.add(1, 1); " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        assertEquals(6, interpretAndCompile(source)); // 2 * 3
    }

    // ========== 方法调用后续操作 ==========

    @Test
    public void testMethodResultWithExtensionFunction() throws Exception {
        Object result = interpretAndCompile("&obj.getName()::split('-')");
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertEquals("test", list.get(0));
        assertEquals("object", list.get(1));
    }

    @Test
    public void testMethodResultChainedExtension() throws Exception {
        assertEquals(2, interpretAndCompile("&obj.getName()::split('-')::size()"));
    }
}
