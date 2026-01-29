package org.tabooproject.fluxon;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;
import static org.tabooproject.fluxon.runtime.FunctionSignature.returnsObject;

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
        // player(String) -> "name:" + arg
        runtime.registerFunction("player", returnsObject().params(Type.STRING), ctx -> {
            ctx.setReturnRef("name:" + ctx.getString(0));
        });
        // player(UUID) -> "uuid:" + arg
        runtime.registerFunction("player", returnsObject().params(Type.fromClass(UUID.class)), ctx -> {
            ctx.setReturnRef("uuid:" + ctx.getRef(0));
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

    @Test
    void testPlayerWithStringLiteral() {
        // 调用 player("test") 应该匹配 player(String)
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("player(\"test\")");
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("name:test", result.getInterpretResult());
        assertEquals("name:test", result.getCompileResult());
    }

    @Test
    void testPlayerWithDynamicStringVariable() {
        // 变量类型未知时，应该在运行时正确解析到 player(String)
        String script = "key = \"test\"\nplayer(&key)";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(script);
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("name:test", result.getInterpretResult());
        assertEquals("name:test", result.getCompileResult());
    }

    @Test
    void testPlayerWithDynamicUUIDVariable() {
        // 变量类型未知时，应该在运行时正确解析到 player(UUID)
        UUID testUUID = UUID.randomUUID();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("player(&key)", ctx -> {}, env -> {
            env.setRootVariable("key", testUUID);
        });
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("uuid:" + testUUID, result.getInterpretResult());
        assertEquals("uuid:" + testUUID, result.getCompileResult());
    }

    @Test
    void testOverloadWithRoundResults() {
        // round() 返回 long，但 overload(int, int, int) 期望 int
        // 应该自动收窄转换
        String script = "currentR = 255.5\ncurrentG = 128.3\ncurrentB = 64.7\noverload(round(&currentR), round(&currentG), round(&currentB))";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(script);
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        // round(255.5)=256, round(128.3)=128, round(64.7)=65, sum=449
        assertEquals("triple:449", result.getInterpretResult());
        assertEquals("triple:449", result.getCompileResult());
    }

    @Test
    void testRoundReturnType() {
        // 测试 round() 的返回类型
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("round(1.5)");
        System.out.println("Interpret type: " + result.getInterpretResult().getClass());
        System.out.println("Compile type: " + result.getCompileResult().getClass());
        assertEquals(2L, result.getInterpretResult());
        assertEquals(2L, result.getCompileResult());
    }

    @Test
    void testOverloadWithLongLiterals() {
        // 直接测试 long 参数
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("overload(1L, 2L, 3L)");
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("triple:6", result.getInterpretResult());
        assertEquals("triple:6", result.getCompileResult());
    }
}
