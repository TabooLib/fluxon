package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.type.TestObject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 特殊值和边界场景测试
 * 测试特殊字符、空字符串、极值等边界情况
 *
 * @author sky
 */
public class SpecialValuesTest extends MemberAccessTestBase {

    // ========== 空字符串 ==========

    @Test
    public void testEmptyStringArgument() {
        assertEquals("one:", interpret("&obj.oneArg('')"));
    }

    @Test
    public void testEmptyStringConcat() {
        assertEquals("hello", interpret("&obj.concat('hello', '')"));
        assertEquals("world", interpret("&obj.concat('', 'world')"));
        assertEquals("", interpret("&obj.concat('', '')"));
    }

    @Test
    public void testEmptyStringField() {
        TestObject obj = new TestObject();
        obj.publicField = "";
        assertEquals("", interpret("&obj.publicField", obj));
    }

    // ========== 特殊字符 ==========

    @Test
    public void testSpecialCharactersInString() {
        assertEquals("hello\tworld", interpret("&obj.echoSpecial('hello\tworld')"));
    }

    @Test
    public void testNewlineInString() {
        assertEquals("line1\nline2", interpret("&obj.echoSpecial('line1\nline2')"));
    }

    @Test
    public void testUnicodeCharacters() {
        assertEquals("你好世界", interpret("&obj.echoSpecial('你好世界')"));
    }

    @Test
    public void testEmojiCharacters() {
        assertEquals("Hello 🎉", interpret("&obj.echoSpecial('Hello 🎉')"));
    }

    @Test
    public void testQuotesInString() {
        // 单引号字符串中包含转义的单引号
        assertEquals("it's", interpret("&obj.echoSpecial('it\\'s')"));
    }

    // ========== 数值边界 ==========

    @Test
    public void testZeroValue() {
        assertEquals(0, interpret("&obj.add(0, 0)"));
    }

    @Test
    public void testNegativeValues() {
        assertEquals(-30, interpret("&obj.add(-10, -20)"));
    }

    @Test
    public void testMixedSignValues() {
        assertEquals(10, interpret("&obj.add(30, -20)"));
        assertEquals(-10, interpret("&obj.add(-30, 20)"));
    }

    @Test
    public void testIntMaxValue() {
        assertEquals(2147483647, interpret("&obj.add(2147483646, 1)"));
    }

    @Test
    public void testIntMinValue() {
        assertEquals(-2147483647, interpret("&obj.add(-2147483646, -1)"));
    }

    // ========== 布尔值 ==========

    @Test
    public void testBooleanTrue() {
        assertEquals("boolean:true", interpret("&obj.processBoolean(true)"));
    }

    @Test
    public void testBooleanFalse() {
        assertEquals("boolean:false", interpret("&obj.processBoolean(false)"));
    }

    @Test
    public void testBooleanFieldTrue() {
        TestObject obj = new TestObject();
        obj.booleanField = true;
        assertEquals(true, interpret("&obj.booleanField", obj));
    }

    @Test
    public void testBooleanFieldFalse() {
        TestObject obj = new TestObject();
        obj.booleanField = false;
        assertEquals(false, interpret("&obj.booleanField", obj));
    }

    // ========== 浮点数边界 ==========

    @Test
    public void testDoubleZero() {
        assertEquals("double:0.0", interpret("&obj.processDouble(0.0)"));
    }

    @Test
    public void testNegativeDouble() {
        assertEquals("double:-3.14", interpret("&obj.processDouble(-3.14)"));
    }

    @Test
    public void testVerySmallDouble() {
        assertEquals("double:1.0E-4", interpret("&obj.processDouble(0.0001)"));
    }

    @Test
    public void testVeryLargeDouble() {
        assertEquals("double:1.0E10", interpret("&obj.processDouble(10000000000.0)"));
    }

    // ========== 多个相同调用 ==========

    @Test
    public void testRepeatedMethodCall() {
        String source = 
            "a = &obj.add(1, 1); " +
            "b = &obj.add(1, 1); " +
            "c = &obj.add(1, 1); " +
            "&a + &b + &c";
        assertEquals(6, interpret(source));
    }

    @Test
    public void testRepeatedFieldAccess() {
        String source = 
            "a = &obj.intField; " +
            "b = &obj.intField; " +
            "c = &obj.intField; " +
            "&a + &b + &c";
        assertEquals(126, interpret(source)); // 42 * 3
    }

    // ========== 表达式中的成员访问 ==========

    @Test
    public void testMemberAccessInCondition() {
        assertEquals("yes", interpret("if &obj.booleanField { 'yes' } else { 'no' }"));
    }

    @Test
    public void testMemberAccessInComparison() {
        assertEquals(true, interpret("&obj.intField > 40"));
        assertEquals(false, interpret("&obj.intField < 40"));
        assertEquals(true, interpret("&obj.intField == 42"));
    }

    @Test
    public void testMemberAccessInArithmetic() {
        assertEquals(84, interpret("&obj.intField * 2"));
        assertEquals(21, interpret("&obj.intField / 2"));
        assertEquals(44, interpret("&obj.intField + 2"));
        assertEquals(40, interpret("&obj.intField - 2"));
    }

    // ========== 返回值作为后续操作的输入 ==========

    @Test
    public void testMethodResultAsMethodArg() {
        assertEquals("test-objecttest-object", interpret("&obj.concat(&obj.getName(), &obj.getName())"));
    }

    @Test
    public void testFieldAsMethodArg() {
        assertEquals("public-value-suffix", interpret("&obj.concat(&obj.publicField, '-suffix')"));
    }

    @Test
    public void testChainResultAsArg() {
        assertEquals("public-valuepublic-value", 
            interpret("&obj.concat(&obj.getSelf().publicField, &obj.publicField)"));
    }

    // ========== 编译模式边界测试 ==========

    @Test
    public void testCompiledEmptyString() throws Exception {
        assertEquals("", compile("&obj.concat('', '')"));
    }

    @Test
    public void testCompiledNegativeValue() throws Exception {
        assertEquals(-30, compile("&obj.add(-10, -20)"));
    }

    @Test
    public void testCompiledBooleanCondition() throws Exception {
        assertEquals("yes", compile("if &obj.booleanField { 'yes' } else { 'no' }"));
    }

    @Test
    public void testCompiledRepeatedCalls() throws Exception {
        String source = 
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 10) { " +
            "  sum = &sum + &obj.add(1, 1); " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        assertEquals(20, compile(source)); // 2 * 10
    }
}
