package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * fast-args 路径的回归测试与参数一致性测试
 */
public class FastArgsTest {

    /**
     * 测试 FunctionContext 的 inline 模式基本功能
     */
    @Test
    void testFunctionContextInlineMode() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        Environment environment = runtime.newEnvironment();
        Function function = new NativeFunction<>(new SymbolFunction(null, "testInline", 4), ctx -> {
            // 验证 inline 模式下的参数访问
            assertEquals(4, ctx.getArgumentCount());
            assertEquals("a", ctx.getArgument(0));
            assertEquals("b", ctx.getArgument(1));
            assertEquals("c", ctx.getArgument(2));
            assertEquals("d", ctx.getArgument(3));
            assertNull(ctx.getArgument(4));
            assertTrue(ctx.hasArgument(0));
            assertTrue(ctx.hasArgument(3));
            assertFalse(ctx.hasArgument(4));
            return "ok";
        });

        FunctionContextPool pool = FunctionContextPool.local();
        FunctionContext<?> context = pool.borrowInline(function, null, 4, "a", "b", "c", "d", environment);
        try {
            Object result = function.call(context);
            assertEquals("ok", result);
        } finally {
            pool.release(context);
        }
    }

    /**
     * 测试 FunctionContext 的 getArguments() 惰性物化
     */
    @Test
    void testFunctionContextLazyMaterialization() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        Environment environment = runtime.newEnvironment();
        AtomicReference<Object[]> capturedArgs = new AtomicReference<>();

        Function function = new NativeFunction<>(new SymbolFunction(null, "testMaterialize", 3), ctx -> {
            // 调用 getArguments() 触发物化
            Object[] args = ctx.getArguments();
            capturedArgs.set(args);
            // 物化后应该切换到数组模式
            assertEquals(3, args.length);
            assertEquals("x", args[0]);
            assertEquals("y", args[1]);
            assertEquals("z", args[2]);
            // 再次调用应该返回同一个数组
            assertSame(args, ctx.getArguments());
            return "ok";
        });

        FunctionContextPool pool = FunctionContextPool.local();
        FunctionContext<?> context = pool.borrowInline(function, null, 3, "x", "y", "z", null, environment);
        try {
            Object result = function.call(context);
            assertEquals("ok", result);
            assertNotNull(capturedArgs.get());
        } finally {
            pool.release(context);
        }
    }

    /**
     * 测试 inline 模式与数组模式的参数一致性
     */
    @Test
    void testInlineVsArrayConsistency() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        Environment environment = runtime.newEnvironment();
        List<Object> inlineResults = new ArrayList<>();
        List<Object> arrayResults = new ArrayList<>();

        Function function = new NativeFunction<>(new SymbolFunction(null, "testConsistency", 4), ctx -> {
            List<Object> results = new ArrayList<>();
            results.add(ctx.getArgumentCount());
            for (int i = 0; i < 5; i++) {
                results.add(ctx.getArgument(i));
            }
            results.add(ctx.hasArgument(0));
            results.add(ctx.hasArgument(3));
            results.add(ctx.hasArgument(4));
            return results;
        });

        FunctionContextPool pool = FunctionContextPool.local();

        // 测试 inline 模式
        FunctionContext<?> inlineCtx = pool.borrowInline(function, null, 4, 1, 2, 3, 4, environment);
        try {
            inlineResults.addAll((List<?>) function.call(inlineCtx));
        } finally {
            pool.release(inlineCtx);
        }

        // 测试数组模式
        FunctionContext<?> arrayCtx = pool.borrow(function, null, new Object[]{1, 2, 3, 4}, environment);
        try {
            arrayResults.addAll((List<?>) function.call(arrayCtx));
        } finally {
            pool.release(arrayCtx);
        }

        // 验证一致性
        assertEquals(inlineResults, arrayResults, "Inline mode and array mode should produce identical results");
    }

    /**
     * 测试空参数的 inline 模式
     */
    @Test
    void testInlineModeZeroArgs() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        Environment environment = runtime.newEnvironment();

        Function function = new NativeFunction<>(new SymbolFunction(null, "testZeroArgs", 0), ctx -> {
            assertEquals(0, ctx.getArgumentCount());
            assertNull(ctx.getArgument(0));
            assertFalse(ctx.hasArgument(0));
            Object[] args = ctx.getArguments();
            assertEquals(0, args.length);
            return "ok";
        });

        FunctionContextPool pool = FunctionContextPool.local();
        FunctionContext<?> context = pool.borrowInline(function, null, 0, null, null, null, null, environment);
        try {
            Object result = function.call(context);
            assertEquals("ok", result);
        } finally {
            pool.release(context);
        }
    }

    /**
     * 测试 Intrinsics.callFunctionFastArgs 对同步 NativeFunction 的处理
     */
    @Test
    void testCallFunctionFastArgsWithNativeFunction() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerFunction("fastArgsTest", 3, ctx -> {
            return ctx.getArgument(0) + "-" + ctx.getArgument(1) + "-" + ctx.getArgument(2);
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
            return ctx.getArgument(0) + "+" + ctx.getArgument(1);
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
        // 使用内置函数测试（NativeFunction）
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerFunction("fastSum", 4, ctx -> {
            int sum = 0;
            for (int i = 0; i < ctx.getArgumentCount(); i++) {
                Object arg = ctx.getArgument(i);
                if (arg instanceof Number) {
                    sum += ((Number) arg).intValue();
                }
            }
            return sum;
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
