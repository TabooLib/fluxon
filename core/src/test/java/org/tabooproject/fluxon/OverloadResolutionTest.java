package org.tabooproject.fluxon;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

/**
 * 函数重载解析测试
 *
 * @author sky
 */
public class OverloadResolutionTest {

    @BeforeAll
    static void setup() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        // 注册测试函数：三个重载
        // overload(String) -> "string:" + arg
        runtime.registerFunction("overload", returns(Type.STRING).params(Type.STRING), ctx -> {
            ctx.setReturnRef("string:" + ctx.getString(0));
        });
        // overload(int) -> "int:" + arg
        runtime.registerFunction("overload", returns(Type.STRING).params(Type.I), ctx -> {
            ctx.setReturnRef("int:" + ctx.getInt(0));
        });
        // overload(int, int, int) -> "triple:" + sum
        runtime.registerFunction("overload", returns(Type.STRING).params(Type.I, Type.I, Type.I), ctx -> {
            ctx.setReturnRef("triple:" + (ctx.getInt(0) + ctx.getInt(1) + ctx.getInt(2)));
        });
    }

    @Test
    void testOverloadWithStringLiteral() {
        // 调用 overload("#dc9dfc") 应该匹配 overload(String)
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("overload(\"#dc9dfc\")");
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("string:#dc9dfc", result.getInterpretResult());
        assertEquals("string:#dc9dfc", result.getCompileResult());
    }

    @Test
    void testOverloadWithIntLiteral() {
        // 调用 overload(123) 应该匹配 overload(int)
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("overload(123)");
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("int:123", result.getInterpretResult());
        assertEquals("int:123", result.getCompileResult());
    }

    @Test
    void testOverloadWithTripleInt() {
        // 调用 overload(1, 2, 3) 应该匹配 overload(int, int, int)
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("overload(1, 2, 3)");
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("triple:6", result.getInterpretResult());
        assertEquals("triple:6", result.getCompileResult());
    }
}
