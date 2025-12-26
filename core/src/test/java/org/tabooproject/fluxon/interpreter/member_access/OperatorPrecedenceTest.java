package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.type.TestObject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 操作符优先级和解析测试
 * 测试 . 操作符与其他操作符的优先级和组合解析
 *
 * @author sky
 */
public class OperatorPrecedenceTest extends MemberAccessTestBase {

    // ========== . 和 :: 操作符优先级 ==========

    @Test
    public void testDotHigherThanContextCall() {
        // . (115) 优先级高于 :: (110)
        // obj.method()::func() 应解析为 (obj.method())::func()
        parse("obj.method()::contains(1)");
    }

    @Test
    public void testDotThenContextCallExecution() {
        // 反射获取值，然后使用 :: 调用扩展函数
        Object result = interpret("&obj.publicField::split('-')");
        assertTrue(result instanceof java.util.List);
        assertEquals(2, ((java.util.List<?>) result).size());
    }

    // ========== . 和算术运算符 ==========

    @Test
    public void testDotWithAddition() {
        assertEquals(84, interpret("&obj.intField + &obj.intField"));
    }

    @Test
    public void testDotWithSubtraction() {
        assertEquals(0, interpret("&obj.intField - &obj.intField"));
    }

    @Test
    public void testDotWithMultiplication() {
        assertEquals(1764, interpret("&obj.intField * &obj.intField")); // 42 * 42
    }

    @Test
    public void testDotWithDivision() {
        assertEquals(1, interpret("&obj.intField / &obj.intField"));
    }

    @Test
    public void testDotWithModulo() {
        assertEquals(0, interpret("&obj.intField % &obj.intField"));
    }

    // ========== . 和比较运算符 ==========

    @Test
    public void testDotWithEquals() {
        assertEquals(true, interpret("&obj.intField == 42"));
    }

    @Test
    public void testDotWithNotEquals() {
        assertEquals(true, interpret("&obj.intField != 0"));
    }

    @Test
    public void testDotWithLessThan() {
        assertEquals(false, interpret("&obj.intField < 10"));
    }

    @Test
    public void testDotWithGreaterThan() {
        assertEquals(true, interpret("&obj.intField > 10"));
    }

    @Test
    public void testDotWithLessOrEqual() {
        assertEquals(true, interpret("&obj.intField <= 42"));
    }

    @Test
    public void testDotWithGreaterOrEqual() {
        assertEquals(true, interpret("&obj.intField >= 42"));
    }

    // ========== . 和逻辑运算符 ==========

    @Test
    public void testDotWithLogicalAnd() {
        assertEquals(true, interpret("&obj.booleanField && true"));
        assertEquals(false, interpret("&obj.booleanField && false"));
    }

    @Test
    public void testDotWithLogicalOr() {
        assertEquals(true, interpret("&obj.booleanField || false"));
    }

    @Test
    public void testDotWithLogicalNot() {
        assertEquals(false, interpret("!&obj.booleanField"));
    }

    // ========== . 和范围操作符 ==========

    @Test
    public void testDotNotConfusedWithRange() throws Exception {
        // 确保 . 和 .. 不混淆
        // 解析并执行验证
        interpretAndCompile("&obj.publicField; 1..10");
    }

    @Test
    public void testRangeAfterDotResult() {
        // 使用字段值作为范围边界
        String source = "x = &obj.intField; 1..5";
        Object result = interpret(source);
        assertTrue(result instanceof java.util.List);
        assertEquals(5, ((java.util.List<?>) result).size());
    }

    // ========== . 在条件表达式中 ==========

    @Test
    public void testDotInIfCondition() {
        assertEquals("yes", interpret("if &obj.booleanField { 'yes' } else { 'no' }"));
    }

    @Test
    public void testDotInWhileCondition() {
        String source = 
            "count = 0; " +
            "i = 0; " +
            "while (&i < 3) { " +
            "  count = &count + 1; " +
            "  i = &i + 1; " +
            "}; " +
            "&count";
        assertEquals(3, interpret(source));
    }

    // ========== . 在函数调用中 ==========

    @Test
    public void testDotResultAsArgument() {
        // 字段值作为方法参数
        assertEquals("public-value-suffix", interpret("&obj.concat(&obj.publicField, '-suffix')"));
    }

    @Test
    public void testDotResultAsMultipleArguments() {
        // 多个字段值作为参数
        assertEquals("public-valuepublic-value", interpret("&obj.concat(&obj.publicField, &obj.publicField)"));
    }

    @Test
    public void testMethodResultAsArgument() {
        // 方法返回值作为另一个方法的参数
        assertEquals("test-objecttest-object", interpret("&obj.concat(&obj.getName(), &obj.getName())"));
    }

    // ========== 复合表达式 ==========

    @Test
    public void testComplexExpression() {
        // 复杂表达式：算术 + 字段访问 + 方法调用
        assertEquals(142, interpret("&obj.intField + &obj.getNumber()")); // 42 + 100
    }

    @Test
    public void testNestedArithmetic() {
        assertEquals(168, interpret("(&obj.intField + &obj.intField) * 2")); // (42 + 42) * 2
    }

    @Test
    public void testChainedWithArithmetic() {
        assertEquals(84, interpret("&obj.getSelf().intField + &obj.intField"));
    }

    // ========== 括号优先级 ==========

    @Test
    public void testParenthesesWithDot() {
        // 括号应该正常工作
        assertEquals(84, interpret("(&obj.intField) + (&obj.intField)"));
    }

    @Test
    public void testParenthesesAroundChain() {
        assertEquals("test-object", interpret("(&obj.getSelf()).getName()"));
    }

    // ========== 字符串连接 ==========

    @Test
    public void testDotWithStringConcat() {
        assertEquals("value:42", interpret("'value:' + &obj.intField"));
    }

    @Test
    public void testMultipleFieldsInStringConcat() {
        assertEquals("public-value-42", interpret("&obj.publicField + '-' + &obj.intField"));
    }

    // ========== 编译模式优先级测试 ==========

    @Test
    public void testCompiledDotWithArithmetic() throws Exception {
        assertEquals(84, compile("&obj.intField + &obj.intField"));
    }

    @Test
    public void testCompiledDotWithComparison() throws Exception {
        assertEquals(true, compile("&obj.intField > 10"));
    }

    @Test
    public void testCompiledDotWithLogical() throws Exception {
        assertEquals(true, compile("&obj.booleanField && true"));
    }

    @Test
    public void testCompiledComplexExpression() throws Exception {
        assertEquals(142, compile("&obj.intField + &obj.getNumber()"));
    }

    // ========== 一致性测试 ==========

    @Test
    public void testPrecedenceConsistency() throws Exception {
        String[] sources = {
            "&obj.intField + 10",
            "&obj.intField * 2",
            "&obj.intField > 0",
            "&obj.booleanField && true",
            "&obj.publicField + '-suffix'"
        };

        for (String source : sources) {
            Object interpretResult = interpret(source);
            Object compileResult = compile(source);
            assertEquals(interpretResult, compileResult, "Mismatch for: " + source);
        }
    }
}
