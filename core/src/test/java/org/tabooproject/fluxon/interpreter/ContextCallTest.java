package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.FluxonFeatures;
import org.tabooproject.fluxon.interpreter.error.ArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ContextCallTest {

    @BeforeEach
    public void BeforeEach() {
        FluxonFeatures.DEFAULT_ALLOW_KETHER_STYLE_CALL = true;
    }

    // 1755611940830L = 2025-08-19 21:59:00
    @Test
    public void test1() {
        assertEquals("2025-08-19 21:59:00", Fluxon.eval("import 'fs:time'; time :: formatTimestamp(1755611940830L)"));
    }

    @Test
    public void test2() {
        assertEquals("[2025, 08, 19 21:59:00]", Fluxon.eval("import 'fs:time'; time :: formatTimestamp(1755611940830L) :: split('-')").toString());
    }

    @Test
    public void test3() {
        // 检索扩展函数: formatTimestamp
        // 检索扩展函数: split
        // 0
        // 0.9793372836056653
        // 1.266626930227011
        // 2.0234217076733807
        // 2.9088529523673623
        // 3.267841899894786
        // print 和 random 不会被视为扩展函数
        Fluxon.eval("import 'fs:time'" +
                "i = 0" +
                "while (&i < 3) { " +
                "  time :: formatTimestamp(1755611940830L) :: split('-') " +
                "  print &i" +
                "  i += random()" +
                "}" +
                "print &i");
    }

    @Test
    public void test4() {
        assertEquals(1, Fluxon.eval("day = 1; &day"));
        Fluxon.eval("time :: { time = 10 ; print &time; print day }");
    }

    @Test
    public void test5() {
        assertEquals(true, Fluxon.eval("list = [1,2,3]; &list::contains 1"));
        assertEquals(false, Fluxon.eval("list = [1,2,3]; !&list::contains 1"));
        Fluxon.eval("list = [1,2,3]; print !&list::contains 1");
    }

    @Test
    public void testOperatorPrecedence() {
        // 测试 ! 和 :: 的优先级
        assertEquals(false, Fluxon.eval("list = [1,2,3]; !&list::contains(1)"));
        assertEquals(true, Fluxon.eval("list = [1,2,3]; !&list::contains(4)"));

        // 测试 & 和 :: 的结合
        assertEquals(true, Fluxon.eval("list = [1,2,3]; &list::contains(2)"));
        assertEquals(false, Fluxon.eval("list = [1,2,3]; &list::contains(5)"));
    }

    @Test
    public void testComplexPrecedence() {
        // 测试复杂的优先级组合
        assertEquals(false, Fluxon.eval("list = [1,2,3]; !&list::contains(1) && true"));
        assertEquals(true, Fluxon.eval("list = [1,2,3]; !&list::contains(4) || false"));

        // 测试多重上下文调用
        assertEquals("HELLO", Fluxon.eval("text = 'hello'; &text::uppercase()"));
        assertEquals(5, Fluxon.eval("text = 'hello'; &text::length()"));

        // 测试引用后的多重操作
        assertEquals("HE", Fluxon.eval("text = 'hello'; &text::uppercase()::substring(0, 2)"));
    }

    @Test
    public void testUnaryWithReference() {
        // 测试负号与引用
        assertEquals(-5, Fluxon.eval("num = 5; -&num"));
        assertEquals(5, Fluxon.eval("num = -5; -&num"));
    }

    @Test
    public void testMixedOperators() {
        // 测试算术运算符与引用
        assertEquals(10, Fluxon.eval("a = 3; b = 7; &a + &b"));
        assertEquals(-4, Fluxon.eval("a = 3; b = 7; &a - &b"));
        assertEquals(21, Fluxon.eval("a = 3; b = 7; &a * &b"));

        // 测试比较运算符与引用
        assertEquals(true, Fluxon.eval("a = 3; b = 7; &a < &b"));
        assertEquals(false, Fluxon.eval("a = 3; b = 7; &a > &b"));
        assertEquals(true, Fluxon.eval("a = 3; b = 3; &a == &b"));
    }

    @Test
    public void testReferenceInExpressions() {
        // 测试引用在括号表达式中
        assertEquals(false, Fluxon.eval("list = [1,2,3]; !(&list::contains(1))"));
        assertEquals(true, Fluxon.eval("list = [1,2,3]; !(!&list::contains(1))"));

        // 测试引用在条件表达式中
        assertEquals(3, Fluxon.eval("list = [1,2,3]; if &list::contains(1) then 3 else 5"));
        assertEquals(5, Fluxon.eval("list = [1,2,3]; if &list::contains(4) then 3 else 5"));
    }

    @Test
    public void testContextCallChaining() {
        // 测试连续的上下文调用
        assertEquals(3, Fluxon.eval("list = [1,2,3]; &list::size()"));
        assertEquals("3", Fluxon.eval("list = [1,2,3]; &list::size()::toString()"));

        // 测试上下文调用与其他操作符的组合
        assertEquals(6, Fluxon.eval("list = [1,2,3]; &list::size() * 2"));
        assertEquals(true, Fluxon.eval("list = [1,2,3]; &list::size() > 2"));
    }

    @Test
    public void testElvisWithReference() {
        // 测试 Elvis 操作符与引用
        assertEquals(5, Fluxon.eval("a = 5; &a ?: 10"));
        assertEquals(10, Fluxon.eval("a = null; &a ?: 10"));
        // 测试 Elvis 与上下文调用
        assertEquals(true, Fluxon.eval("list = [1,2,3]; &list::contains(1) ?: false"));
        assertEquals(true, Fluxon.eval("list = null; (&list ?: [1]) :: contains(1)"));
    }

    @Test
    public void testAssignmentWithReference() {
        // 测试赋值与引用的组合
        assertEquals(10, Fluxon.eval("a = 5; b = &a * 2; &b"));
        assertEquals(15, Fluxon.eval("a = 5; a += &a * 2; &a"));
        // 测试复合赋值
        assertEquals(25, Fluxon.eval("a = 5; a *= &a; &a"));
    }

    @Test
    public void testEdgeCases() {
        // 测试边界情况
        assertEquals(false, Fluxon.eval("list = []; !&list::isEmpty()"));
        assertEquals(true, Fluxon.eval("list = []; &list::isEmpty()"));

        // 测试可选引用
        // &?unknownVar 不会抛出异常
        assertEquals(null, Fluxon.eval("&?unknownVar"));

        // 测试多重否定
        assertEquals(true, Fluxon.eval("list = [1,2,3]; !!&list::contains(1)"));
        assertEquals(false, Fluxon.eval("list = [1,2,3]; !!!&list::contains(1)"));
    }

    @Test
    public void testRandom() {
        assertEquals(1, Fluxon.eval("[1]::get(g::random(1))"));
        try {
            Fluxon.eval("[1]::get(random(1))");
            throw new IllegalStateException("random(1) should return a ArrayList");
        } catch (ArgumentTypeMismatchException e) {
            assertEquals("Argument 0 expect Number but got [1]", e.getMessage());
        }
    }
}
