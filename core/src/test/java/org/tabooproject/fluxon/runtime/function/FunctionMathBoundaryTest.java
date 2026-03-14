package org.tabooproject.fluxon.runtime.function;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.FluxonTestUtil.TestResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.FluxonTestUtil.*;

/**
 * FunctionMath 边界测试
 * 测试边界值、特殊情况和异常处理
 *
 * @author sky
 */
public class FunctionMathBoundaryTest {

    @Test
    void testMinWithZero() {
        assertBothEqual(0, runSilent("min(0, 5)"));
        assertBothEqual(-5, runSilent("min(0, -5)"));
        assertBothEqual(0, runSilent("min(0, 0)"));
    }

    @Test
    void testMinWithNegatives() {
        assertBothEqual(-10, runSilent("min(-5, -10)"));
        assertBothEqual(-100, runSilent("min(-100, -1)"));
    }

    @Test
    void testMinWithSameValues() {
        assertBothEqual(42, runSilent("min(42, 42)"));
        assertBothEqual(3.14, runSilent("min(3.14, 3.14)"));
    }

    @Test
    void testMinIntBoundary() {
        // Integer.MAX_VALUE = 2147483647
        // Integer.MIN_VALUE = -2147483648
        assertBothEqual(2147483646, runSilent("min(2147483647, 2147483646)"));
        // 用计算表达式避免字面量解析问题
        assertBothEqual(-2147483647, runSilent("min(-2147483647, -2147483646)"));
        assertBothEqual(-2147483647, runSilent("min(-2147483647, 0)"));
    }

    @Test
    void testMaxWithZero() {
        assertBothEqual(5, runSilent("max(0, 5)"));
        assertBothEqual(0, runSilent("max(0, -5)"));
        assertBothEqual(0, runSilent("max(0, 0)"));
    }

    @Test
    void testMaxWithNegatives() {
        assertBothEqual(-5, runSilent("max(-5, -10)"));
        assertBothEqual(-1, runSilent("max(-100, -1)"));
    }

    @Test
    void testMaxIntBoundary() {
        assertBothEqual(2147483647, runSilent("max(2147483647, 2147483646)"));
        assertBothEqual(-2147483646, runSilent("max(-2147483647, -2147483646)"));
        assertBothEqual(2147483647, runSilent("max(2147483647, 0)"));
    }

    @Test
    void testMinMaxLongBoundary() {
        // Long.MAX_VALUE = 9223372036854775807L
        assertBothEqual(9223372036854775806L, runSilent("min(9223372036854775807L, 9223372036854775806L)"));
        assertBothEqual(9223372036854775807L, runSilent("max(9223372036854775807L, 9223372036854775806L)"));
    }

    @Test
    void testMinMaxDoubleSpecialValues() {
        // NaN 行为
        TestResult nanResult = runSilent("min(0.0/0.0, 1.0)");
        assertTrue(Double.isNaN((Double) nanResult.getInterpretResult()));
        // Infinity
        TestResult infResult = runSilent("min(1.0/0.0, 100.0)");
        assertEquals(100.0, infResult.getInterpretResult());
        TestResult negInfResult = runSilent("max(-1.0/0.0, -100.0)");
        assertEquals(-100.0, negInfResult.getInterpretResult());
    }

    @Test
    void testAbsZero() {
        assertBothEqual(0, runSilent("abs(0)"));
        assertBothEqual(0.0, runSilent("abs(0.0)"));
        assertBothEqual(0.0, runSilent("abs(-0.0)"));
    }

    @Test
    void testAbsPositive() {
        assertBothEqual(42, runSilent("abs(42)"));
        assertBothEqual(3.14, runSilent("abs(3.14)"));
    }

    @Test
    void testAbsNegative() {
        assertBothEqual(42, runSilent("abs(-42)"));
        assertBothEqual(3.14, runSilent("abs(-3.14)"));
    }

    @Test
    void testAbsIntBoundary() {
        assertBothEqual(2147483647, runSilent("abs(2147483647)"));
        assertBothEqual(2147483647, runSilent("abs(-2147483647)"));
    }

    @Test
    void testAbsDoubleSpecialValues() {
        TestResult infResult = runSilent("abs(-1.0/0.0)");
        assertTrue(Double.isInfinite((Double) infResult.getInterpretResult()));
        assertTrue((Double) infResult.getInterpretResult() > 0);
    }

    @Test
    void testClampInRange() {
        assertBothEqual(5, runSilent("clamp(5, 0, 10)"));
        assertBothEqual(5.5, runSilent("clamp(5.5, 0.0, 10.0)"));
    }

    @Test
    void testClampAtBoundary() {
        assertBothEqual(0, runSilent("clamp(0, 0, 10)"));
        assertBothEqual(10, runSilent("clamp(10, 0, 10)"));
    }

    @Test
    void testClampBelowMin() {
        assertBothEqual(0, runSilent("clamp(-5, 0, 10)"));
        assertBothEqual(-10, runSilent("clamp(-100, -10, 10)"));
    }

    @Test
    void testClampAboveMax() {
        assertBothEqual(10, runSilent("clamp(15, 0, 10)"));
        assertBothEqual(100, runSilent("clamp(1000, -100, 100)"));
    }

    @Test
    void testClampWithNegativeRange() {
        assertBothEqual(-5, runSilent("clamp(-5, -10, -1)"));
        assertBothEqual(-10, runSilent("clamp(-100, -10, -1)"));
        assertBothEqual(-1, runSilent("clamp(100, -10, -1)"));
    }

    @Test
    void testClampSinglePointRange() {
        // 当 min == max 时
        assertBothEqual(5, runSilent("clamp(0, 5, 5)"));
        assertBothEqual(5, runSilent("clamp(10, 5, 5)"));
        assertBothEqual(5, runSilent("clamp(5, 5, 5)"));
    }

    @Test
    void testPowZeroExponent() {
        assertBothEqual(1, runSilent("pow(5, 0)"));
        assertBothEqual(1, runSilent("pow(0, 0)")); // 0^0 = 1 (按 Math.pow 定义)
        assertBothEqual(1.0, runSilent("pow(5.0, 0.0)"));
    }

    @Test
    void testPowOneExponent() {
        assertBothEqual(5, runSilent("pow(5, 1)"));
        assertBothEqual(-3, runSilent("pow(-3, 1)"));
    }

    @Test
    void testPowZeroBase() {
        assertBothEqual(0, runSilent("pow(0, 5)"));
        assertBothEqual(0.0, runSilent("pow(0.0, 5.0)"));
    }

    @Test
    void testPowNegativeBase() {
        assertBothEqual(9, runSilent("pow(-3, 2)"));
        assertBothEqual(-27, runSilent("pow(-3, 3)"));
    }

    @Test
    void testPowNegativeExponent() {
        assertBothEqual(0.25, runSilent("pow(2.0, -2.0)"));
        assertBothEqual(0.125, runSilent("pow(2.0, -3.0)"));
    }

    @Test
    void testPowFractionalExponent() {
        // sqrt(4) = 4^0.5 = 2
        TestResult result = runSilent("pow(4.0, 0.5)");
        assertEquals(2.0, (Double) result.getInterpretResult(), 0.0001);
        // cbrt(8) = 8^(1/3) ≈ 2
        result = runSilent("pow(8.0, 1.0/3.0)");
        assertEquals(2.0, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testPowLargeValues() {
        assertBothEqual(1024, runSilent("pow(2, 10)"));
        assertBothEqual(1000000, runSilent("pow(10, 6)"));
    }

    @Test
    void testSqrtZero() {
        assertBothEqual(0.0, runSilent("sqrt(0.0)"));
    }

    @Test
    void testSqrtOne() {
        assertBothEqual(1.0, runSilent("sqrt(1.0)"));
    }

    @Test
    void testSqrtPerfectSquares() {
        assertBothEqual(2.0, runSilent("sqrt(4.0)"));
        assertBothEqual(3.0, runSilent("sqrt(9.0)"));
        assertBothEqual(10.0, runSilent("sqrt(100.0)"));
    }

    @Test
    void testSqrtNonPerfectSquares() {
        TestResult result = runSilent("sqrt(2.0)");
        assertEquals(Math.sqrt(2), (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testSqrtNegativeThrowsError() {
        runExpectingError("sqrt(-1.0)", "Cannot take square root of negative number");
    }

    @Test
    void testCbrtZero() {
        assertBothEqual(0.0, runSilent("cbrt(0.0)"));
    }

    @Test
    void testCbrtPositive() {
        assertBothEqual(2.0, runSilent("cbrt(8.0)"));
        assertBothEqual(3.0, runSilent("cbrt(27.0)"));
    }

    @Test
    void testCbrtNegative() {
        // cbrt 可以处理负数
        assertBothEqual(-2.0, runSilent("cbrt(-8.0)"));
        assertBothEqual(-3.0, runSilent("cbrt(-27.0)"));
    }

    @Test
    void testLogOne() {
        assertBothEqual(0.0, runSilent("log(1.0)"));
        assertBothEqual(0.0, runSilent("log10(1.0)"));
    }

    @Test
    void testLogE() {
        TestResult result = runSilent("log(&E)");
        assertEquals(1.0, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testLog10Ten() {
        assertBothEqual(1.0, runSilent("log10(10.0)"));
        assertBothEqual(2.0, runSilent("log10(100.0)"));
        assertBothEqual(3.0, runSilent("log10(1000.0)"));
    }

    @Test
    void testLogZeroThrowsError() {
        runExpectingError("log(0.0)", "log input must be positive");
    }

    @Test
    void testLogNegativeThrowsError() {
        runExpectingError("log(-1.0)", "log input must be positive");
    }

    @Test
    void testLog10ZeroThrowsError() {
        runExpectingError("log10(0.0)", "log10 input must be positive");
    }

    @Test
    void testLog10NegativeThrowsError() {
        runExpectingError("log10(-1.0)", "log10 input must be positive");
    }

    @Test
    void testExpZero() {
        assertBothEqual(1.0, runSilent("exp(0.0)"));
    }

    @Test
    void testExpOne() {
        TestResult result = runSilent("exp(1.0)");
        assertEquals(Math.E, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testExpNegative() {
        TestResult result = runSilent("exp(-1.0)");
        assertEquals(1.0 / Math.E, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testExpLargeValue() {
        TestResult result = runSilent("exp(10.0)");
        assertEquals(Math.exp(10), (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testSinBoundary() {
        TestResult result = runSilent("sin(0.0)");
        assertEquals(0.0, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("sin(&PI / 2.0)");
        assertEquals(1.0, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("sin(&PI)");
        assertEquals(0.0, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("sin(3.0 * &PI / 2.0)");
        assertEquals(-1.0, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testCosBoundary() {
        TestResult result = runSilent("cos(0.0)");
        assertEquals(1.0, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("cos(&PI / 2.0)");
        assertEquals(0.0, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("cos(&PI)");
        assertEquals(-1.0, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testTanBoundary() {
        TestResult result = runSilent("tan(0.0)");
        assertEquals(0.0, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("tan(&PI / 4.0)");
        assertEquals(1.0, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testAsinBoundary() {
        TestResult result = runSilent("asin(0.0)");
        assertEquals(0.0, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("asin(1.0)");
        assertEquals(Math.PI / 2, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("asin(-1.0)");
        assertEquals(-Math.PI / 2, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testAsinOutOfRangeThrowsError() {
        runExpectingError("asin(1.1)", "asin input must be between -1 and 1");
        runExpectingError("asin(-1.1)", "asin input must be between -1 and 1");
    }

    @Test
    void testAcosBoundary() {
        TestResult result = runSilent("acos(1.0)");
        assertEquals(0.0, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("acos(0.0)");
        assertEquals(Math.PI / 2, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("acos(-1.0)");
        assertEquals(Math.PI, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testAcosOutOfRangeThrowsError() {
        runExpectingError("acos(1.1)", "acos input must be between -1 and 1");
        runExpectingError("acos(-1.1)", "acos input must be between -1 and 1");
    }

    @Test
    void testAtan() {
        TestResult result = runSilent("atan(0.0)");
        assertEquals(0.0, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("atan(1.0)");
        assertEquals(Math.PI / 4, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testAtan2() {
        TestResult result = runSilent("atan2(1.0, 1.0)");
        assertEquals(Math.PI / 4, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("atan2(0.0, 1.0)");
        assertEquals(0.0, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("atan2(1.0, 0.0)");
        assertEquals(Math.PI / 2, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testRoundBoundary() {
        assertBothEqual(4L, runSilent("round(3.5)"));
        assertBothEqual(3L, runSilent("round(3.4)"));
        assertBothEqual(-3L, runSilent("round(-3.4)"));
        // Math.round(-3.5) 返回 -3 (向上取整到较大值)
        assertBothEqual(-3L, runSilent("round(-3.5)"));
        assertBothEqual(0L, runSilent("round(0.0)"));
        assertBothEqual(0L, runSilent("round(0.49)"));
        assertBothEqual(1L, runSilent("round(0.5)"));
    }

    @Test
    void testFloorBoundary() {
        assertBothEqual(3.0, runSilent("floor(3.9)"));
        assertBothEqual(3.0, runSilent("floor(3.1)"));
        assertBothEqual(3.0, runSilent("floor(3.0)"));
        assertBothEqual(-4.0, runSilent("floor(-3.1)"));
        assertBothEqual(-4.0, runSilent("floor(-3.9)"));
        assertBothEqual(0.0, runSilent("floor(0.9)"));
        assertBothEqual(-1.0, runSilent("floor(-0.1)"));
    }

    @Test
    void testCeilBoundary() {
        assertBothEqual(4.0, runSilent("ceil(3.1)"));
        assertBothEqual(4.0, runSilent("ceil(3.9)"));
        assertBothEqual(3.0, runSilent("ceil(3.0)"));
        assertBothEqual(-3.0, runSilent("ceil(-3.1)"));
        assertBothEqual(-3.0, runSilent("ceil(-3.9)"));
        assertBothEqual(1.0, runSilent("ceil(0.1)"));
        // Math.ceil(-0.1) 返回 -0.0，用 assertEquals 带 delta 比较
        TestResult result = runSilent("ceil(-0.1)");
        assertEquals(0.0, (Double) result.getInterpretResult(), 0.0);
        assertEquals(0.0, (Double) result.getCompileResult(), 0.0);
    }

    @Test
    void testSignPositive() {
        assertBothEqual(1, runSilent("sign(42)"));
        assertBothEqual(1, runSilent("sign(1)"));
        assertBothEqual(1, runSilent("sign(2147483647)"));
    }

    @Test
    void testSignNegative() {
        assertBothEqual(-1, runSilent("sign(-42)"));
        assertBothEqual(-1, runSilent("sign(-1)"));
        assertBothEqual(-1, runSilent("sign(-2147483648)"));
    }

    @Test
    void testSignZero() {
        assertBothEqual(0, runSilent("sign(0)"));
    }

    @Test
    void testSignDouble() {
        assertBothEqual(1.0, runSilent("sign(3.14)"));
        assertBothEqual(-1.0, runSilent("sign(-3.14)"));
        assertBothEqual(0.0, runSilent("sign(0.0)"));
    }

    @Test
    void testSignLong() {
        assertBothEqual(1, runSilent("sign(9223372036854775807L)"));
        // 使用略小于 MIN_VALUE 的值避免解析问题
        assertBothEqual(-1, runSilent("sign(-9223372036854775807L)"));
    }

    @Test
    void testLerpBoundary() {
        assertBothEqual(0.0, runSilent("lerp(0.0, 10.0, 0.0)"));
        assertBothEqual(10.0, runSilent("lerp(0.0, 10.0, 1.0)"));
        assertBothEqual(5.0, runSilent("lerp(0.0, 10.0, 0.5)"));
    }

    @Test
    void testLerpNegativeRange() {
        assertBothEqual(-10.0, runSilent("lerp(-10.0, -20.0, 0.0)"));
        assertBothEqual(-20.0, runSilent("lerp(-10.0, -20.0, 1.0)"));
        assertBothEqual(-15.0, runSilent("lerp(-10.0, -20.0, 0.5)"));
    }

    @Test
    void testLerpExtrapolation() {
        // t > 1
        assertBothEqual(20.0, runSilent("lerp(0.0, 10.0, 2.0)"));
        // t < 0
        assertBothEqual(-10.0, runSilent("lerp(0.0, 10.0, -1.0)"));
    }

    @Test
    void testHypotBasic() {
        assertBothEqual(5.0, runSilent("hypot(3.0, 4.0)"));
        assertBothEqual(13.0, runSilent("hypot(5.0, 12.0)"));
    }

    @Test
    void testHypotZero() {
        assertBothEqual(4.0, runSilent("hypot(0.0, 4.0)"));
        assertBothEqual(3.0, runSilent("hypot(3.0, 0.0)"));
        assertBothEqual(0.0, runSilent("hypot(0.0, 0.0)"));
    }

    @Test
    void testHypotNegative() {
        // hypot 使用绝对值
        assertBothEqual(5.0, runSilent("hypot(-3.0, 4.0)"));
        assertBothEqual(5.0, runSilent("hypot(3.0, -4.0)"));
        assertBothEqual(5.0, runSilent("hypot(-3.0, -4.0)"));
    }

    @Test
    void testRadDegBoundary() {
        TestResult result = runSilent("rad(0.0)");
        assertEquals(0.0, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("rad(180.0)");
        assertEquals(Math.PI, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("rad(360.0)");
        assertEquals(2 * Math.PI, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("rad(90.0)");
        assertEquals(Math.PI / 2, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testDegBoundary() {
        TestResult result = runSilent("deg(0.0)");
        assertEquals(0.0, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("deg(&PI)");
        assertEquals(180.0, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("deg(2.0 * &PI)");
        assertEquals(360.0, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testRadDegInverse() {
        // deg(rad(x)) == x
        TestResult result = runSilent("deg(rad(45.0))");
        assertEquals(45.0, (Double) result.getInterpretResult(), 0.0001);
        result = runSilent("rad(deg(&PI / 4.0))");
        assertEquals(Math.PI / 4, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testRandomNoArgsRange() {
        for (int i = 0; i < 100; i++) {
            TestResult result = runSilent("random()");
            double val = (Double) result.getInterpretResult();
            assertTrue(val >= 0.0 && val < 1.0, "random() should be in [0, 1)");
        }
    }

    @Test
    void testRandomIntRange() {
        for (int i = 0; i < 100; i++) {
            TestResult result = runSilent("random(10)");
            int val = (Integer) result.getInterpretResult();
            assertTrue(val >= 0 && val < 10, "random(10) should be in [0, 10)");
        }
    }

    @Test
    void testRandomIntRangeWithStart() {
        for (int i = 0; i < 100; i++) {
            TestResult result = runSilent("random(5, 10)");
            int val = (Integer) result.getInterpretResult();
            assertTrue(val >= 5 && val < 10, "random(5, 10) should be in [5, 10)");
        }
    }

    @Test
    void testRandomDoubleRange() {
        for (int i = 0; i < 100; i++) {
            TestResult result = runSilent("random(10.0)");
            double val = (Double) result.getInterpretResult();
            assertTrue(val >= 0.0 && val < 10.0, "random(10.0) should be in [0, 10)");
        }
    }

    @Test
    void testRandomDoubleRangeWithStart() {
        for (int i = 0; i < 100; i++) {
            TestResult result = runSilent("random(5.0, 10.0)");
            double val = (Double) result.getInterpretResult();
            assertTrue(val >= 5.0 && val < 10.0, "random(5.0, 10.0) should be in [5, 10)");
        }
    }

    @Test
    void testRandomInvalidEndThrowsError() {
        runExpectingError("random(0)", "random 0 must be positive");
        runExpectingError("random(-5)", "random -5 must be positive");
        runExpectingError("random(0.0)", "random 0.0 must be positive");
        runExpectingError("random(-5.0)", "random -5.0 must be positive");
    }

    @Test
    void testRandomInvalidRangeThrowsError() {
        runExpectingError("random(10, 5)", "random 10 must be less than 5");
        runExpectingError("random(5, 5)", "random 5 must be less than 5");
        runExpectingError("random(10.0, 5.0)", "random 10.0 must be less than 5.0");
    }

    @Test
    void testPIConstant() {
        TestResult result = runSilent("&PI");
        assertEquals(Math.PI, (Double) result.getInterpretResult(), 0.0000001);
    }

    @Test
    void testEConstant() {
        TestResult result = runSilent("&E");
        assertEquals(Math.E, (Double) result.getInterpretResult(), 0.0000001);
    }

    @Test
    void testMinReturnsCorrectType() {
        TestResult result = runSilent("min(1, 2)");
        assertTrue(result.getInterpretResult() instanceof Integer);
        assertTrue(result.getCompileResult() instanceof Integer);
        result = runSilent("min(1L, 2L)");
        assertTrue(result.getInterpretResult() instanceof Long);
        assertTrue(result.getCompileResult() instanceof Long);
        result = runSilent("min(1.0, 2.0)");
        assertTrue(result.getInterpretResult() instanceof Double);
        assertTrue(result.getCompileResult() instanceof Double);
    }

    @Test
    void testAbsReturnsCorrectType() {
        TestResult result = runSilent("abs(-5)");
        assertTrue(result.getInterpretResult() instanceof Integer);
        result = runSilent("abs(-5L)");
        assertTrue(result.getInterpretResult() instanceof Long);
        result = runSilent("abs(-5.0)");
        assertTrue(result.getInterpretResult() instanceof Double);
    }

    @Test
    void testPowReturnsCorrectType() {
        TestResult result = runSilent("pow(2, 3)");
        assertTrue(result.getInterpretResult() instanceof Integer);
        result = runSilent("pow(2L, 3L)");
        assertTrue(result.getInterpretResult() instanceof Long);
        result = runSilent("pow(2.0, 3.0)");
        assertTrue(result.getInterpretResult() instanceof Double);
    }

    @Test
    void testClampReturnsCorrectType() {
        TestResult result = runSilent("clamp(5, 0, 10)");
        assertTrue(result.getInterpretResult() instanceof Integer);
        result = runSilent("clamp(5L, 0L, 10L)");
        assertTrue(result.getInterpretResult() instanceof Long);
        result = runSilent("clamp(5.0, 0.0, 10.0)");
        assertTrue(result.getInterpretResult() instanceof Double);
    }
}
