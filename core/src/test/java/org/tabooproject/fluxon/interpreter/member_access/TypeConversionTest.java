package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 类型转换测试
 * 测试反射调用中的参数类型转换和匹配
 *
 * @author sky
 */
public class TypeConversionTest extends MemberAccessTestBase {

    // ========== 基本类型参数 ==========

    @Test
    public void testIntParameter() {
        assertEquals(30, interpret("&obj.add(10, 20)"));
    }

    @Test
    public void testBooleanParameter() {
        assertEquals("boolean:true", interpret("&obj.processBoolean(true)"));
        assertEquals("boolean:false", interpret("&obj.processBoolean(false)"));
    }

    @Test
    public void testDoubleParameter() {
        assertEquals("double:3.14", interpret("&obj.processDouble(3.14)"));
    }

    @Test
    public void testLongParameter() {
        assertEquals("long:9999999999", interpret("&obj.processLong(9999999999)"));
    }

    // ========== 数值类型自动转换 ==========

    @Test
    public void testIntToDoubleConversion() {
        // int 参数传给 double 形参
        assertEquals("double:42.0", interpret("&obj.intToDouble(42)"));
    }

    @Test
    public void testIntToLongConversion() {
        // int 参数传给 long 形参
        assertEquals("long:100", interpret("&obj.processLong(100)"));
    }

    // ========== 包装类型 ==========

    @Test
    public void testBoxedInteger() {
        assertEquals("Integer:42", interpret("&obj.intValue(42)"));
    }

    @Test
    public void testBoxedDouble() {
        assertEquals("Double:3.14", interpret("&obj.doubleValue(3.14)"));
    }

    // ========== null 参数 ==========

    @Test
    public void testNullStringParameter() {
        assertEquals("null-check:is-null", interpret("&obj.processNull(null)"));
    }

    @Test
    public void testNonNullStringParameter() {
        assertEquals("null-check:not-null", interpret("&obj.processNull('value')"));
    }

    @Test
    public void testObjectWithNull() {
        assertEquals("object:null", interpret("&obj.processObject(null)"));
    }

    @Test
    public void testObjectWithString() {
        assertEquals("object:String", interpret("&obj.processObject('hello')"));
    }

    @Test
    public void testObjectWithInt() {
        assertEquals("object:Integer", interpret("&obj.processObject(42)"));
    }

    // ========== 混合参数类型 ==========

    @Test
    public void testMixedArgs() {
        assertEquals("hello:42:true:3.14", interpret("&obj.mixedArgs('hello', 42, true, 3.14)"));
    }

    // ========== 集合类型参数 ==========

    @Test
    public void testCollectionParameter() {
        assertEquals("collection:3", interpret("&obj.processCollection([1, 2, 3])"));
    }

    @Test
    public void testEmptyCollectionParameter() {
        assertEquals("collection:0", interpret("&obj.processCollection([])"));
    }

    // ========== 重载方法的类型匹配 ==========

    @Test
    public void testOverloadIntVsString() {
        // 相同方法名，根据参数类型选择
        assertEquals("overload:1:42", interpret("&obj.overload(42)"));
        assertEquals("overload:1:hello", interpret("&obj.overload('hello')"));
    }

    @Test
    public void testOverloadTwoArgsVariants() {
        assertEquals("overload:2:10,20", interpret("&obj.overload(10, 20)"));
        assertEquals("overload:2:a:1", interpret("&obj.overload('a', 1)"));
        assertEquals("overload:2:1:b", interpret("&obj.overload(1, 'b')"));
    }

    // ========== 编译模式类型转换 ==========

    @Test
    public void testCompiledIntToDouble() throws Exception {
        assertEquals("double:100.0", compile("&obj.intToDouble(100)", "TestCompiledIntToDouble"));
    }

    @Test
    public void testCompiledMixedArgs() throws Exception {
        assertEquals("world:100:false:2.718", compile("&obj.mixedArgs('world', 100, false, 2.718)", "TestCompiledMixed"));
    }

    @Test
    public void testCompiledNullParameter() throws Exception {
        assertEquals("null-check:is-null", compile("&obj.processNull(null)", "TestCompiledNull"));
    }

    @Test
    public void testCompiledOverloadSelection() throws Exception {
        assertEquals("overload:1:99", compile("&obj.overload(99)", "TestCompiledOverloadInt"));
        assertEquals("overload:1:test", compile("&obj.overload('test')", "TestCompiledOverloadStr"));
    }

    // ========== 一致性测试 ==========

    @Test
    public void testTypeConversionConsistency() throws Exception {
        String source = "&obj.mixedArgs('test', 123, true, 9.99)";
        Object interpretResult = interpret(source);
        Object compileResult = compile(source, "TestTypeConsistency");
        assertEquals(interpretResult, compileResult);
    }

    @Test
    public void testOverloadConsistency() throws Exception {
        String[] sources = {
            "&obj.overload()",
            "&obj.overload(1)",
            "&obj.overload('x')",
            "&obj.overload(1, 2)",
            "&obj.overload('a', 1)",
            "&obj.overload(1, 'b')",
            "&obj.overload(1, 2, 3)"
        };
        
        for (int i = 0; i < sources.length; i++) {
            Object interpretResult = interpret(sources[i]);
            Object compileResult = compile(sources[i], "TestOverloadConsist" + i);
            assertEquals(interpretResult, compileResult, "Mismatch for: " + sources[i]);
        }
    }
}
