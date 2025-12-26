package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.type.TestObject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 可变参数方法调用测试
 *
 * @author sky
 */
public class VarargsTest extends MemberAccessTestBase {

    // ========== 字符串可变参数 ==========

    @Test
    public void testVarargsNoArgs() {
        assertEquals("varargs:0", interpret("&obj.varargs()"));
    }

    @Test
    public void testVarargsOneArg() {
        assertEquals("varargs:1", interpret("&obj.varargs('a')"));
    }

    @Test
    public void testVarargsTwoArgs() {
        assertEquals("varargs:2", interpret("&obj.varargs('a', 'b')"));
    }

    @Test
    public void testVarargsThreeArgs() {
        assertEquals("varargs:3", interpret("&obj.varargs('a', 'b', 'c')"));
    }

    @Test
    public void testVarargsManyArgs() {
        assertEquals("varargs:5", interpret("&obj.varargs('a', 'b', 'c', 'd', 'e')"));
    }

    // ========== 数值可变参数 ==========

    @Test
    public void testVarargsSumNoArgs() {
        assertEquals(0, interpret("&obj.varargsSum()"));
    }

    @Test
    public void testVarargsSumOneArg() {
        assertEquals(10, interpret("&obj.varargsSum(10)"));
    }

    @Test
    public void testVarargsSumTwoArgs() {
        assertEquals(30, interpret("&obj.varargsSum(10, 20)"));
    }

    @Test
    public void testVarargsSumThreeArgs() {
        assertEquals(60, interpret("&obj.varargsSum(10, 20, 30)"));
    }

    @Test
    public void testVarargsSumManyArgs() {
        assertEquals(150, interpret("&obj.varargsSum(10, 20, 30, 40, 50)"));
    }

    // ========== 传入数组参数 ==========

    @Test
    public void testVarargsWithArrayArgument() throws Exception {
        Object result = interpretAndCompile("&obj.varargs(&obj.getArray())");
        assertEquals("varargs:3", result);
    }

    @Test
    public void testVarargsSumWithArrayArgument() throws Exception {
        Object result = interpretAndCompile("&obj.varargsSum(&obj.getPrimitiveArray())");
        assertEquals(15, result);
    }

    // ========== 编译模式可变参数 ==========

    @Test
    public void testCompiledVarargsNoArgs() throws Exception {
        assertEquals("varargs:0", compile("&obj.varargs()"));
    }

    @Test
    public void testCompiledVarargsMultipleArgs() throws Exception {
        assertEquals("varargs:3", compile("&obj.varargs('x', 'y', 'z')"));
    }

    @Test
    public void testCompiledVarargsSumMultiple() throws Exception {
        assertEquals(100, compile("&obj.varargsSum(10, 20, 30, 40)"));
    }

    // ========== 一致性测试 ==========

    @Test
    public void testVarargsConsistencyNoArgs() throws Exception {
        String source = "&obj.varargs()";
        assertEquals(interpret(source), compile(source));
    }

    @Test
    public void testVarargsConsistencyWithArgs() throws Exception {
        String source = "&obj.varargs('a', 'b', 'c')";
        assertEquals(interpret(source), compile(source));
    }

    @Test
    public void testVarargsSumConsistency() throws Exception {
        String source = "&obj.varargsSum(1, 2, 3, 4, 5)";
        assertEquals(interpret(source), compile(source));
    }

    // ========== 可变参数与链式调用 ==========

    @Test
    public void testVarargsChained() {
        assertEquals("varargs:2", interpret("&obj.getSelf().varargs('a', 'b')"));
    }

    @Test
    public void testVarargsSumChained() {
        assertEquals(60, interpret("&obj.getSelf().varargsSum(10, 20, 30)"));
    }

    // ========== 可变参数在循环中 ==========

    @Test
    public void testVarargsInLoop() {
        String source =
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 3) { " +
            "  sum = &sum + &obj.varargsSum(1, 2, 3); " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        assertEquals(18, interpret(source)); // 6 * 3
    }

    // ========== 可变参数作为表达式 ==========

    @Test
    public void testVarargsSumInArithmetic() {
        assertEquals(21, interpret("&obj.varargsSum(1, 2, 3) + &obj.varargsSum(4, 5, 6)"));
    }

    @Test
    public void testVarargsAsCondition() {
        assertEquals("yes", interpret("if &obj.varargsSum(1) > 0 { 'yes' } else { 'no' }"));
    }
}
