package org.tabooproject.fluxon.compiler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.type.TestRuntime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 参数绑定测试
 *
 * @author sky
 */
public class ParameterBindingTest {

    @BeforeEach
    public void setup() {
        TestRuntime.registerTestFunctions();
    }

    @Test
    public void testDefineParameter() {
        CompilationContext ctx = new CompilationContext("&x + 1");
        ctx.defineParameter("x", double.class);
        assertEquals(0, ctx.getParameterIndex("x"));
        assertEquals(1, ctx.getParameterCount());
        assertEquals(Type.D, ctx.getParameters().get("x").getType());
    }

    @Test
    public void testDefineMultipleParameters() {
        CompilationContext ctx = new CompilationContext("&x + &y");
        ctx.defineParameter("x", double.class);
        ctx.defineParameter("y", long.class);
        assertEquals(0, ctx.getParameterIndex("x"));
        assertEquals(1, ctx.getParameterIndex("y"));
        assertEquals(2, ctx.getParameterCount());
    }

    @Test
    public void testDuplicateParameterThrows() {
        CompilationContext ctx = new CompilationContext("&x");
        ctx.defineParameter("x", double.class);
        assertThrows(IllegalArgumentException.class, () -> ctx.defineParameter("x", int.class));
    }

    @Test
    public void testEvalWithDoubleParameter() {
        CompilationContext ctx = new CompilationContext("(10 * &x) + 5");
        ctx.defineParameter("x", double.class);
        ParsedScript script = Fluxon.parse(ctx);
        Object result = script.eval(env -> env.setParameter("x", 5.0));
        assertEquals(55.0, result);
    }

    @Test
    public void testEvalWithLongParameter() {
        CompilationContext ctx = new CompilationContext("&x * 2");
        ctx.defineParameter("x", long.class);
        ParsedScript script = Fluxon.parse(ctx);
        Object result = script.eval(env -> env.setParameter("x", 100L));
        assertEquals(200L, result);
    }

    @Test
    public void testEvalWithObjectParameter() {
        CompilationContext ctx = new CompilationContext("&name");
        ctx.defineParameter("name", String.class);
        ParsedScript script = Fluxon.parse(ctx);
        Object result = script.eval(env -> env.setParameter("name", "hello"));
        assertEquals("hello", result);
    }

    @Test
    public void testMultipleParameterEval() {
        CompilationContext ctx = new CompilationContext("&a + &b + &c");
        ctx.defineParameter("a", double.class);
        ctx.defineParameter("b", double.class);
        ctx.defineParameter("c", double.class);
        ParsedScript script = Fluxon.parse(ctx);
        Object result = script.eval(env -> {
            env.setParameter("a", 1.0);
            env.setParameter("b", 2.0);
            env.setParameter("c", 3.0);
        });
        assertEquals(6.0, result);
    }

    @Test
    public void testReuseEnvironment() {
        CompilationContext ctx = new CompilationContext("&x * &x");
        ctx.defineParameter("x", double.class);
        ParsedScript script = Fluxon.parse(ctx);
        Environment env = script.newEnvironment();
        env.setParameter("x", 3.0);
        assertEquals(9.0, script.eval(env));
        env.setParameter("x", 5.0);
        assertEquals(25.0, script.eval(env));
    }

    @Test
    public void testUnknownParameterThrows() {
        CompilationContext ctx = new CompilationContext("&x");
        ctx.defineParameter("x", double.class);
        ParsedScript script = Fluxon.parse(ctx);
        Environment env = script.newEnvironment();
        assertThrows(IllegalArgumentException.class, () -> env.setParameter("unknown", 1.0));
    }

    @Test
    public void testNoParametersThrows() {
        CompilationContext ctx = new CompilationContext("1 + 2");
        ParsedScript script = Fluxon.parse(ctx);
        Environment env = script.newEnvironment();
        assertThrows(IllegalStateException.class, () -> env.setParameter("x", 1.0));
    }

    @Test
    public void testTypeAnalyzerReceivesParameterTypes() {
        CompilationContext ctx = new CompilationContext("&x + 1.0");
        ctx.defineParameter("x", double.class);
        ParsedScript script = Fluxon.parse(ctx);
        @SuppressWarnings("unchecked")
        java.util.Map<Integer, Type> types = ctx.getAttribute("variableTypes");
        assertNotNull(types);
        assertEquals(Type.D, types.get(0));
    }

    @Test
    public void testCompileWithParameter() {
        CompilationContext ctx = new CompilationContext("&x * 10");
        ctx.defineParameter("x", double.class);
        CompileResult result = Fluxon.compile(
                org.tabooproject.fluxon.runtime.FluxonRuntime.getInstance().newEnvironment(),
                ctx,
                "ParamTest"
        );
        assertNotNull(result.getMainClass());
    }
}
