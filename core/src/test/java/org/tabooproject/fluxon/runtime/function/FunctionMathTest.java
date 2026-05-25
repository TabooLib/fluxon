package org.tabooproject.fluxon.runtime.function;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.Fluxon;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.compiler.CompileResult;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FunctionMath 数学函数测试
 * 测试类型精确的重载解析
 *
 * @author sky
 */
public class FunctionMathTest {

    @Test
    void testMinInt() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("min(5, 3)");
        assertEquals(3, result.getInterpretResult());
        assertEquals(3, result.getCompileResult());
        assertTrue(result.getInterpretResult() instanceof Integer);
    }

    @Test
    void testMinLong() {
        // Long 字面量使用 L 后缀
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("min(5000000000L, 3000000000L)");
        assertEquals(3000000000L, result.getInterpretResult());
        assertEquals(3000000000L, result.getCompileResult());
        assertTrue(result.getInterpretResult() instanceof Long);
    }

    @Test
    void testMinDouble() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("min(5.5, 3.3)");
        assertEquals(3.3, result.getInterpretResult());
        assertEquals(3.3, result.getCompileResult());
        assertTrue(result.getInterpretResult() instanceof Double);
    }

    @Test
    void testMaxInt() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("max(5, 3)");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());
        assertTrue(result.getInterpretResult() instanceof Integer);
    }

    @Test
    void testMaxLong() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("max(5000000000L, 3000000000L)");
        assertEquals(5000000000L, result.getInterpretResult());
        assertEquals(5000000000L, result.getCompileResult());
        assertTrue(result.getInterpretResult() instanceof Long);
    }

    @Test
    void testMaxDouble() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("max(5.5, 3.3)");
        assertEquals(5.5, result.getInterpretResult());
        assertEquals(5.5, result.getCompileResult());
        assertTrue(result.getInterpretResult() instanceof Double);
    }

    @Test
    void testAbsInt() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("abs(-5)");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());
        assertTrue(result.getInterpretResult() instanceof Integer);
    }

    @Test
    void testAbsLong() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("abs(-5000000000L)");
        assertEquals(5000000000L, result.getInterpretResult());
        assertEquals(5000000000L, result.getCompileResult());
        assertTrue(result.getInterpretResult() instanceof Long);
    }

    @Test
    void testAbsDouble() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("abs(-5.5)");
        assertEquals(5.5, result.getInterpretResult());
        assertEquals(5.5, result.getCompileResult());
        assertTrue(result.getInterpretResult() instanceof Double);
    }

    @Test
    void testClampInt() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("clamp(15, 0, 10)");
        assertEquals(10, result.getInterpretResult());
        assertEquals(10, result.getCompileResult());
        result = FluxonTestUtil.runSilent("clamp(-5, 0, 10)");
        assertEquals(0, result.getInterpretResult());
        assertEquals(0, result.getCompileResult());
        result = FluxonTestUtil.runSilent("clamp(5, 0, 10)");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());
    }

    @Test
    void testClampDouble() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("clamp(15.0, 0.0, 10.0)");
        assertEquals(10.0, result.getInterpretResult());
        assertEquals(10.0, result.getCompileResult());
    }

    @Test
    void testPowInt() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("pow(2, 3)");
        assertEquals(8, result.getInterpretResult());
        assertEquals(8, result.getCompileResult());
        assertTrue(result.getInterpretResult() instanceof Integer);
    }

    @Test
    void testPowDouble() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("pow(2.0, 3.0)");
        assertEquals(8.0, result.getInterpretResult());
        assertEquals(8.0, result.getCompileResult());
        assertTrue(result.getInterpretResult() instanceof Double);
    }

    @Test
    void testRandomNoArgs() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("random()");
        assertTrue(result.getInterpretResult() instanceof Double);
        assertTrue(result.getCompileResult() instanceof Double);
        double val = (Double) result.getInterpretResult();
        assertTrue(val >= 0.0 && val < 1.0);
    }

    @Test
    void testRandomIntArg() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("random(10)");
        assertTrue(result.getInterpretResult() instanceof Integer, "random(int) should return Integer");
        assertTrue(result.getCompileResult() instanceof Integer, "random(int) should return Integer (compiled)");
        int val = (Integer) result.getInterpretResult();
        assertTrue(val >= 0 && val < 10);
    }

    @Test
    void testRandomDoubleArg() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("random(10.0)");
        assertTrue(result.getInterpretResult() instanceof Double, "random(double) should return Double");
        assertTrue(result.getCompileResult() instanceof Double, "random(double) should return Double (compiled)");
        double val = (Double) result.getInterpretResult();
        assertTrue(val >= 0.0 && val < 10.0);
    }

    @Test
    void testRandomIntRange() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("random(5, 10)");
        assertTrue(result.getInterpretResult() instanceof Integer);
        int val = (Integer) result.getInterpretResult();
        assertTrue(val >= 5 && val < 10);
    }

    @Test
    void testRandomDoubleRange() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("random(5.0, 10.0)");
        assertTrue(result.getInterpretResult() instanceof Double);
        double val = (Double) result.getInterpretResult();
        assertTrue(val >= 5.0 && val < 10.0);
    }

    @Test
    void testRandomDoubleRange2() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("random(1.5, 2)");
        System.out.println(result.getInterpretResult());
        System.out.println(result.getCompileResult());
    }

    @Test
    void testSinCos() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("sin(0.0)");
        assertEquals(0.0, (Double) result.getInterpretResult(), 0.0001);
        result = FluxonTestUtil.runSilent("cos(0.0)");
        assertEquals(1.0, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testTan() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("tan(0.0)");
        assertEquals(0.0, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testExp() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("exp(0.0)");
        assertEquals(1.0, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testLog() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("log(&E)");
        assertEquals(1.0, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testSqrt() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("sqrt(4.0)");
        assertEquals(2.0, result.getInterpretResult());
        assertEquals(2.0, result.getCompileResult());
    }

    @Test
    void testRound() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("round(3.7)");
        assertEquals(4L, result.getInterpretResult());
        assertEquals(4L, result.getCompileResult());
        assertTrue(result.getInterpretResult() instanceof Long);
    }

    @Test
    void testFloorCeil() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("floor(3.7)");
        assertEquals(3.0, result.getInterpretResult());
        result = FluxonTestUtil.runSilent("ceil(3.2)");
        assertEquals(4.0, result.getInterpretResult());
    }

    @Test
    void testSign() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("sign(-5)");
        assertEquals(-1, result.getInterpretResult());
        result = FluxonTestUtil.runSilent("sign(5)");
        assertEquals(1, result.getInterpretResult());
        result = FluxonTestUtil.runSilent("sign(0)");
        assertEquals(0, result.getInterpretResult());
    }

    @Test
    void testLerp() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("lerp(0.0, 10.0, 0.5)");
        assertEquals(5.0, result.getInterpretResult());
        assertEquals(5.0, result.getCompileResult());
    }

    @Test
    void testClampAndLerpUseDirectBindingBytecode() {
        CompileResult result = Fluxon.compile(
                "a = clamp(15, 0, 10)\n" +
                        "b = lerp(0.0, 10.0, 0.5)\n" +
                        "&a + &b",
                "MathDirectBindingTest"
        );
        String owner = "org/tabooproject/fluxon/runtime/function/FunctionMath";
        assertTrue(hasStaticInvocation(result, owner, "clamp"));
        assertTrue(hasStaticInvocation(result, owner, "lerp"));
    }

    @Test
    void testRadDeg() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("rad(180.0)");
        assertEquals(Math.PI, (Double) result.getInterpretResult(), 0.0001);
        result = FluxonTestUtil.runSilent("deg(&PI)");
        assertEquals(180.0, (Double) result.getInterpretResult(), 0.0001);
    }

    @Test
    void testHypot() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("hypot(3.0, 4.0)");
        assertEquals(5.0, result.getInterpretResult());
        assertEquals(5.0, result.getCompileResult());
    }

    @Test
    void testCbrt() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("cbrt(8.0)");
        assertEquals(2.0, result.getInterpretResult());
        assertEquals(2.0, result.getCompileResult());
    }

    @Test
    void testConstants() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("&PI");
        assertEquals(Math.PI, result.getInterpretResult());
        result = FluxonTestUtil.runSilent("&E");
        assertEquals(Math.E, result.getInterpretResult());
    }

    private static boolean hasStaticInvocation(CompileResult result, String owner, String method) {
        boolean[] matched = {false};
        ClassReader reader = new ClassReader(result.getMainClass());
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, visitor) {
                    @Override
                    public void visitMethodInsn(int opcode, String actualOwner, String actualName, String actualDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC && owner.equals(actualOwner) && method.equals(actualName)) {
                            matched[0] = true;
                        }
                        super.visitMethodInsn(opcode, actualOwner, actualName, actualDescriptor, isInterface);
                    }
                };
            }
        }, 0);
        return matched[0];
    }
}
