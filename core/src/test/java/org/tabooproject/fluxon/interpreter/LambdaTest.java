package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.FunctionContextPool;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Operations;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

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

    /**
     * 测试逃逸后的捕获型 lambda 作为集合扩展回调时仍使用定义时环境。
     */
    @Test
    public void testEscapedLambdaKeepsCaptureThroughIterableExtension() {
        String script = ""
                + "def greaterThan(base) = {\n"
                + "  |it| &it > &base\n"
                + "}\n"
                + "predicate = greaterThan(2)\n"
                + "numbers = [1, 2, 3, 4]\n"
                + "&numbers::filter(&predicate)";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(script);
        assertEquals(Arrays.asList(3, 4), result.getInterpretResult());
        assertTrue(result.isMatch(), "Interpret: " + result.getInterpretResult() + ", Compile: " + result.getCompileResult());
    }

    /**
     * 测试二级逃逸 lambda 的捕获隔离。
     * 外层和内层都复用同一段语法时，不能互相覆盖定义时环境。
     */
    @Test
    public void testNestedEscapedLambdaKeepsIndependentCapturedFrames() {
        String script = ""
                + "def makeOuter(prefix) = {\n"
                + "  |seed| {\n"
                + "    local = &prefix + ':' + &seed\n"
                + "    |value| &local + ':' + &value\n"
                + "  }\n"
                + "}\n"
                + "outerA = makeOuter('A')\n"
                + "outerB = makeOuter('B')\n"
                + "innerA1 = call(&outerA, [1])\n"
                + "innerA2 = call(&outerA, [2])\n"
                + "innerB = call(&outerB, [3])\n"
                + "[call(&innerA1, [10]), call(&innerA2, [20]), call(&innerB, [30]), call(&innerA1, [40])]";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(script);
        assertEquals(Arrays.asList("A:1:10", "A:2:20", "B:3:30", "A:1:40"), result.getInterpretResult());
        assertTrue(result.isMatch(), "Interpret: " + result.getInterpretResult() + ", Compile: " + result.getCompileResult());
    }

    /**
     * 测试函数内的非捕获 lambda 不会误判为捕获父变量。
     */
    @Test
    public void testNonCapturingLambdaDoesNotDisableParentEnvFree() {
        ParsedScript script = Fluxon.parse("def apply(value) { callback = |it| &it + 1; &value + 1 }\napply(1)");
        FunctionDefinition definition = findFunction(script, "apply");
        assertTrue(!definition.hasVariablesCapturedByChildren(), "非捕获 lambda 不应该禁用父函数 env-free");
    }

    /**
     * 与 await ext(..., |_, entityId, sourceId| ...) 同形的 lambda：多参、忽略首参、多行 || 条件
     */
    @Test
    public void testAwaitStyleMultiParamLambdaParseAndPredicate() {
        String lambdaBody =
                "|_, entityId, sourceId|\n"
                        + "    &sourceId == 'source_a_1' || &sourceId == 'source_a_2' ||\n"
                        + "    &sourceId == 'source_a_3' || &sourceId == 'source_a_4' ||\n"
                        + "    &sourceId == 'source_a_5' || &sourceId == 'source_a_6'";
        ensureStubAsyncPredicateRegistered();
        String snippet = "async def flow() = await stub_async_predicate(" + lambdaBody + ")";
        assertNotNull(Fluxon.parse(snippet));
        assertNotNull(Fluxon.parse("await stub_async_predicate(" + lambdaBody + ")"));
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "stub_async_predicate(" + lambdaBody + ")",
                "TestAwaitStyleMultiParamLambda"
        );
        assertTrue(result.isMatch());
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    private static final AtomicBoolean stubAsyncPredicateRegistered = new AtomicBoolean(false);

    private static void ensureStubAsyncPredicateRegistered() {
        if (!stubAsyncPredicateRegistered.compareAndSet(false, true)) {
            return;
        }
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerFunction(
                "stub_async_predicate",
                returns(Type.Z).params(Function.TYPE),
                context -> {
                    Function predicate = (Function) context.getRef(0);
                    FunctionContextPool pool = context.getPool();
                    FunctionContext<?> seed = pool.borrow(
                            context.getFunction(),
                            null,
                            0,
                            context.getEnvironment()
                    );
                    try (FunctionContext<?> lambdaCtx = pool.borrowCopy(seed, null)) {
                        lambdaCtx.updateRefs(0, "entity_placeholder", "source_a_3");
                        predicate.call(lambdaCtx);
                        context.setReturnBool(Operations.isTrue(lambdaCtx.getReturnRef()));
                    }
                }
        );
    }

    @Test
    public void testCapturingLambdaStillDisablesParentEnvFree() {
        ParsedScript script = Fluxon.parse("def apply(value) { callback = |it| &it + &value; &value + 1 }\napply(1)");
        FunctionDefinition definition = findFunction(script, "apply");
        assertTrue(definition.hasVariablesCapturedByChildren(), "捕获父变量的 lambda 必须禁用父函数 env-free");
    }

    private FunctionDefinition findFunction(ParsedScript script, String name) {
        for (ParseResult result : script.getResults()) {
            if (result instanceof FunctionDefinition) {
                FunctionDefinition definition = (FunctionDefinition) result;
                if (name.equals(definition.getName())) return definition;
            }
        }
        throw new AssertionError("Function not found: " + name);
    }
}
