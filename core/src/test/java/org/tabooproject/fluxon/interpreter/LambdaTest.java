package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lambda 表达式测试
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class LambdaTest {

    // ========== 基础 Lambda 测试 ==========

    @Test
    public void testSimpleLambda() {
        // 测试简单的 lambda 表达式
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "f = lambda (x) -> &x + 1; " +
                        "call(&f, [5])");
        assertEquals(6, result.getInterpretResult());
    }

    @Test
    public void testLambdaWithMultipleParams() {
        // 测试多参数 lambda
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "add = lambda (x, y) -> &x + &y; " +
                        "call(&add, [3, 4])");
        assertEquals(7, result.getInterpretResult());
    }

    @Test
    public void testLambdaWithNoParams() {
        // 测试无参数 lambda
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "getAnswer = lambda -> 42; " +
                        "call(&getAnswer, [])");
        assertEquals(42, result.getInterpretResult());
    }

    @Test
    public void testLambdaWithBlock() {
        // 测试带代码块的 lambda
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "f = lambda (x) { y = &x * 2; &y + 1 }; " +
                        "call(&f, [5])");
        assertEquals(11, result.getInterpretResult());
    }

    // ========== 闭包捕获测试 ==========

    @Test
    public void testLambdaCaptureLocalVariable() {
        // 测试 lambda 捕获外部局部变量
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "outer = 10; " +
                        "f = lambda (x) -> &x + &outer; " +
                        "call(&f, [5])");
        assertEquals(15, result.getInterpretResult());
    }

    @Test
    public void testLambdaCaptureInLoop() {
        // 测试在循环中创建 lambda 并捕获变量
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "funcs = []; " +
                        "for i in 1..3 { " +
                        "  &funcs += lambda -> &i " +
                        "}; " +
                        "call(&funcs[0], [])");
        // 注意：这里捕获的是最终的 i 值（类似 Java 的闭包行为）
        assertNotNull(result.getInterpretResult());
    }

    @Test
    public void testNestedLambda() {
        // 测试嵌套 lambda
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "makeAdder = lambda (x) -> lambda (y) -> &x + &y; " +
                        "add5 = call(&makeAdder, [5]); " +
                        "call(&add5, [3])");
        assertEquals(8, result.getInterpretResult());
    }

    // ========== Lambda 作为参数测试 ==========

    @Test
    public void testLambdaAsParameter() {
        // 测试 lambda 作为函数参数
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "def apply(f, x) = call(&f, [&x]); " +
                        "double = lambda (n) -> &n * 2; " +
                        "apply(&double, 5)");
        assertEquals(10, result.getInterpretResult());
    }

    @Test
    public void testMapWithLambda() {
        // 测试使用 lambda 进行 map 操作
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "nums = [1, 2, 3]; " +
                        "doubled = &nums :: map(lambda (x) -> &x * 2); " +
                        "&doubled");
        assertEquals("[2, 4, 6]", result.getInterpretResult().toString());
    }

    @Test
    public void testFilterWithLambda() {
        // 测试使用 lambda 进行 filter 操作
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "nums = [1, 2, 3, 4, 5]; " +
                        "evens = &nums :: filter(lambda (x) -> &x % 2 == 0); " +
                        "&evens");
        assertEquals("[2, 4]", result.getInterpretResult().toString());
    }

    // ========== Lambda 返回值测试 ==========

    @Test
    public void testLambdaReturnLambda() {
        // 测试 lambda 返回另一个 lambda
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "makeMultiplier = lambda (factor) -> lambda (x) -> &x * &factor; " +
                        "triple = call(&makeMultiplier, [3]); " +
                        "call(&triple, [4])");
        assertEquals(12, result.getInterpretResult());
    }

    @Test
    public void testLambdaWithConditional() {
        // 测试 lambda 内部使用条件表达式
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "abs = lambda (x) -> if &x < 0 then -&x else &x; " +
                        "call(&abs, [-5])");
        assertEquals(5, result.getInterpretResult());
    }

    // ========== 复杂场景测试 ==========

    @Test
    public void testLambdaInExpression() {
        // 测试 lambda 在表达式中使用
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "result = call(lambda (x, y) -> &x * &y, [3, 4]); " +
                        "&result");
        assertEquals(12, result.getInterpretResult());
    }

    @Test
    public void testMultipleLambdasInList() {
        // 测试在列表中存储多个 lambda
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "ops = [lambda (x) -> &x + 1, lambda (x) -> &x * 2, lambda (x) -> &x - 1]; " +
                        "call(&ops[1], [5])");
        assertEquals(10, result.getInterpretResult());
    }

    @Test
    public void testLambdaWithMultipleStatements() {
        // 测试包含多个语句的 lambda
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "f = lambda (x, y) { " +
                        "  a = &x * 2; " +
                        "  b = &y + 3; " +
                        "  &a + &b " +
                        "}; " +
                        "call(&f, [5, 10])");
        assertEquals(23, result.getInterpretResult());
    }

    // ========== 边界情况测试 ==========

    @Test
    public void testLambdaWithNullReturn() {
        // 测试返回 null 的 lambda
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "f = lambda -> null; " +
                        "call(&f, [])");
        assertNull(result.getInterpretResult());
    }

    @Test
    public void testLambdaCapturingFunction() {
        // 测试 lambda 捕获函数
        FluxonTestUtil.TestResult result = FluxonTestUtil.interpret(
                "def inc(x) = &x + 1; " +
                        "wrapper = lambda (x) -> inc(&x); " +
                        "call(&wrapper, [5])");
        assertEquals(6, result.getInterpretResult());
    }
}
