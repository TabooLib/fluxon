package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LambdaTest {

    @Test
    public void testSimpleLambdaCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("inc = |x| &x + 1; call(&inc, [5])");
        assertEquals(6, result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    public void testZeroArgumentLambda() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("producer = || 42; call(&producer)");
        assertEquals(42, result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    public void testImplicitItParameter() {
        // || 语法自动绑定第一个参数到 it
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("inc = || &it + 1; call(&inc, [5])");
        assertEquals(6, result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    public void testImplicitItWithMap() {
        // 使用 it 进行 map 操作
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("list = [1, 2, 3]; doubled = &list::map(|| &it * 2); &doubled");
        assertEquals(Arrays.asList(2, 4, 6), result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    public void testImplicitItWithMemberAccess() {
        // 测试 it 的成员访问 (模拟 each(|| &it.name) 场景)
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("list = ['hello', 'world']; lengths = &list::map(|| &it::length()); &lengths");
        assertEquals(Arrays.asList(5, 5), result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    public void testLambdaWithIterableMap() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("list = [1, 2, 3]; doubled = &list::map(|x| &x * 2); &doubled");
        assertEquals(Arrays.asList(2, 4, 6), result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    public void testLambdaCaptureOuterVariable() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "outer = 5; adder = |x| &x + &outer; outer = 7; call(&adder, [3])");
        assertEquals(10, result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    public void testNestedLambdaFactory() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "n = 10; makeAdder = |x| &x + &n; add10 = &makeAdder; call(&add10, [2])");
        assertEquals(12, result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    @Test
    public void testLambdaEachCountInsideFunction() {
        String script = ""
                + "def countAll(list) = {\n"
                + "  counter = 0\n"
                + "  &list::each(|item| { print('counter: ' + &counter); counter += 1 })\n"
                + "  &counter\n"
                + "}\n"
                + "numbers = [1,2,3,4]; countAll(&numbers)";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(script);
        assertEquals(4, result.getInterpretResult());
        assertTrue(result.isMatch());
    }

    /**
     * 测试 lambda 捕获函数参数
     * 复现问题：函数参数在 lambda 内部被错误地解析为其他值
     */
    @Test
    public void testLambdaCaptureFunctionParameter() {
        // 函数参数被 lambda 捕获（注意：变量取值必须使用 &name）
        String script = ""
                + "def testCapture(shooter) = {\n"
                + "  list = [1, 2, 3]\n"
                + "  print('shooter before each: ' + &shooter)\n"
                + "  &list::filter(|it| {\n"
                + "    print('it: ' + &it + ', shooter: ' + &shooter)\n"
                + "    &it != &shooter\n"
                + "  })\n"
                + "}\n"
                + "testCapture(2)";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(script);
        // 预期结果：[1, 3]（过滤掉 shooter=2）
        assertEquals(Arrays.asList(1, 3), result.getInterpretResult());
        assertTrue(result.isMatch(), "Interpret: " + result.getInterpretResult() + ", Compile: " + result.getCompileResult());
    }

    /**
     * 测试 lambda 中多次访问捕获的函数参数
     * 确保捕获的值在多次访问时保持一致
     */
    @Test
    public void testLambdaCaptureFunctionParameterMultipleAccess() {
        String script = ""
                + "def multiAccess(value) = {\n"
                + "  list = [1, 2, 3, 4, 5]\n"
                + "  count = 0\n"
                + "  &list::each(|it| {\n"
                + "    if &it == &value {\n"
                + "      count += 1\n"
                + "    }\n"
                + "  })\n"
                + "  &count\n"
                + "}\n"
                + "multiAccess(3)";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(script);
        // 预期结果：1（只有 3 匹配）
        assertEquals(1, result.getInterpretResult());
        assertTrue(result.isMatch(), "Interpret: " + result.getInterpretResult() + ", Compile: " + result.getCompileResult());
    }

    /**
     * 测试嵌套 lambda 捕获外部函数参数
     */
    @Test
    public void testNestedLambdaCaptureFunctionParameter() {
        String script = ""
                + "def outerFunc(target) = {\n"
                + "  list1 = [1, 2]\n"
                + "  count = 0\n"
                + "  &list1::each(|x| {\n"
                + "    list2 = [3, 4]\n"
                + "    &list2::each(|y| {\n"
                + "      sum = &x + &y\n"
                + "      if &sum != &target {\n"
                + "        count += 1\n"
                + "      }\n"
                + "    })\n"
                + "  })\n"
                + "  &count\n"
                + "}\n"
                + "outerFunc(5)";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(script);
        // 1+3=4, 1+4=5(skip), 2+3=5(skip), 2+4=6 -> count = 2
        assertEquals(2, result.getInterpretResult());
        assertTrue(result.isMatch(), "Interpret: " + result.getInterpretResult() + ", Compile: " + result.getCompileResult());
    }

    /**
     * 测试同一个 lambda 语法在多个函数调用帧中逃逸后的捕获隔离。
     * 这会压住 lambda 缓存复用时错误共享最后一次父环境的问题。
     */
    @Test
    @Disabled("已知缺陷：捕获型 lambda 逃逸后还没有绑定定义时 Environment")
    public void testEscapedLambdaFactoryKeepsIndependentCapturedFrames() {
        String script = ""
                + "def makeCombiner(prefix, offset) = {\n"
                + "  local = &prefix + ':' + &offset\n"
                + "  |value| &local + ':' + &value\n"
                + "}\n"
                + "first = makeCombiner('A', 1)\n"
                + "second = makeCombiner('B', 2)\n"
                + "[call(&first, [10]), call(&second, [20]), call(&first, [30]), call(&second, [40])]";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(script);
        assertEquals(Arrays.asList("A:1:10", "B:2:20", "A:1:30", "B:2:40"), result.getInterpretResult());
        assertTrue(result.isMatch(), "Interpret: " + result.getInterpretResult() + ", Compile: " + result.getCompileResult());
    }
}
