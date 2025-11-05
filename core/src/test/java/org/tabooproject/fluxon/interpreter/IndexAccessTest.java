package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 索引访问功能测试
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class IndexAccessTest {

    @Test
    public void testListIndexAccess() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("list = [10, 20, 30]\n" +
                "&list[0]");
        assertEquals(10, testResult.getInterpretResult());
        assertEquals(10, testResult.getCompileResult());
        testResult = FluxonTestUtil.runSilent("list = [10, 20, 30]\n" +
                "&list[1]");
        assertEquals(20, testResult.getInterpretResult());
        assertEquals(20, testResult.getCompileResult());
        testResult = FluxonTestUtil.runSilent("list = [10, 20, 30]\n" +
                "&list[2]");
        assertEquals(30, testResult.getInterpretResult());
        assertEquals(30, testResult.getCompileResult());
    }

    @Test
    public void testMapIndexAccess() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("map = [name: 'test', value: 42]\n" +
                "&map['name']");
        assertEquals("test", testResult.getInterpretResult());
        assertEquals("test", testResult.getCompileResult());
        testResult = FluxonTestUtil.runSilent("map = [name: 'test', value: 42]\n" +
                "&map['value']");
        assertEquals(42, testResult.getInterpretResult());
        assertEquals(42, testResult.getCompileResult());
    }

    @Test
    public void testListIndexAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("list = [1, 2, 3]\n" +
                "&list[0] = 99\n" +
                "&list[0]");
        assertEquals(99, testResult.getInterpretResult());
        assertEquals(99, testResult.getCompileResult());
    }

    @Test
    public void testMapIndexAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("map = [key: 'old']\n" +
                "&map['key'] = 'new'\n" +
                "&map['key']");
        assertEquals("new", testResult.getInterpretResult());
        assertEquals("new", testResult.getCompileResult());
    }

    @Test
    public void testCompoundIndexAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("list = [10, 20, 30]\n" +
                "&list[0] += 5\n" +
                "&list[0]");
        assertEquals(15, testResult.getInterpretResult());
        assertEquals(15, testResult.getCompileResult());
    }

    @Test
    public void testMultiIndexAccess() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("nested = [a: [b: [c: 100]]]\n" +
                "&nested['a', 'b', 'c']");
        assertEquals(100, testResult.getInterpretResult());
        assertEquals(100, testResult.getCompileResult());
    }

    @Test
    public void testChainedIndexAccess() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("nested = [a: [b: [c: 200]]]\n" +
                "&nested['a']['b']['c']");
        assertEquals(200, testResult.getInterpretResult());
        assertEquals(200, testResult.getCompileResult());
    }

    @Test
    public void testReferenceIndexAccess() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("map = [:]\n" +
                "&map['test'] = 42\n" +
                "&map['test']");
        assertEquals(42, testResult.getInterpretResult());
        assertEquals(42, testResult.getCompileResult());
    }

    @Test
    public void testReferenceIndexCompoundAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("list = [5, 10, 15]\n" +
                "&list[1] *= 2\n" +
                "&list[1]");
        assertEquals(20, testResult.getInterpretResult());
        assertEquals(20, testResult.getCompileResult());
    }

    @Test
    public void testListCompoundAddAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("list = [10, 20, 30]\n" +
                "&list[1] += 5\n" +
                "&list[1]");
        assertEquals(25, testResult.getInterpretResult());
        assertEquals(25, testResult.getCompileResult());
    }

    @Test
    public void testListCompoundSubtractAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("list = [10, 20, 30]\n" +
                "&list[1] -= 5\n" +
                "&list[1]");
        assertEquals(15, testResult.getInterpretResult());
        assertEquals(15, testResult.getCompileResult());
    }

    @Test
    public void testListCompoundMultiplyAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("list = [10, 20, 30]\n" +
                "&list[1] *= 3\n" +
                "&list[1]");
        assertEquals(60, testResult.getInterpretResult());
        assertEquals(60, testResult.getCompileResult());
    }

    @Test
    public void testListCompoundDivideAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("list = [10, 20, 30]\n" +
                "&list[1] /= 4\n" +
                "&list[1]");
        assertEquals(5, testResult.getInterpretResult());
        assertEquals(5, testResult.getCompileResult());
    }

    @Test
    public void testListCompoundModuloAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("list = [10, 23, 30]\n" +
                "&list[1] %= 5\n" +
                "&list[1]");
        assertEquals(3, testResult.getInterpretResult());
        assertEquals(3, testResult.getCompileResult());
    }

    @Test
    public void testMapCompoundAddAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("map = [value: 10]\n" +
                "&map['value'] += 5\n" +
                "&map['value']");
        assertEquals(15, testResult.getInterpretResult());
        assertEquals(15, testResult.getCompileResult());
    }

    @Test
    public void testMapCompoundSubtractAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("map = [value: 20]\n" +
                "&map['value'] -= 7\n" +
                "&map['value']");
        assertEquals(13, testResult.getInterpretResult());
        assertEquals(13, testResult.getCompileResult());
    }

    @Test
    public void testMapCompoundMultiplyAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("map = [value: 6]\n" +
                "&map['value'] *= 7\n" +
                "&map['value']");
        assertEquals(42, testResult.getInterpretResult());
        assertEquals(42, testResult.getCompileResult());
    }

    @Test
    public void testMapCompoundDivideAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("map = [value: 50]\n" +
                "&map['value'] /= 5\n" +
                "&map['value']");
        assertEquals(10, testResult.getInterpretResult());
        assertEquals(10, testResult.getCompileResult());
    }

    @Test
    public void testMapCompoundModuloAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("map = [value: 17]\n" +
                "&map['value'] %= 5\n" +
                "&map['value']");
        assertEquals(2, testResult.getInterpretResult());
        assertEquals(2, testResult.getCompileResult());
    }

    @Test
    public void testNestedMapCompoundAssignment() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("map = [nested: [value: 100]]\n" +
                "&map['nested']['value'] += 50\n" +
                "&map['nested']['value']");
        assertEquals(150, testResult.getInterpretResult());
        assertEquals(150, testResult.getCompileResult());
    }

    @Test
    public void testMethodCallWithIndexAccess() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent(
                "field = 'a\"b\"c\"d\"e'\n" +
                "&field::split(\"\\\"\")[3]");
        assertEquals("d", testResult.getInterpretResult());
        assertEquals("d", testResult.getCompileResult());
    }

    @Test
    public void testChainedMethodCallWithIndexAccess() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent(
                "text = 'hello world test'\n" +
                "&text::split(' ')[1]");
        assertEquals("world", testResult.getInterpretResult());
        assertEquals("world", testResult.getCompileResult());
    }

    @Test
    public void testMultipleMethodCallsWithIndexAccess() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent(
                "text = 'Hello World'\n" +
                "&text::lowercase()::split(' ')[0]");
        assertEquals("hello", testResult.getInterpretResult());
        assertEquals("hello", testResult.getCompileResult());
    }
}
