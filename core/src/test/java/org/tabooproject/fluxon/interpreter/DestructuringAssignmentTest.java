package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 解构赋值表达式测试
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class DestructuringAssignmentTest {

    // ========== 基础解构赋值测试 ==========

    @Test
    public void testListDestructuring() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "(a, b) = [1, 2]; &a + &b");
        assertEquals(3, result.getInterpretResult());
        assertEquals(3, result.getCompileResult());
    }

    @Test
    public void testListDestructuringThreeVariables() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "(a, b, c) = [10, 20, 30]; &a + &b + &c");
        assertEquals(60, result.getInterpretResult());
        assertEquals(60, result.getCompileResult());
    }

    @Test
    public void testMapEntryDestructuringViaForLoop() {
        // Map.Entry 解构主要通过 for 循环使用
        // 这里测试解构赋值与 for 循环解构行为一致
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [[1, 2]]; " +
                "(k, v) = &list[0]; " +
                "&k + &v");
        assertEquals(3, result.getInterpretResult());
        assertEquals(3, result.getCompileResult());
    }

    // ========== 与 for 循环解构一致性测试 ==========

    @Test
    public void testConsistencyWithForLoop() {
        // 使用 for 循环解构
        FluxonTestUtil.TestResult forResult = FluxonTestUtil.runSilent(
                "result = 0; " +
                "for (k, v) in [[1, 2], [3, 4]] { " +
                "  result += &k + &v " +
                "}; &result");
        
        // 使用解构赋值
        FluxonTestUtil.TestResult assignResult = FluxonTestUtil.runSilent(
                "result = 0; " +
                "(a, b) = [1, 2]; result += &a + &b; " +
                "(c, d) = [3, 4]; result += &c + &d; " +
                "&result");
        
        assertEquals(forResult.getInterpretResult(), assignResult.getInterpretResult());
        assertEquals(forResult.getCompileResult(), assignResult.getCompileResult());
    }

    // ========== 复杂场景测试 ==========

    @Test
    public void testDestructuringWithExpressionValue() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def getList() { [5, 10] }; " +
                "(x, y) = getList(); " +
                "&x * &y");
        assertEquals(50, result.getInterpretResult());
        assertEquals(50, result.getCompileResult());
    }

    @Test
    public void testDestructuringInBlock() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = { " +
                "  (a, b) = [100, 200]; " +
                "  &a + &b " +
                "}; &result");
        assertEquals(300, result.getInterpretResult());
        assertEquals(300, result.getCompileResult());
    }

    @Test
    public void testMultipleDestructuringAssignments() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "(a, b) = [1, 2]; " +
                "(c, d) = [3, 4]; " +
                "(e, f) = [5, 6]; " +
                "&a + &b + &c + &d + &e + &f");
        assertEquals(21, result.getInterpretResult());
        assertEquals(21, result.getCompileResult());
    }

    @Test
    public void testDestructuringWithNestedList() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "(first, second) = [[1, 2], [3, 4]]; " +
                "&first[0] + &second[1]");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());
    }

    // ========== 变量作用域测试 ==========

    @Test
    public void testVariableScopeAfterDestructuring() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "(x, y) = [10, 20]; " +
                "x = &x * 2; " +
                "&x + &y");
        assertEquals(40, result.getInterpretResult());
        assertEquals(40, result.getCompileResult());
    }

    @Test
    public void testDestructuringReassignment() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "(a, b) = [1, 2]; " +
                "(a, b) = [&b, &a]; " +  // swap
                "&a * 10 + &b");
        assertEquals(21, result.getInterpretResult());
        assertEquals(21, result.getCompileResult());
    }

    // ========== 字符串解构测试 ==========

    @Test
    public void testStringDestructuring() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "(first, second) = ['hello', 'world']; " +
                "&first + ' ' + &second");
        assertEquals("hello world", result.getInterpretResult());
        assertEquals("hello world", result.getCompileResult());
    }

    // ========== 解构返回值测试 ==========

    @Test
    public void testDestructuringExpressionReturnsValue() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = ((a, b) = [42, 58]); &result");
        // 解构表达式返回被解构的值
        assertNotNull(result.getInterpretResult());
        assertNotNull(result.getCompileResult());
    }

    // ========== 结构 Map 测试 ===========

    @Test
    public void testDestructuringMap() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "(name, version) = ['name': 'Fluxon', 'version': 1.0]" +
                "&name + ': ' + &version");
        assertTrue(result.isMatch());
        assertEquals("Fluxon: 1.0", result.getInterpretResult());
    }
}
