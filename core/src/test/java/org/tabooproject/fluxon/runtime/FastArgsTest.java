package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

/**
 * 函数调用与参数一致性测试
 */
public class FastArgsTest {

    /**
     * 测试 FunctionContext 的基本参数访问
     */
    @Test
    void testFunctionContextBasicAccess() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        Environment environment = runtime.newEnvironment();
        Function function = new NativeFunction<>("testBasic", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT, Type.OBJECT, Type.OBJECT), ctx -> {
            assertEquals(4, ctx.getArgumentCount());
            assertEquals("a", ctx.getRef(0));
            assertEquals("b", ctx.getRef(1));
            assertEquals("c", ctx.getRef(2));
            assertEquals("d", ctx.getRef(3));
            ctx.setReturnRef("ok");
        });

        FunctionContextPool pool = FunctionContextPool.local();
        try (FunctionContext<?> context = pool.borrow(function, null, new Object[]{"a", "b", "c", "d"}, environment)) {
            function.call(context);
            assertEquals("ok", context.getReturnRef());
        }
    }

    /**
     * 测试空参数
     */
    @Test
    void testZeroArgs() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        Environment environment = runtime.newEnvironment();
        Function function = new NativeFunction<>("testZeroArgs", returns(Type.OBJECT).noParams(), ctx -> {
            assertEquals(0, ctx.getArgumentCount());
            ctx.setReturnRef("ok");
        });

        FunctionContextPool pool = FunctionContextPool.local();
        try (FunctionContext<?> context = pool.borrow(function, null, new Object[0], environment)) {
            function.call(context);
            assertEquals("ok", context.getReturnRef());
        }
    }

    /**
     * 测试同步 NativeFunction 调用
     */
    @Test
    void testCallFunctionWithNativeFunction() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerFunction("callFuncTest", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT, Type.OBJECT), ctx -> {
            ctx.setReturnRef(ctx.getRef(0) + "-" + ctx.getRef(1) + "-" + ctx.getRef(2));
        });

        Environment environment = runtime.newEnvironment();
        Function function = environment.getFunction("callFuncTest");
        FunctionContextPool pool = FunctionContextPool.local();
        try (FunctionContext<?> ctx = pool.borrow(function, null, new Object[]{"a", "b", "c"}, environment)) {
            function.call(ctx);
            assertEquals("a-b-c", ctx.getReturnRef());
        }
    }

    /**
     * 测试异步函数调用
     */
    @Test
    void testCallFunctionWithAsyncFunction() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerAsyncFunction("asyncCallFuncTest", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT), ctx -> {
            ctx.setReturnRef(ctx.getRef(0) + "+" + ctx.getRef(1));
        });

        Environment environment = runtime.newEnvironment();
        Function function = environment.getFunction("asyncCallFuncTest");
        FunctionContextPool pool = FunctionContextPool.local();
        FunctionContext<?> ctx = pool.borrow(function, null, new Object[]{"x", "y"}, environment);
        Object result = Intrinsics.finishCall(ctx);
        Object awaited = Intrinsics.awaitValue(result);
        assertEquals("x+y", awaited);
    }

    /**
     * 测试 UserFunction 调用
     */
    @Test
    void testCallFunctionWithUserFunction() throws Exception {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def add(a, b) = &a + &b; add(10, 20)"
        );
        assertEquals(30, result.getInterpretResult());
    }

    /**
     * 测试参数求值顺序（应为从左到右）
     */
    @Test
    void testArgumentEvaluationOrder() throws Exception {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "order = []; " +
                "def track(x) = { &order += &x; &x }; " +
                "def collect(a, b, c) = [&a, &b, &c]; " +
                "collect(track(1), track(2), track(3)); " +
                "&order"
        );
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
    }

    /**
     * 测试解释器与编译器结果一致性
     */
    @Test
    void testInterpretCompileConsistency() throws Exception {
        String[] scripts = {
                "def f0 = 42; f0()",
                "def f1(a) = &a * 2; f1(5)",
                "def f2(a, b) = &a + &b; f2(3, 4)",
                "def f3(a, b, c) = &a * &b + &c; f3(2, 3, 4)",
                "def f4(a, b, c, d) = &a + &b + &c + &d; f4(1, 2, 3, 4)",
        };
        for (String script : scripts) {
            FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(script);
            assertEquals(result.getInterpretResult(), result.getCompileResult(), "Interpret and compile should produce same result for: " + script);
        }
    }

    /**
     * 测试 Type.F 参数的 getFloat 是否正确获取值
     */
    @Test
    void testFloatParameterGetFloat() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerFunction("floatTest", returns(Type.D).params(Type.F), ctx -> {
            float f = ctx.getFloat(0);
            ctx.setReturnDouble(f);
        });
        // double 字面量 → Type.F
        FluxonTestUtil.TestResult r1 = FluxonTestUtil.runSilent("floatTest(1.5)");
        assertEquals(1.5, ((Number) r1.getInterpretResult()).doubleValue(), 0.01, "interpret: getFloat(1.5)");
        assertEquals(1.5, ((Number) r1.getCompileResult()).doubleValue(), 0.01, "compile: getFloat(1.5)");
        // 零值
        FluxonTestUtil.TestResult r2 = FluxonTestUtil.runSilent("floatTest(0.0)");
        assertEquals(0.0, ((Number) r2.getInterpretResult()).doubleValue(), 0.01, "interpret: getFloat(0.0)");
        assertEquals(0.0, ((Number) r2.getCompileResult()).doubleValue(), 0.01, "compile: getFloat(0.0)");
        // int 字面量 → Type.F
        FluxonTestUtil.TestResult r3 = FluxonTestUtil.runSilent("floatTest(42)");
        assertEquals(42.0, ((Number) r3.getInterpretResult()).doubleValue(), 0.01, "interpret: getFloat(42)");
        assertEquals(42.0, ((Number) r3.getCompileResult()).doubleValue(), 0.01, "compile: getFloat(42)");
        // 负值
        FluxonTestUtil.TestResult r4 = FluxonTestUtil.runSilent("floatTest(-3.14)");
        assertEquals(-3.14, ((Number) r4.getInterpretResult()).doubleValue(), 0.01, "interpret: getFloat(-3.14)");
        assertEquals(-3.14, ((Number) r4.getCompileResult()).doubleValue(), 0.01, "compile: getFloat(-3.14)");
        // 表达式传参
        FluxonTestUtil.TestResult r5 = FluxonTestUtil.runSilent("floatTest(1.0 + 2.5)");
        assertEquals(3.5, ((Number) r5.getInterpretResult()).doubleValue(), 0.01, "interpret: getFloat(1.0+2.5)");
        assertEquals(3.5, ((Number) r5.getCompileResult()).doubleValue(), 0.01, "compile: getFloat(1.0+2.5)");
        // 变量传参
        FluxonTestUtil.TestResult r6 = FluxonTestUtil.runSilent("x = 7.7; floatTest(&x)");
        assertEquals(7.7, ((Number) r6.getInterpretResult()).doubleValue(), 0.1, "interpret: getFloat(var)");
        assertEquals(7.7, ((Number) r6.getCompileResult()).doubleValue(), 0.1, "compile: getFloat(var)");
        // 用户函数转发参数
        FluxonTestUtil.TestResult r7 = FluxonTestUtil.runSilent("def test(number) { floatTest(&number) }; test(1.0)");
        assertEquals(1.0, ((Number) r7.getInterpretResult()).doubleValue(), 0.01, "interpret: getFloat via def");
        assertEquals(1.0, ((Number) r7.getCompileResult()).doubleValue(), 0.01, "compile: getFloat via def");
        FluxonTestUtil.TestResult r8 = FluxonTestUtil.runSilent("def test(number) { floatTest(&number) }; test(3.14)");
        assertEquals(3.14, ((Number) r8.getInterpretResult()).doubleValue(), 0.01, "interpret: getFloat via def 3.14");
        assertEquals(3.14, ((Number) r8.getCompileResult()).doubleValue(), 0.01, "compile: getFloat via def 3.14");
        FluxonTestUtil.TestResult r9 = FluxonTestUtil.runSilent("def test(number) { floatTest(&number) }; test(42)");
        assertEquals(42.0, ((Number) r9.getInterpretResult()).doubleValue(), 0.01, "interpret: getFloat via def int");
        assertEquals(42.0, ((Number) r9.getCompileResult()).doubleValue(), 0.01, "compile: getFloat via def int");
    }

    /**
     * 测试 def 带类型注解 float 的参数
     */
    @Test
    void testFloatTypedParameter() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerFunction("floatTest2", returns(Type.D).params(Type.F), ctx -> {
            float f = ctx.getFloat(0);
            ctx.setReturnDouble(f);
        });
        // def test(number: float) 类型注解
        FluxonTestUtil.TestResult r1 = FluxonTestUtil.runSilent("def test(number: float) { floatTest2(&number) }; test(1.5)");
        assertEquals(1.5, ((Number) r1.getInterpretResult()).doubleValue(), 0.01, "interpret: typed float 1.5");
        assertEquals(1.5, ((Number) r1.getCompileResult()).doubleValue(), 0.01, "compile: typed float 1.5");
        FluxonTestUtil.TestResult r2 = FluxonTestUtil.runSilent("def test(number: float) { floatTest2(&number) }; test(42)");
        assertEquals(42.0, ((Number) r2.getInterpretResult()).doubleValue(), 0.01, "interpret: typed float from int");
        assertEquals(42.0, ((Number) r2.getCompileResult()).doubleValue(), 0.01, "compile: typed float from int");
    }
}
