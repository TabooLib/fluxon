package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.type.TestObject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 错误处理测试
 * 测试反射访问中的各种错误场景
 *
 * @author sky
 */
@SuppressWarnings("deprecation")
public class ErrorHandlingTest extends MemberAccessTestBase {

    // ========== 特性开关测试 ==========

    @Test
    public void testFeatureFlagDisabled() {
        CompilationContext ctx = new CompilationContext("obj.field");
        ctx.setAllowReflectionAccess(false);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        try {
            Fluxon.parse(ctx, env);
            fail("Should throw exception when reflection access is disabled");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Reflection access is not enabled")
                    || e.getMessage().contains("not enabled"));
        }
    }

    @Test
    public void testFeatureFlagEnabled() {
        CompilationContext ctx = new CompilationContext("text.field");
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        try {
            Fluxon.parse(ctx, env);
            assertTrue(true);
        } catch (Exception e) {
            fail("Should be able to parse with reflection enabled: " + e.getMessage());
        }
    }

    // ========== null 对象访问 ==========

    @Test
    public void testNullObjectFieldAccess() {
        CompilationContext ctx = new CompilationContext("&nullObj.publicField");
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("nullObj", null);
        try {
            Fluxon.parse(ctx, env).eval(env);
            fail("Should throw exception for null object");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("null")
                    || e.getMessage().contains("Null")
                    || e instanceof NullPointerException);
        }
    }

    @Test
    public void testNullObjectMethodCall() {
        CompilationContext ctx = new CompilationContext("&nullObj.getName()");
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("nullObj", null);
        try {
            Fluxon.parse(ctx, env).eval(env);
            fail("Should throw exception for null object method call");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("null")
                    || e.getMessage().contains("Null")
                    || e instanceof NullPointerException);
        }
    }

    @Test
    public void testNullObjectInCompileMode() throws Exception {
        CompilationContext ctx = new CompilationContext("&nullObj.method()");
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("nullObj", null);

        CompileResult compileResult = Fluxon.compile(env, ctx, "TestNullCompile");
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base = (RuntimeScriptBase) scriptClass.newInstance();

        try {
            base.eval(env);
            fail("Should throw exception for null object");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("null")
                    || e.getMessage().contains("Null")
                    || e.getCause() instanceof NullPointerException);
        }
    }

    // ========== 不存在的成员 ==========

    @Test
    public void testNonExistentField() {
        try {
            interpret("&obj.nonExistentField");
            fail("Should throw exception for non-existent field");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("nonExistent")
                    || e.getMessage().contains("not found")
                    || e.getMessage().contains("Member"));
        }
    }

    @Test
    public void testNonExistentMethod() {
        try {
            interpret("&obj.nonExistentMethod()");
            fail("Should throw exception for non-existent method");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("nonExistent")
                    || e.getMessage().contains("not found")
                    || e.getMessage().contains("Member"));
        }
    }

    @Test
    public void testNonExistentMethodInCompileMode() throws Exception {
        try {
            compile("&obj.nonExistentMethod()", "TestNonExistMethod");
            fail("Should throw exception for non-existent method");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("nonExistentMethod")
                    || e.getMessage().contains("not found")
                    || e.getMessage().contains("Member"));
        }
    }

    // ========== 私有成员访问 ==========

    @Test
    public void testPrivateFieldAccess() {
        try {
            interpret("&obj.privateField");
            fail("Should throw exception for private field access");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("private")
                    || e.getMessage().contains("not found")
                    || e.getMessage().contains("access")
                    || e.getMessage().contains("Member"));
        }
    }

    @Test
    public void testProtectedFieldAccess() {
        try {
            interpret("&obj.protectedField");
            fail("Should throw exception for protected field access");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("protected")
                    || e.getMessage().contains("not found")
                    || e.getMessage().contains("access")
                    || e.getMessage().contains("Member"));
        }
    }

    // ========== 参数不匹配 ==========

    @Test
    public void testWrongArgumentType() {
        try {
            interpret("&obj.add('a', 'b')");
            fail("Should throw exception for wrong argument type");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("type")
                    || e.getMessage().contains("not found")
                    || e.getMessage().contains("match")
                    || e.getMessage().contains("Member"));
        }
    }

    @Test
    public void testWrongArgumentCount() {
        try {
            interpret("&obj.add(1)"); // 需要两个参数
            fail("Should throw exception for wrong argument count");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("argument")
                    || e.getMessage().contains("not found")
                    || e.getMessage().contains("match")
                    || e.getMessage().contains("Member"));
        }
    }

    @Test
    public void testTooManyArguments() {
        try {
            interpret("&obj.add(1, 2, 3)"); // 只需要两个参数
            fail("Should throw exception for too many arguments");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("argument")
                    || e.getMessage().contains("not found")
                    || e.getMessage().contains("match")
                    || e.getMessage().contains("Member"));
        }
    }

    // ========== 方法抛出异常 ==========

    @Test
    public void testMethodThrowsRuntimeException() {
        try {
            interpret("&obj.throwException()");
            fail("Should propagate RuntimeException");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("test-exception")
                    || e.getCause() != null && e.getCause().getMessage().contains("test-exception"));
        }
    }

    @Test
    public void testMethodThrowsIllegalArgumentException() {
        try {
            interpret("&obj.throwExceptionWithReturn()");
            fail("Should propagate IllegalArgumentException");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("test-illegal-arg")
                    || e.getCause() != null && e.getCause().getMessage().contains("test-illegal-arg"));
        }
    }

    @Test
    public void testMethodThrowsCheckedException() {
        try {
            interpret("&obj.throwCheckedException()");
            fail("Should propagate checked exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("test-checked-exception")
                    || e.getCause() != null && e.getCause().getMessage().contains("test-checked-exception"));
        }
    }

    @Test
    public void testExceptionInCompiledMode() throws Exception {
        try {
            compile("&obj.throwException()", "TestCompiledException");
            fail("Should propagate exception in compiled mode");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("test-exception")
                    || e.getCause() != null && e.getCause().getMessage().contains("test-exception"));
        }
    }

    // ========== 链式调用中的 null ==========

    @Test
    public void testNullInChain() {
        try {
            interpret("&obj.getNullValue().length()");
            fail("Should throw exception for null in chain");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("null")
                    || e.getMessage().contains("Null")
                    || e instanceof NullPointerException
                    || e.getCause() instanceof NullPointerException);
        }
    }

    @Test
    public void testNullReturnThenAccess() {
        try {
            interpret("&obj.getNull().toString()");
            fail("Should throw exception for null return then access");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("null")
                    || e.getMessage().contains("Null")
                    || e instanceof NullPointerException
                    || e.getCause() instanceof NullPointerException);
        }
    }

    // ========== 编译模式异常一致性 ==========

    @Test
    public void testExceptionConsistencyNullObject() {
        CompilationContext ctx = new CompilationContext("&nullObj.field");
        ctx.setAllowReflectionAccess(true);
        Environment env1 = FluxonRuntime.getInstance().newEnvironment();
        env1.defineRootVariable("nullObj", null);
        Environment env2 = FluxonRuntime.getInstance().newEnvironment();
        env2.defineRootVariable("nullObj", null);

        Exception interpretException = null;
        Exception compileException = null;

        try {
            Fluxon.parse(ctx, env1).eval(env1);
        } catch (Exception e) {
            interpretException = e;
        }

        try {
            CompileResult compileResult = Fluxon.compile(env2, ctx, "TestExceptConsist");
            Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
            RuntimeScriptBase base = (RuntimeScriptBase) scriptClass.newInstance();
            base.eval(env2);
        } catch (Exception e) {
            compileException = e;
        }

        assertNotNull(interpretException, "Interpret mode should throw exception");
        assertNotNull(compileException, "Compile mode should throw exception");
    }
}
