package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.FluxonTestUtil.TestResult;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.FluxonTestUtil.*;

/**
 * 常量语法测试
 * 测试全大写标识符常量定义、内联和重赋值检测
 */
public class ConstantTest {

    @Test
    public void testIsConstantName_ValidNames() {
        assertTrue(SymbolEnvironment.isConstantName("PI"));
        assertTrue(SymbolEnvironment.isConstantName("MAX_VALUE"));
        assertTrue(SymbolEnvironment.isConstantName("HTTP_200"));
        assertTrue(SymbolEnvironment.isConstantName("A"));
        assertTrue(SymbolEnvironment.isConstantName("ABC123"));
        assertTrue(SymbolEnvironment.isConstantName("A_B_C"));
    }

    @Test
    public void testIsConstantName_InvalidNames() {
        assertFalse(SymbolEnvironment.isConstantName("pi"));           // 全小写
        assertFalse(SymbolEnvironment.isConstantName("Pi"));           // 混合大小写
        assertFalse(SymbolEnvironment.isConstantName("maxValue"));     // 驼峰
        assertFalse(SymbolEnvironment.isConstantName("Max_Value"));    // 混合
        assertFalse(SymbolEnvironment.isConstantName("_MAX"));         // 下划线开头
        assertFalse(SymbolEnvironment.isConstantName("123MAX"));       // 数字开头
        assertFalse(SymbolEnvironment.isConstantName(""));             // 空字符串
        assertFalse(SymbolEnvironment.isConstantName(null));           // null
        assertFalse(SymbolEnvironment.isConstantName("服务器"));        // 中文
    }

    @Test
    public void testIntegerConstant() {
        TestResult result = runSilent("MAX = 100; &MAX");
        assertBothEqual(100, result);
    }

    @Test
    public void testDoubleConstant() {
        TestResult result = runSilent("PI = 3.14159; &PI");
        assertBothEqual(3.14159, result);
    }

    @Test
    public void testLongConstant() {
        TestResult result = runSilent("BIG = 9999999999L; &BIG");
        assertBothEqual(9999999999L, result);
    }

    @Test
    public void testStringConstant() {
        TestResult result = runSilent("GREETING = \"Hello\"; &GREETING");
        assertBothEqual("Hello", result);
    }

    @Test
    public void testBooleanConstant() {
        TestResult result = runSilent("ENABLED = true; &ENABLED");
        assertBothEqual(true, result);
    }

    @Test
    public void testNullConstant() {
        TestResult result = runSilent("NOTHING = null; &NOTHING");
        assertBothEqual(null, result);
    }

    @Test
    public void testConstantInliningInExpression() {
        TestResult result = runSilent("FACTOR = 10; result = &FACTOR * 2; &result");
        assertBothEqual(20, result);
    }

    @Test
    public void testConstantInliningMultipleReferences() {
        TestResult result = runSilent("X = 5; result = &X + &X + &X; &result");
        assertBothEqual(15, result);
    }

    @Test
    public void testConstantInliningWithVariable() {
        TestResult result = runSilent("CONST = 100; variable = 50; result = &CONST + &variable; &result");
        assertBothEqual(150, result);
    }

    @Test
    public void testPureRootConstantReadsSkipRuntimeLookup() {
        String source = "a = 10\n" +
                "b = 20\n" +
                "max = if &a > &b then &a else &b\n" +
                "&max";
        TestResult result = runSilent(source);
        assertBothEqual(20, result);
        CompileResult compileResult = Fluxon.compile(source, "PureRootConstantReadShapeTest");
        assertEquals(1, countMethodInvocation(compileResult, Intrinsics.TYPE.getPath(), "getVariable"));
    }

    @Test
    public void testConstantInliningStringConcat() {
        TestResult result = runSilent("PREFIX = \"log:\"; msg = &PREFIX + \"info\"; &msg");
        assertBothEqual("log:info", result);
    }

    @Test
    public void testReassignConstantError() {
        runExpectingError("X = 10; X = 20", "Cannot reassign constant: X");
    }

    @Test
    public void testReassignConstantWithCompoundAssignmentError() {
        runExpectingError("X = 10; X += 5", "Cannot reassign constant: X");
    }

    @Test
    public void testReassignConstantToVariableError() {
        runExpectingError("X = 10; y = 5; X = &y", "Cannot reassign constant: X");
    }

    @Test
    public void testMixedCaseNotConstant() {
        // 混合大小写不是常量，可以重新赋值
        TestResult result = runSilent("MaxValue = 10; MaxValue = 20; &MaxValue");
        assertBothEqual(20, result);
    }

    @Test
    public void testLowercaseNotConstant() {
        // 全小写不是常量，可以重新赋值
        TestResult result = runSilent("max = 10; max = 20; &max");
        assertBothEqual(20, result);
    }

    @Test
    public void testConstantNameWithNonLiteralIsVariable() {
        // 全大写但赋值非字面量，应该被当作变量（可重新赋值）
        TestResult result = runSilent("x = 10; RESULT = &x; RESULT = 20; &RESULT");
        assertBothEqual(20, result);
    }

    @Test
    public void testConstantNameWithExpressionIsVariable() {
        // 全大写但赋值表达式，应该被当作变量（可重新赋值）
        TestResult result = runSilent("SUM = 1 + 2; SUM = 10; &SUM");
        assertBothEqual(10, result);
    }

    @Test
    public void testSingleCharConstant() {
        TestResult result = runSilent("A = 1; &A");
        assertBothEqual(1, result);
    }

    @Test
    public void testConstantWithUnderscore() {
        TestResult result = runSilent("MAX_SIZE = 256; &MAX_SIZE");
        assertBothEqual(256, result);
    }

    @Test
    public void testConstantWithNumbers() {
        TestResult result = runSilent("HTTP200 = 200; &HTTP200");
        assertBothEqual(200, result);
    }

    @Test
    public void testConstantInFunction() {
        // 常量在函数中也应该被内联
        TestResult result = runSilent(
                "PI = 3.14159\n" +
                "def area(r) = &PI * &r * &r\n" +
                "area(10)"
        );
        assertBothEqual(3.14159 * 10 * 10, result);
    }

    @Test
    public void testMultipleConstants() {
        TestResult result = runSilent(
                "WIDTH = 10\n" +
                "HEIGHT = 20\n" +
                "&WIDTH * &HEIGHT"
        );
        assertBothEqual(200, result);
    }

    private static int countMethodInvocation(CompileResult result, String owner, String method) {
        int[] count = {0};
        ClassReader reader = new ClassReader(result.getMainClass());
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, visitor) {
                    @Override
                    public void visitMethodInsn(int opcode, String actualOwner, String actualName, String actualDescriptor, boolean isInterface) {
                        if (owner.equals(actualOwner) && method.equals(actualName)) {
                            count[0]++;
                        }
                        super.visitMethodInsn(opcode, actualOwner, actualName, actualDescriptor, isInterface);
                    }
                };
            }
        }, 0);
        return count[0];
    }
}
