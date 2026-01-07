package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.parser.DomainRegistry;
import org.tabooproject.fluxon.parser.ParsedScript;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.FluxonTestUtil.assertBothEqual;

/**
 * Domain 语法功能测试
 */
public class DomainTest {

    private DomainRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DomainRegistry();
    }

    @Test
    void testBasicDomainWithBlock() {
        registry.register("run", (env, body) -> body.get());
        assertBothEqual(3, FluxonTestUtil.runSilent(
                "run { 1 + 2 }",
                ctx -> ctx.setDomainRegistry(registry),
                env -> {}
        ));
    }

    @Test
    void testDomainWithExpression() {
        registry.register("run", (env, body) -> body.get());
        assertBothEqual(42, FluxonTestUtil.runSilent(
                "run 42",
                ctx -> ctx.setDomainRegistry(registry),
                env -> {}
        ));
    }

    @Test
    void testDomainReturnValueTransformation() {
        registry.register("double", (env, body) -> {
            Object result = body.get();
            if (result instanceof Number) {
                return ((Number) result).intValue() * 2;
            }
            return result;
        });
        assertBothEqual(42, FluxonTestUtil.runSilent(
                "double { 21 }",
                ctx -> ctx.setDomainRegistry(registry),
                env -> {}
        ));
    }

    @Test
    void testDomainMultipleExecutions() {
        AtomicInteger counter = new AtomicInteger(0);
        registry.register("retry", (env, body) -> {
            int maxAttempts = 3;
            Exception lastException = null;
            for (int i = 0; i < maxAttempts; i++) {
                try {
                    return body.get();
                } catch (Exception e) {
                    lastException = e;
                }
            }
            throw new RuntimeException("All attempts failed", lastException);
        });

        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "retry { failTwice() }",
                ctx -> ctx.setDomainRegistry(registry),
                env -> env.defineRootFunction("failTwice", new NativeFunction<>(null, ctx -> {
                    int count = counter.incrementAndGet();
                    if (count < 3) {
                        throw new RuntimeException("Attempt " + count + " failed");
                    }
                    return "success";
                }))
        );
        assertEquals("success", result.getInterpretResult());
        // 编译模式会创建新的 env，counter 会重置，所以只验证解释模式
    }

    @Test
    void testDomainWithTargetContext() {
        registry.register("withContext", (env, body) -> {
            env.assign("contextValue", "context-value", -1);
            return body.get();
        });
        assertBothEqual("context-value", FluxonTestUtil.runSilent(
                "withContext { &contextValue }",
                ctx -> ctx.setDomainRegistry(registry),
                env -> env.defineRootVariable("contextValue", null)
        ));
    }

    @Test
    void testNestedDomains() {
        registry.register("outer", (env, body) -> {
            env.assign("outerContext", "outer-context", -1);
            return body.get();
        });
        registry.register("inner", (env, body) -> {
            String outerContext = (String) env.get("outerContext", -1);
            env.assign("innerContext", outerContext + ":inner-context", -1);
            return body.get();
        });
        assertBothEqual("outer-context:inner-context", FluxonTestUtil.runSilent(
                "outer { inner { &innerContext } }",
                ctx -> ctx.setDomainRegistry(registry),
                env -> {
                    env.defineRootVariable("outerContext", null);
                    env.defineRootVariable("innerContext", null);
                }
        ));
    }

    @Test
    void testDomainWithVariables() {
        registry.register("run", (env, body) -> body.get());
        assertBothEqual(15, FluxonTestUtil.runSilent(
                "x = 10; run { &x + 5 }",
                ctx -> ctx.setDomainRegistry(registry),
                env -> {}
        ));
    }

    @Test
    void testDomainLazyEvaluation() {
        AtomicInteger counter = new AtomicInteger(0);
        registry.register("lazy", (env, body) -> {
            // 不执行 body，只返回计数
            return counter.get();
        });

        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "lazy { increment() }",
                ctx -> ctx.setDomainRegistry(registry),
                env -> env.defineRootFunction("increment", new NativeFunction<>(null, ctx -> counter.incrementAndGet()))
        );
        assertEquals(0, result.getInterpretResult());
        // 编译模式会创建新的 env，counter 会重置，所以只验证解释模式
    }

    @Test
    void testDomainErrorPropagation() {
        registry.register("run", (env, body) -> body.get());

        CompilationContext context = new CompilationContext("run { throw(\"test error\") }");
        context.setDomainRegistry(registry);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        assertThrows(Exception.class, () -> {
            Fluxon.parse(context, env).eval(env);
        });
    }

    @Test
    void testDomainParsingAst() {
        registry.register("test", (env, body) -> body.get());

        CompilationContext context = new CompilationContext("test { 42 }");
        context.setDomainRegistry(registry);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        ParsedScript script = Fluxon.parse(context, env);

        // 验证脚本可以正确执行，结果为 42
        assertEquals(42, script.eval(env));
    }

    @Test
    void testIndependentRegistries() {
        DomainRegistry registry1 = new DomainRegistry();
        DomainRegistry registry2 = new DomainRegistry();

        registry1.register("foo", (env, body) -> "foo-result");
        registry2.register("bar", (env, body) -> "bar-result");

        assertBothEqual("foo-result", FluxonTestUtil.runSilent(
                "foo { 1 }",
                ctx -> ctx.setDomainRegistry(registry1),
                env -> {}
        ));

        assertBothEqual("bar-result", FluxonTestUtil.runSilent(
                "bar { 1 }",
                ctx -> ctx.setDomainRegistry(registry2),
                env -> {}
        ));

        // 使用 registry1 解析 bar（应该解析为普通标识符）
        CompilationContext ctx3 = new CompilationContext("bar");
        ctx3.setDomainRegistry(registry1);
        ParsedScript script3 = Fluxon.parse(ctx3, FluxonRuntime.getInstance().newEnvironment());
        // bar 不是 domain，会被解析为标识符引用
        assertNotNull(script3);
    }

    @Test
    void testDomainInExpression() {
        registry.register("getValue", (env, body) -> 10);
        assertBothEqual(42, FluxonTestUtil.runSilent(
                "getValue { 0 } + 32",
                ctx -> ctx.setDomainRegistry(registry),
                env -> {}
        ));
    }

    @Test
    void testDomainAlso() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("[] :: also { add(1); add(2); add(3) }");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertTrue(result.isMatch());
    }

    @Test
    void testDomainWith() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("[] :: with { add(1); add(2); add(3) }");
        assertEquals("true", result.getInterpretResult().toString());
        assertTrue(result.isMatch());
    }
}
