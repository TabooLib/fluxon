package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.tabooproject.fluxon.FluxonTestUtil.assertBothEqual;

/**
 * 幂运算符测试类
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class PowerOperatorTest {

    @Test
    public void testBasicPower() {
        // 测试基本幂运算
        assertBothEqual(8.0, FluxonTestUtil.runSilent("2^3"));
        assertBothEqual(1.0, FluxonTestUtil.runSilent("5^0"));
        assertBothEqual(5.0, FluxonTestUtil.runSilent("5^1"));
    }

    @Test
    public void testRightAssociativity() {
        // 测试右结合性：2^3^2 = 2^(3^2) = 2^9 = 512
        assertBothEqual(512.0, FluxonTestUtil.runSilent("2^3^2"));
    }

    @Test
    public void testPrecedenceWithMultiply() {
        // 测试优先级：2*3^2 = 2*(3^2) = 2*9 = 18
        assertBothEqual(18.0, FluxonTestUtil.runSilent("2*3^2"));
        // 测试优先级：3^2*2 = (3^2)*2 = 9*2 = 18
        assertBothEqual(18.0, FluxonTestUtil.runSilent("3^2*2"));
    }

    @Test
    public void testPrecedenceWithAddition() {
        // 测试优先级：1+2^3 = 1+(2^3) = 1+8 = 9
        assertBothEqual(9.0, FluxonTestUtil.runSilent("1+2^3"));
        // 测试优先级：2^3+1 = (2^3)+1 = 8+1 = 9
        assertBothEqual(9.0, FluxonTestUtil.runSilent("2^3+1"));
    }

    @Test
    public void testNegativeExponent() {
        // 测试负指数
        assertBothEqual(0.25, FluxonTestUtil.runSilent("2^-2"));
        assertBothEqual(0.125, FluxonTestUtil.runSilent("2^-3"));
    }

    @Test
    public void testFractionalExponent() {
        // 测试小数指数（开方）
        assertBothEqual(2.0, FluxonTestUtil.runSilent("4^0.5"));
        assertBothEqual(3.0, FluxonTestUtil.runSilent("27^(1.0/3)"));
    }

    @Test
    public void testNegativeBase() {
        // 测试负底数
        assertBothEqual(-8.0, FluxonTestUtil.runSilent("(-2)^3"));
        assertBothEqual(16.0, FluxonTestUtil.runSilent("(-2)^4"));
    }

    @Test
    public void testWithVariables() {
        // 测试变量幂运算
        assertBothEqual(32.0, FluxonTestUtil.runSilent("base = 2; exp = 5; &base^&exp"));
    }
}
