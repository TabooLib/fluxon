package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * fast-args 路径的回归测试与参数一致性测试
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
     * 测试 Intrinsics.callFunctionFastArgs 对同步 NativeFunction 的处理
     */
    @Test
    void testCallFunctionFastArgsWithNativeFunction() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerFunction("fastArgsTest", 3, ctx -> {
            ctx.setReturnRef(ctx.getRef(0) + "-" + ctx.getRef(1) + "-" + ctx.getRef(2));
        });

        Environment environment = runtime.newEnvironment();
        Object result = Intrinsics.callFunctionFastArgs(
                FunctionContextPool.local(),
                environment, "fastArgsTest", 3,
                "a", "b", "c", null,
                -1, -1
        );
        assertEquals("a-b-c", result);
    }

    /**
     * 测试 Intrinsics.callFunctionFastArgs 对异步函数的回退
     */
    @Test
    void testCallFunctionFastArgsWithAsyncFunction() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerAsyncFunction("asyncFastArgsTest", 2, ctx -> {
            ctx.setReturnRef(ctx.getRef(0) + "+" + ctx.getRef(1));
        });

        Environment environment = runtime.newEnvironment();
        Object result = Intrinsics.callFunctionFastArgs(
                FunctionContextPool.local(),
                environment, "asyncFastArgsTest", 2,
                "x", "y", null, null,
                -1, -1
        );
        // 异步函数返回 CompletableFuture，需要 await
        Object awaited = Intrinsics.awaitValue(result);
        assertEquals("x+y", awaited);
    }

    /**
     * 测试 Intrinsics.callFunctionFastArgs 对 UserFunction 的回退
     */
    @Test
    void testCallFunctionFastArgsWithUserFunction() throws Exception {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def add(a, b) = &a + &b; add(10, 20)"
        );
        assertEquals(30, result.getInterpretResult());
    }

    /**
     * 测试 fast-args 开关开启时的行为
     */
    @Test
    void testFastArgsEnabled() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerFunction("fastSum", 4, ctx -> {
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

        // 测试 0-4 个参数
        assertEquals(0, Intrinsics.callFunctionFastArgs(pool, environment, "fastSum", 0, null, null, null, null, -1, -1));
        assertEquals(1, Intrinsics.callFunctionFastArgs(pool, environment, "fastSum", 1, 1, null, null, null, -1, -1));
        assertEquals(3, Intrinsics.callFunctionFastArgs(pool, environment, "fastSum", 2, 1, 2, null, null, -1, -1));
        assertEquals(6, Intrinsics.callFunctionFastArgs(pool, environment, "fastSum", 3, 1, 2, 3, null, -1, -1));
        assertEquals(10, Intrinsics.callFunctionFastArgs(pool, environment, "fastSum", 4, 1, 2, 3, 4, -1, -1));
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
        // 验证参数按顺序求值
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
