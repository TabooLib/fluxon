package org.tabooproject.fluxon.interpreter;

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
}
