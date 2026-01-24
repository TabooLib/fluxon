package org.tabooproject.fluxon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.type.TestRuntime;

import static org.junit.jupiter.api.Assertions.*;

public class TernaryTypeTest {

    @BeforeEach
    public void setup() {
        TestRuntime.registerTestFunctions();
    }

    @Test
    public void testIfThenElseIntType() {
        String source = "_num1 = 10; _num2 = 20; _max = if &_num1 > &_num2 then &_num1 else &_num2; &_max";
        CompilationContext context = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        CompileResult result = Fluxon.compile(env, context, "TernaryTest");
        byte[] mainClass = result.getMainClass();
        String bytecodeStr = new String(mainClass, java.nio.charset.StandardCharsets.ISO_8859_1);

        System.out.println("Has setLocalInt: " + bytecodeStr.contains("setLocalInt"));
        System.out.println("Has setLocalRef: " + bytecodeStr.contains("setLocalRef"));
        System.out.println("Has getLocalInt: " + bytecodeStr.contains("getLocalInt"));
        System.out.println("Has getLocalRef: " + bytecodeStr.contains("getLocalRef"));

        // 所有变量都应该使用 int 路径
        assertTrue(bytecodeStr.contains("setLocalInt"), "Should use setLocalInt for int variables");
        assertFalse(bytecodeStr.contains("setLocalRef"), "Should NOT use setLocalRef for int variables");

        // 执行验证
        org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader cl = new org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader();
        Class<?> scriptClass = result.defineClass(cl);
        try {
            org.tabooproject.fluxon.runtime.RuntimeScriptBase base = (org.tabooproject.fluxon.runtime.RuntimeScriptBase) scriptClass.newInstance();
            Environment execEnv = FluxonRuntime.getInstance().newEnvironment();
            Object execResult = base.eval(execEnv);
            System.out.println("Result: " + execResult);
            assertEquals(20, execResult);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
