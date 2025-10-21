package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.compiler.FluxonFeatures;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 可迭代类型测试
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class IterableTest {

    @BeforeEach
    public void BeforeEach() {
        FluxonFeatures.DEFAULT_ALLOW_KETHER_STYLE_CALL = true;
    }

    // ========== first() 方法测试 ==========

    @Test
    public void testIterableFirst() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::first()");
        assertEquals(1, result.getInterpretResult());
        assertEquals(1, result.getCompileResult());
    }

    @Test
    public void testIterableFirstSingleElement() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [42]; &list::first()");
        assertEquals(42, result.getInterpretResult());
        assertEquals(42, result.getCompileResult());
    }

    // ========== last() 方法测试 ==========

    @Test
    public void testIterableLast() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::last()");
        assertEquals(3, result.getInterpretResult());
        assertEquals(3, result.getCompileResult());
    }

    @Test
    public void testIterableLastSingleElement() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [42]; &list::last()");
        assertEquals(42, result.getInterpretResult());
        assertEquals(42, result.getCompileResult());
    }

    // ========== take() 方法测试 ==========

    @Test
    public void testIterableTakeZero() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::take(0)");
        assertEquals("[]", result.getInterpretResult().toString());
        assertEquals("[]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableTakeOne() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::take(1)");
        assertEquals("[1]", result.getInterpretResult().toString());
        assertEquals("[1]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableTakeTwo() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::take(2)");
        assertEquals("[1, 2]", result.getInterpretResult().toString());
        assertEquals("[1, 2]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableTakeFull() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::take(3)");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableTakeExceed() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::take(10)");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3]", result.getCompileResult().toString());
    }

    // ========== drop() 方法测试 ==========

    @Test
    public void testIterableDropZero() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::drop(0)");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableDropOne() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::drop(1)");
        assertEquals("[2, 3]", result.getInterpretResult().toString());
        assertEquals("[2, 3]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableDropTwo() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::drop(2)");
        assertEquals("[3]", result.getInterpretResult().toString());
        assertEquals("[3]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableDropFull() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::drop(3)");
        assertEquals("[]", result.getInterpretResult().toString());
        assertEquals("[]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableDropExceed() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::drop(10)");
        assertEquals("[]", result.getInterpretResult().toString());
        assertEquals("[]", result.getCompileResult().toString());
    }

    // ========== takeLast() 方法测试 ==========

    @Test
    public void testIterableTakeLastZero() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::takeLast(0)");
        assertEquals("[]", result.getInterpretResult().toString());
        assertEquals("[]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableTakeLastOne() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::takeLast(1)");
        assertEquals("[3]", result.getInterpretResult().toString());
        assertEquals("[3]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableTakeLastTwo() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::takeLast(2)");
        assertEquals("[2, 3]", result.getInterpretResult().toString());
        assertEquals("[2, 3]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableTakeLastFull() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::takeLast(3)");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableTakeLastExceed() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::takeLast(10)");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3]", result.getCompileResult().toString());
    }

    // ========== dropLast() 方法测试 ==========

    @Test
    public void testIterableDropLastZero() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::dropLast(0)");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableDropLastOne() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::dropLast(1)");
        assertEquals("[1, 2]", result.getInterpretResult().toString());
        assertEquals("[1, 2]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableDropLastTwo() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::dropLast(2)");
        assertEquals("[1]", result.getInterpretResult().toString());
        assertEquals("[1]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableDropLastFull() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::dropLast(3)");
        assertEquals("[]", result.getInterpretResult().toString());
        assertEquals("[]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableDropLastExceed() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; &list::dropLast(10)");
        assertEquals("[]", result.getInterpretResult().toString());
        assertEquals("[]", result.getCompileResult().toString());
    }

    // ========== 边界情况测试 ==========

    @Test
    public void testIterableEmptyList() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("list = []; &list::take(1)");
        assertEquals("[]", result.getInterpretResult().toString());
        assertEquals("[]", result.getCompileResult().toString());

        result = FluxonTestUtil.runSilent("list = []; &list::drop(1)");
        assertEquals("[]", result.getInterpretResult().toString());
        assertEquals("[]", result.getCompileResult().toString());
    }

    // ========== 链式调用测试 ==========

    @Test
    public void testIterableChainedOperations() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3, 4, 5]; &list::drop(1)::take(3)");
        assertEquals("[2, 3, 4]", result.getInterpretResult().toString());
        assertEquals("[2, 3, 4]", result.getCompileResult().toString());
    }

    @Test
    public void testIterableChainedDropLast() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3, 4, 5]; &list::dropLast(2)::takeLast(2)");
        assertEquals("[2, 3]", result.getInterpretResult().toString());
        assertEquals("[2, 3]", result.getCompileResult().toString());
    }

    // ========== 与其他操作结合测试 ==========

    @Test
    public void testIterableFirstInExpression() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [10, 20, 30]; &list::first() + 5");
        assertEquals(15, result.getInterpretResult());
        assertEquals(15, result.getCompileResult());
    }

    @Test
    public void testIterableLastInExpression() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [10, 20, 30]; &list::last() * 2");
        assertEquals(60, result.getInterpretResult());
        assertEquals(60, result.getCompileResult());
    }

    @Test
    public void testIterableTakeInLoop() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3, 4, 5]; " +
                        "result = 0; " +
                        "for i in &list::take(3) { result += &i }; " +
                        "&result");
        assertEquals(6, result.getInterpretResult());
        assertEquals(6, result.getCompileResult());
    }

    @Test
    public void testIterableDropInLoop() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3, 4, 5]; " +
                        "result = 0; " +
                        "for i in &list::drop(2) { result += &i }; " +
                        "&result");
        assertEquals(12, result.getInterpretResult());
        assertEquals(12, result.getCompileResult());
    }

    @Test
    public void testIterableOperationsInConditional() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; " +
                        "if &list::first() == 1 then 'yes' else 'no'");
        assertEquals("yes", result.getInterpretResult());
        assertEquals("yes", result.getCompileResult());
    }

    // ========== 不同类型集合测试 ==========

    @Test
    public void testIterableOperationsOnLargeList() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = 1..100; " +
                        "result = 0; " +
                        "for i in &list::take(10) { result += &i }; " +
                        "&result");
        assertEquals(55, result.getInterpretResult());
        assertEquals(55, result.getCompileResult());
    }
}
