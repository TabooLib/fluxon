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
}
