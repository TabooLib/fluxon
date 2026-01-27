package org.tabooproject.fluxon.optimizer;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil.TestResult;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;
import org.tabooproject.fluxon.parser.expression.UnaryExpression;
import org.tabooproject.fluxon.parser.expression.literal.BooleanLiteral;
import org.tabooproject.fluxon.parser.expression.literal.DoubleLiteral;
import org.tabooproject.fluxon.parser.expression.literal.FloatLiteral;
import org.tabooproject.fluxon.parser.expression.literal.IntLiteral;
import org.tabooproject.fluxon.parser.expression.literal.LongLiteral;
import org.tabooproject.fluxon.parser.expression.literal.StringLiteral;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.FluxonTestUtil.*;

/**
 * 常量折叠优化器测试
 *
 * @author sky
 */
public class ConstantFolderTest {

    private final ConstantFolder folder = new ConstantFolder();

    // ==================== 整数运算 ====================

    @Test
    public void testIntAddition() {
        BinaryExpression expr = binary(intLit(1), TokenType.PLUS, intLit(2));
        ParseResult result = folder.fold(expr);
        assertIntLiteral(3, result);
    }

    @Test
    public void testIntSubtraction() {
        BinaryExpression expr = binary(intLit(5), TokenType.MINUS, intLit(3));
        ParseResult result = folder.fold(expr);
        assertIntLiteral(2, result);
    }

    @Test
    public void testIntMultiplication() {
        BinaryExpression expr = binary(intLit(4), TokenType.MULTIPLY, intLit(5));
        ParseResult result = folder.fold(expr);
        assertIntLiteral(20, result);
    }

    @Test
    public void testIntDivision() {
        BinaryExpression expr = binary(intLit(10), TokenType.DIVIDE, intLit(3));
        ParseResult result = folder.fold(expr);
        assertIntLiteral(3, result);
    }

    @Test
    public void testIntModulo() {
        BinaryExpression expr = binary(intLit(10), TokenType.MODULO, intLit(3));
        ParseResult result = folder.fold(expr);
        assertIntLiteral(1, result);
    }

    // ==================== 嵌套表达式 ====================

    @Test
    public void testNestedExpression() {
        // (1 + 2) * 3 = 9
        BinaryExpression inner = binary(intLit(1), TokenType.PLUS, intLit(2));
        BinaryExpression expr = binary(inner, TokenType.MULTIPLY, intLit(3));
        ParseResult result = folder.fold(expr);
        assertIntLiteral(9, result);
    }

    @Test
    public void testDeeplyNested() {
        // ((2 + 3) * (4 - 1)) = 5 * 3 = 15
        BinaryExpression left = binary(intLit(2), TokenType.PLUS, intLit(3));
        BinaryExpression right = binary(intLit(4), TokenType.MINUS, intLit(1));
        BinaryExpression expr = binary(left, TokenType.MULTIPLY, right);
        ParseResult result = folder.fold(expr);
        assertIntLiteral(15, result);
    }

    // ==================== 幂运算 ====================

    @Test
    public void testPower() {
        BinaryExpression expr = binary(intLit(2), TokenType.POWER, intLit(3));
        ParseResult result = folder.fold(expr);
        assertDoubleLiteral(8.0, result);
    }

    @Test
    public void testPowerWithDouble() {
        BinaryExpression expr = binary(doubleLit(2.0), TokenType.POWER, doubleLit(0.5));
        ParseResult result = folder.fold(expr);
        assertDoubleLiteral(Math.sqrt(2.0), result);
    }

    // ==================== 一元运算 ====================

    @Test
    public void testUnaryNegation() {
        UnaryExpression expr = unary(TokenType.MINUS, intLit(5));
        ParseResult result = folder.fold(expr);
        assertIntLiteral(-5, result);
    }

    @Test
    public void testDoubleNegation() {
        // --3 = 3
        UnaryExpression inner = unary(TokenType.MINUS, intLit(3));
        UnaryExpression expr = unary(TokenType.MINUS, inner);
        ParseResult result = folder.fold(expr);
        assertIntLiteral(3, result);
    }

    @Test
    public void testUnaryNot() {
        UnaryExpression expr = unary(TokenType.NOT, boolLit(true));
        ParseResult result = folder.fold(expr);
        assertBooleanLiteral(false, result);
    }

    @Test
    public void testDoubleNot() {
        // !!true = true
        UnaryExpression inner = unary(TokenType.NOT, boolLit(true));
        UnaryExpression expr = unary(TokenType.NOT, inner);
        ParseResult result = folder.fold(expr);
        assertBooleanLiteral(true, result);
    }

    // ==================== 字符串拼接 ====================

    @Test
    public void testStringConcat() {
        BinaryExpression expr = binary(strLit("Hello"), TokenType.PLUS, strLit(" World"));
        ParseResult result = folder.fold(expr);
        assertStringLiteral("Hello World", result);
    }

    @Test
    public void testMultipleStringConcat() {
        // "a" + "b" + "c"
        BinaryExpression left = binary(strLit("a"), TokenType.PLUS, strLit("b"));
        BinaryExpression expr = binary(left, TokenType.PLUS, strLit("c"));
        ParseResult result = folder.fold(expr);
        assertStringLiteral("abc", result);
    }

    // ==================== 类型提升 ====================

    @Test
    public void testIntPlusDouble() {
        BinaryExpression expr = binary(intLit(1), TokenType.PLUS, doubleLit(2.5));
        ParseResult result = folder.fold(expr);
        assertDoubleLiteral(3.5, result);
    }

    @Test
    public void testIntPlusLong() {
        BinaryExpression expr = binary(intLit(1), TokenType.PLUS, longLit(2L));
        ParseResult result = folder.fold(expr);
        assertLongLiteral(3L, result);
    }

    @Test
    public void testFloatPlusDouble() {
        BinaryExpression expr = binary(floatLit(1.5f), TokenType.PLUS, doubleLit(2.5));
        ParseResult result = folder.fold(expr);
        assertDoubleLiteral(4.0, result);
    }

    // ==================== 比较运算 ====================

    @Test
    public void testGreater() {
        BinaryExpression expr = binary(intLit(5), TokenType.GREATER, intLit(3));
        ParseResult result = folder.fold(expr);
        assertBooleanLiteral(true, result);
    }

    @Test
    public void testLessEqual() {
        BinaryExpression expr = binary(intLit(3), TokenType.LESS_EQUAL, intLit(3));
        ParseResult result = folder.fold(expr);
        assertBooleanLiteral(true, result);
    }

    @Test
    public void testEqual() {
        BinaryExpression expr = binary(intLit(5), TokenType.EQUAL, intLit(5));
        ParseResult result = folder.fold(expr);
        assertBooleanLiteral(true, result);
    }

    @Test
    public void testNotEqual() {
        BinaryExpression expr = binary(intLit(5), TokenType.NOT_EQUAL, intLit(3));
        ParseResult result = folder.fold(expr);
        assertBooleanLiteral(true, result);
    }

    // ==================== 除零处理 ====================

    @Test
    public void testIntDivByZeroNotFolded() {
        BinaryExpression expr = binary(intLit(1), TokenType.DIVIDE, intLit(0));
        ParseResult result = folder.fold(expr);
        // 整数除零不折叠，保持原表达式
        assertSame(expr, result);
    }

    @Test
    public void testLongDivByZeroNotFolded() {
        BinaryExpression expr = binary(longLit(1L), TokenType.DIVIDE, longLit(0L));
        ParseResult result = folder.fold(expr);
        assertSame(expr, result);
    }

    @Test
    public void testDoubleDivByZeroFolded() {
        // 浮点除零可以折叠为 Infinity
        BinaryExpression expr = binary(doubleLit(1.0), TokenType.DIVIDE, doubleLit(0.0));
        ParseResult result = folder.fold(expr);
        assertDoubleLiteral(Double.POSITIVE_INFINITY, result);
    }

    // ==================== 集成测试 ====================

    @Test
    public void testIntegrationSimple() {
        // 使用 Fluxon 执行器验证结果一致性
        TestResult result = runSilent("1 + 2");
        assertBothEqual(3, result);
    }

    @Test
    public void testIntegrationNested() {
        TestResult result = runSilent("(1 + 2) * 3");
        assertBothEqual(9, result);
    }

    @Test
    public void testIntegrationPower() {
        TestResult result = runSilent("2 ^ 3");
        assertBothEqual(8.0, result);
    }

    @Test
    public void testIntegrationMixedTypes() {
        TestResult result = runSilent("1 + 2.0");
        assertBothEqual(3.0, result);
    }

    @Test
    public void testIntegrationStringConcat() {
        TestResult result = runSilent("\"Hello\" + \" \" + \"World\"");
        assertBothEqual("Hello World", result);
    }

    @Test
    public void testIntegrationComparison() {
        TestResult result = runSilent("5 > 3");
        assertBothEqual(true, result);
    }

    @Test
    public void testIntegrationUnary() {
        TestResult result = runSilent("-5");
        assertBothEqual(-5, result);
    }

    @Test
    public void testIntegrationDoubleNegation() {
        TestResult result = runSilent("--3");
        assertBothEqual(3, result);
    }

    @Test
    public void testIntegrationComplexExpression() {
        // 复杂表达式（不使用赋值）
        String expr = "6.5*7.8^2.3 + (3.5^3+7/2)^3 -(5*4/(2-3))*4";
        TestResult result = runSilent(expr);
        // 验证解释执行和编译执行结果一致
        assertMatch(result);
        assertNotNull(result.getInterpretResult());
    }

    @Test
    public void testIntegrationAssignmentWithConstantFolding() {
        // 赋值语句中的常量折叠
        String expr = "eval = 1 + 2 * 3; &eval";
        TestResult result = runSilent(expr);
        assertBothEqual(7, result);
    }

    @Test
    public void testIntegrationRepeatedComplexExpression() {
        // 重复的复杂表达式
        String expr = "6.5*7.8^2.3 + (3.5^3+7/2)^3 -(5*4/(2-3))*4 + " +
                      "6.5*7.8^2.3 + (3.5^3+7/2)^3 -(5*4/(2-3))*4";
        TestResult result = runSilent(expr);
        assertMatch(result);
        assertNotNull(result.getInterpretResult());
    }

    // ==================== 辅助方法 ====================

    private static IntLiteral intLit(int value) {
        return new IntLiteral(value);
    }

    private static LongLiteral longLit(long value) {
        return new LongLiteral(value);
    }

    private static FloatLiteral floatLit(float value) {
        return new FloatLiteral(value);
    }

    private static DoubleLiteral doubleLit(double value) {
        return new DoubleLiteral(value);
    }

    private static BooleanLiteral boolLit(boolean value) {
        return new BooleanLiteral(value);
    }

    private static StringLiteral strLit(String value) {
        return new StringLiteral(value);
    }

    private static Token token(TokenType type) {
        return new Token(type);
    }

    private static BinaryExpression binary(ParseResult left, TokenType op, ParseResult right) {
        return new BinaryExpression(left, token(op), right);
    }

    private static UnaryExpression unary(TokenType op, ParseResult operand) {
        return new UnaryExpression(token(op), operand);
    }

    private static void assertIntLiteral(int expected, ParseResult result) {
        assertInstanceOf(IntLiteral.class, result);
        assertEquals(expected, ((IntLiteral) result).getValue());
    }

    private static void assertLongLiteral(long expected, ParseResult result) {
        assertInstanceOf(LongLiteral.class, result);
        assertEquals(expected, ((LongLiteral) result).getValue());
    }

    private static void assertFloatLiteral(float expected, ParseResult result) {
        assertInstanceOf(FloatLiteral.class, result);
        assertEquals(expected, ((FloatLiteral) result).getValue(), 0.0001f);
    }

    private static void assertDoubleLiteral(double expected, ParseResult result) {
        assertInstanceOf(DoubleLiteral.class, result);
        assertEquals(expected, ((DoubleLiteral) result).getValue(), 0.0001);
    }

    private static void assertBooleanLiteral(boolean expected, ParseResult result) {
        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(expected, ((BooleanLiteral) result).getValue());
    }

    private static void assertStringLiteral(String expected, ParseResult result) {
        assertInstanceOf(StringLiteral.class, result);
        assertEquals(expected, ((StringLiteral) result).getValue());
    }
}
