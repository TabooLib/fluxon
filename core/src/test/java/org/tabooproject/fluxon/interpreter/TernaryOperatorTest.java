package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.tabooproject.fluxon.FluxonTestUtil.assertBothEqual;

/**
 * 三元运算符测试类
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class TernaryOperatorTest {

    @Test
    public void testBasicTernary() {
        // 测试基本的三元运算符
        assertBothEqual(10, FluxonTestUtil.runSilent("true ? 10 : 20"));
        assertBothEqual(20, FluxonTestUtil.runSilent("false ? 10 : 20"));
    }

    @Test
    public void testTernaryWithExpressions() {
        // 测试包含表达式的三元运算符
        assertBothEqual(15, FluxonTestUtil.runSilent("5 > 3 ? 10 + 5 : 20"));
        assertBothEqual(20, FluxonTestUtil.runSilent("5 < 3 ? 10 + 5 : 20"));
    }

    @Test
    public void testTernaryWithVariables() {
        // 测试包含变量的三元运算符
        assertBothEqual("adult", FluxonTestUtil.runSilent("age = 25; &age >= 18 ? \"adult\" : \"minor\""));
        assertBothEqual("minor", FluxonTestUtil.runSilent("age = 15; &age >= 18 ? \"adult\" : \"minor\""));
    }

    @Test
    public void testNestedTernary() {
        // 测试嵌套的三元运算符
        assertBothEqual("A", FluxonTestUtil.runSilent("score = 95; &score >= 90 ? \"A\" : &score >= 80 ? \"B\" : \"C\""));
        assertBothEqual("B", FluxonTestUtil.runSilent("score = 85; &score >= 90 ? \"A\" : &score >= 80 ? \"B\" : \"C\""));
        assertBothEqual("C", FluxonTestUtil.runSilent("score = 75; &score >= 90 ? \"A\" : &score >= 80 ? \"B\" : \"C\""));
    }

    @Test
    public void testTernaryWithNull() {
        // 测试包含 null 的三元运算符
        assertBothEqual("default", FluxonTestUtil.runSilent("value = null; &value != null ? &value : \"default\""));
        assertBothEqual("hello", FluxonTestUtil.runSilent("value = \"hello\"; &value != null ? &value : \"default\""));
    }

    @Test
    public void testTernaryWithDifferentTypes() {
        // 测试不同类型的三元运算符
        assertBothEqual(10, FluxonTestUtil.runSilent("true ? 10 : \"hello\""));
        assertBothEqual("hello", FluxonTestUtil.runSilent("false ? 10 : \"hello\""));
    }

    @Test
    public void testTernaryWithLogicalOperators() {
        // 测试与逻辑运算符结合的三元运算符
        assertBothEqual("both", FluxonTestUtil.runSilent("a = true; b = true; &a && &b ? \"both\" : \"not both\""));
        assertBothEqual("not both", FluxonTestUtil.runSilent("a = true; b = false; &a && &b ? \"both\" : \"not both\""));
    }

    @Test
    public void testTernaryWithComparisonOperators() {
        // 测试与比较运算符结合的三元运算符
        assertBothEqual("greater", FluxonTestUtil.runSilent("x = 10; y = 5; &x > &y ? \"greater\" : \"less or equal\""));
        assertBothEqual("less or equal", FluxonTestUtil.runSilent("x = 5; y = 10; &x > &y ? \"greater\" : \"less or equal\""));
    }
}
