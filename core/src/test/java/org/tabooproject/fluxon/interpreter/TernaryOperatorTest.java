package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.tabooproject.fluxon.Fluxon;

/**
 * 三元运算符测试类
 */
public class TernaryOperatorTest {

    @Test
    public void testBasicTernary() {
        // 测试基本的三元运算符
        assertEquals(10, Fluxon.eval("true ? 10 : 20"));
        assertEquals(20, Fluxon.eval("false ? 10 : 20"));
    }

    @Test
    public void testTernaryWithExpressions() {
        // 测试包含表达式的三元运算符
        assertEquals(15, Fluxon.eval("5 > 3 ? 10 + 5 : 20"));
        assertEquals(20, Fluxon.eval("5 < 3 ? 10 + 5 : 20"));
    }

    @Test
    public void testTernaryWithVariables() {
        // 测试包含变量的三元运算符
        assertEquals("adult", Fluxon.eval("age = 25; &age >= 18 ? \"adult\" : \"minor\""));
        assertEquals("minor", Fluxon.eval("age = 15; &age >= 18 ? \"adult\" : \"minor\""));
    }

    @Test
    public void testNestedTernary() {
        // 测试嵌套的三元运算符
        assertEquals("A", Fluxon.eval("score = 95; &score >= 90 ? \"A\" : &score >= 80 ? \"B\" : \"C\""));
        assertEquals("B", Fluxon.eval("score = 85; &score >= 90 ? \"A\" : &score >= 80 ? \"B\" : \"C\""));
        assertEquals("C", Fluxon.eval("score = 75; &score >= 90 ? \"A\" : &score >= 80 ? \"B\" : \"C\""));
    }

    @Test
    public void testTernaryWithNull() {
        // 测试包含 null 的三元运算符
        assertEquals("default", Fluxon.eval("value = null; &value != null ? &value : \"default\""));
        assertEquals("hello", Fluxon.eval("value = \"hello\"; &value != null ? &value : \"default\""));
    }

    @Test
    public void testTernaryWithDifferentTypes() {
        // 测试不同类型的三元运算符
        assertEquals(10, Fluxon.eval("true ? 10 : \"hello\""));
        assertEquals("hello", Fluxon.eval("false ? 10 : \"hello\""));
    }

    @Test
    public void testTernaryWithLogicalOperators() {
        // 测试与逻辑运算符结合的三元运算符
        assertEquals("both", Fluxon.eval("a = true; b = true; &a && &b ? \"both\" : \"not both\""));
        assertEquals("not both", Fluxon.eval("a = true; b = false; &a && &b ? \"both\" : \"not both\""));
    }

    @Test
    public void testTernaryWithComparisonOperators() {
        // 测试与比较运算符结合的三元运算符
        assertEquals("greater", Fluxon.eval("x = 10; y = 5; &x > &y ? \"greater\" : \"less or equal\""));
        assertEquals("less or equal", Fluxon.eval("x = 5; y = 10; &x > &y ? \"greater\" : \"less or equal\""));
    }
}