package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.junit.jupiter.api.Assertions.*;

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
        Function function = new NativeFunction<>(new SymbolFunction(null, "testBasic", 4), ctx -> {
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
        Function function = new NativeFunction<>(new SymbolFunction(null, "testZeroArgs", 0), ctx -> {
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
     * 测试 Intrinsics.callFunction 对同步 NativeFunction 的处理
     */
    @Test
    void testCallFunctionWithNativeFunction() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerFunction("callFuncTest", 3, ctx -> {
            ctx.setReturnRef(ctx.getRef(0) + "-" + ctx.getRef(1) + "-" + ctx.getRef(2));
        });

        Environment environment = runtime.newEnvironment();
        Object result = Intrinsics.callFunction(
                FunctionContextPool.local(),
                environment, "callFuncTest",
                new Object[]{"a", "b", "c"},
                -1, -1
        );
        assertEquals("a-b-c", result);
    }

    /**
     * 测试 Intrinsics.callFunction 对异步函数的处理
     */
    @Test
    void testCallFunctionWithAsyncFunction() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerAsyncFunction("asyncCallFuncTest", 2, ctx -> {
            ctx.setReturnRef(ctx.getRef(0) + "+" + ctx.getRef(1));
        });

        Environment environment = runtime.newEnvironment();
        Object result = Intrinsics.callFunction(
                FunctionContextPool.local(),
                environment, "asyncCallFuncTest",
                new Object[]{"x", "y"},
                -1, -1
        );
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
     * 测试不同参数数量 0-4
     */
    @Test
    void testVariableArgCounts() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerFunction("varArgSum", 4, ctx -> {
            int sum = 0;
            for (int i = 0; i < ctx.getArgumentCount(); i++) {
                Object arg = ctx.getRef(i);
                if (arg instanceof Number) {
                    sum += ((Number) arg).intValue();
                }
            }
            ctx.setReturnRef(sum);
        });

        Environment environment = runtime.newEnvironment();
        FunctionContextPool pool = FunctionContextPool.local();

        assertEquals(0, Intrinsics.callFunction(pool, environment, "varArgSum", new Object[0], -1, -1));
        assertEquals(1, Intrinsics.callFunction(pool, environment, "varArgSum", new Object[]{1}, -1, -1));
        assertEquals(3, Intrinsics.callFunction(pool, environment, "varArgSum", new Object[]{1, 2}, -1, -1));
        assertEquals(6, Intrinsics.callFunction(pool, environment, "varArgSum", new Object[]{1, 2, 3}, -1, -1));
        assertEquals(10, Intrinsics.callFunction(pool, environment, "varArgSum", new Object[]{1, 2, 3, 4}, -1, -1));
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
}
