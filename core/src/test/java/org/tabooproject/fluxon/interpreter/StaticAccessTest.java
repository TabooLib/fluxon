package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 静态方法和静态字段访问测试
 */
public class StaticAccessTest {

    // ==================== Part 1: forName().staticMethod() 测试 ====================

    @Test
    public void testForNameStaticMethod() {
        // 调用 Integer.parseInt 静态方法
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "forName(\"java.lang.Integer\").parseInt(\"42\")"
        );
        FluxonTestUtil.assertBothEqual(42, result);
    }

    @Test
    public void testForNameFallbackToClassMethod() {
        // getName() 应该回退到 Class 的实例方法
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "forName(\"java.lang.Integer\").getName()"
        );
        FluxonTestUtil.assertBothEqual("java.lang.Integer", result);
    }

    @Test
    public void testForNameStaticMethodWithMultipleArgs() {
        // 调用 Integer.parseInt(String, int) 静态方法
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "forName(\"java.lang.Integer\").parseInt(\"ff\", 16)"
        );
        FluxonTestUtil.assertBothEqual(255, result);
    }

    // ==================== Part 2: static 关键字语法测试 ====================

    @Test
    public void testStaticMethodCall() {
        // static java.lang.Integer.parseInt("42")
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "static java.lang.Integer.parseInt(\"100\")"
        );
        FluxonTestUtil.assertBothEqual(100, result);
    }

    @Test
    public void testStaticMethodCallWithMultipleArgs() {
        // static java.lang.Integer.parseInt("ff", 16)
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "static java.lang.Integer.parseInt(\"a\", 16)"
        );
        FluxonTestUtil.assertBothEqual(10, result);
    }

    @Test
    public void testStaticFieldAccess() {
        // static java.lang.Integer.MAX_VALUE
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "static java.lang.Integer.MAX_VALUE"
        );
        FluxonTestUtil.assertBothEqual(Integer.MAX_VALUE, result);
    }

    @Test
    public void testStaticFieldAccessMinValue() {
        // static java.lang.Integer.MIN_VALUE
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "static java.lang.Integer.MIN_VALUE"
        );
        FluxonTestUtil.assertBothEqual(Integer.MIN_VALUE, result);
    }

    @Test
    public void testStaticFieldChainedAccess() {
        // static java.lang.System.out 返回 PrintStream 对象
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "static java.lang.System.out"
        );
        assertNotNull(result.getInterpretResult());
        assertTrue(result.getInterpretResult() instanceof PrintStream);
    }

    @Test
    public void testStaticMethodCurrentTimeMillis() {
        // static java.lang.System.currentTimeMillis()
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "static java.lang.System.currentTimeMillis()"
        );
        assertNotNull(result.getInterpretResult());
        assertTrue(result.getInterpretResult() instanceof Long);
        assertTrue((Long) result.getInterpretResult() > 0);
    }

    @Test
    public void testStaticMethodValueOf() {
        // static java.lang.String.valueOf(123)
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "static java.lang.String.valueOf(123)"
        );
        FluxonTestUtil.assertBothEqual("123", result);
    }

    @Test
    public void testStaticMethodFormat() {
        // static java.lang.String.format("%d + %d = %d", 1, 2, 3)
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "static java.lang.String.format(\"%d + %d = %d\", 1, 2, 3)"
        );
        FluxonTestUtil.assertBothEqual("1 + 2 = 3", result);
    }

    // ==================== 错误处理测试 ====================

    @Test
    public void testStaticMethodNotFound() {
        // 调用不存在的静态方法应该抛出异常
        assertThrows(RuntimeException.class, () -> {
            FluxonTestUtil.interpret("static java.lang.Integer.nonExistentMethod()");
        });
    }

    @Test
    public void testStaticClassNotFound() {
        // 类不存在应该抛出异常
        assertThrows(RuntimeException.class, () -> {
            FluxonTestUtil.interpret("static com.nonexistent.FakeClass.method()");
        });
    }
}
