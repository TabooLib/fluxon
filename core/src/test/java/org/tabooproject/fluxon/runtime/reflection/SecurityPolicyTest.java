package org.tabooproject.fluxon.runtime.reflection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecurityPolicy 安全策略测试
 *
 * @author sky
 */
public class SecurityPolicyTest {

    @AfterEach
    public void tearDown() {
        FluxonRuntime.getInstance().setSecurityPolicy(SecurityPolicy.ALLOW_ALL);
    }

    @Test
    public void testDefaultAllowAll() {
        SecurityPolicy policy = FluxonRuntime.getInstance().getSecurityPolicy();
        assertTrue(policy.isClassAllowed(Runtime.class));
        assertTrue(policy.isMemberAllowed(Runtime.class, "exec"));
        assertTrue(policy.isClassAllowed(String.class));
        assertTrue(policy.isMemberAllowed(String.class, "length"));
    }

    @Test
    public void testSetNullFallbackToAllowAll() {
        FluxonRuntime.getInstance().setSecurityPolicy(null);
        SecurityPolicy policy = FluxonRuntime.getInstance().getSecurityPolicy();
        assertSame(SecurityPolicy.ALLOW_ALL, policy);
    }

    @Test
    public void testBlockClass() {
        FluxonRuntime.getInstance().setSecurityPolicy(new SecurityPolicy() {
            @Override
            public boolean isClassAllowed(Class<?> clazz) {
                return clazz != Runtime.class;
            }
            @Override
            public boolean isMemberAllowed(Class<?> clazz, String memberName) {
                return true;
            }
        });
        SecurityException ex = assertThrows(SecurityException.class, () -> {
            evalWithReflection("static java.lang.Runtime.getRuntime()");
        });
        assertTrue(ex.getMessage().contains("java.lang.Runtime"));
    }

    @Test
    public void testBlockMember() {
        FluxonRuntime.getInstance().setSecurityPolicy(new SecurityPolicy() {
            @Override
            public boolean isClassAllowed(Class<?> clazz) {
                return true;
            }
            @Override
            public boolean isMemberAllowed(Class<?> clazz, String memberName) {
                return !(clazz == System.class && memberName.equals("exit"));
            }
        });
        // 允许访问 System 的其他成员
        assertNotNull(evalWithReflection("static java.lang.System.currentTimeMillis()"));
        // 阻止 System.exit
        SecurityException ex = assertThrows(SecurityException.class, () -> {
            evalWithReflection("static java.lang.System.exit(0)");
        });
        assertTrue(ex.getMessage().contains("exit"));
    }

    @Test
    public void testAllowedClassPassesThrough() {
        FluxonRuntime.getInstance().setSecurityPolicy(new SecurityPolicy() {
            @Override
            public boolean isClassAllowed(Class<?> clazz) {
                return clazz != Runtime.class;
            }
            @Override
            public boolean isMemberAllowed(Class<?> clazz, String memberName) {
                return true;
            }
        });
        Object result = evalWithReflection("static java.lang.Integer.parseInt('42')");
        assertEquals(42, result);
    }

    @Test
    public void testBlockStaticFieldAccess() {
        FluxonRuntime.getInstance().setSecurityPolicy(new SecurityPolicy() {
            @Override
            public boolean isClassAllowed(Class<?> clazz) {
                return true;
            }
            @Override
            public boolean isMemberAllowed(Class<?> clazz, String memberName) {
                return !(clazz == Integer.class && memberName.equals("MAX_VALUE"));
            }
        });
        SecurityException ex = assertThrows(SecurityException.class, () -> {
            evalWithReflection("static java.lang.Integer.MAX_VALUE");
        });
        assertTrue(ex.getMessage().contains("MAX_VALUE"));
    }

    private Object evalWithReflection(String source) {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        ctx.setAllowJavaConstruction(true);
        ctx.setAllowInvalidReference(true);
        ParsedScript script = Fluxon.parse(ctx, env);
        return script.eval(env);
    }
}
