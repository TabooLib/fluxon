package org.tabooproject.fluxon.interpreter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.Fluxon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;
import org.tabooproject.fluxon.runtime.stdlib.Operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * For 循环表达式测试
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ForExpressionTest {

    @Test
    public void testBasicListIteration() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent(
                "result = 0; for i in [1, 2, 3] { &result += &i }; &result");
        assertEquals(6, result.getInterpretResult());
        assertEquals(6, result.getCompileResult());
    }

    @Test
    public void testEmptyListIteration() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; for i in [] { &result += 1 }; &result");
        assertEquals(0, result.getInterpretResult());
        assertEquals(0, result.getCompileResult());
    }

    @Test
    public void testRangeIteration() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent(
                "result = 0; for i in 1..4 { &result += &i }; &result");
        assertEquals(10, result.getInterpretResult());
        assertEquals(10, result.getCompileResult());
    }

    @Test
    public void testExclusiveRangeIteration() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; for i in 1..<4 { &result += &i }; &result");
        assertEquals(6, result.getInterpretResult());
        assertEquals(6, result.getCompileResult());
    }

    @Test
    public void testMapIteration() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "map = [a: 10, b: 20]; result = 0; for (k, v) in &map { &result += &v }; &result");
        assertEquals(30, result.getInterpretResult());
        assertEquals(30, result.getCompileResult());
    }

    @Test
    public void testStringIteration() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text = 'hello'; result = ''; for c in &text { &result += &c }; &result");
        assertEquals("hello", result.getInterpretResult());
        assertEquals("hello", result.getCompileResult());
    }

    @Test
    public void testForLoopVariableReference() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; result = []; for i in &list { &result += &i }; &result");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3]", result.getCompileResult().toString());
    }

    @Test
    public void testForLoopVariableModification() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; for i in &list { i = &i * 2 }; &list");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3]", result.getCompileResult().toString());
    }

    @Test
    public void testNestedForLoop() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in 1..3 { " +
                        "  for j in 1..3 { " +
                        "    result += (&i * &j) " +
                        "  } " +
                        "}; " +
                        "&result");
        assertEquals(36, result.getInterpretResult());
        assertEquals(36, result.getCompileResult());
    }

    @Test
    public void testDeepNestedForLoop() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in 1..2 { " +
                        "  for j in 1..2 { " +
                        "    for k in 1..2 { " +
                        "      result += 1 " +
                        "    } " +
                        "  } " +
                        "}; " +
                        "&result");
        assertEquals(8, result.getInterpretResult());
        assertEquals(8, result.getCompileResult());
    }

    @Test
    public void testMapDestructuring() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "map = [a: 1, b: 2]; " +
                        "result = 0; " +
                        "for (key, value) in &map { " +
                        "  result += &value " +
                        "}; " +
                        "&result");
        assertEquals(3, result.getInterpretResult());
        assertEquals(3, result.getCompileResult());
    }

    @Test
    public void testListOfListsDestructuring() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [[1, 2], [3, 4], [5, 6]]; " +
                        "result = 0; " +
                        "for (first, second) in &list { " +
                        "  result += &first + &second " +
                        "}; " +
                        "&result");
        assertEquals(21, result.getInterpretResult());
        assertEquals(21, result.getCompileResult());
    }

    @Test
    public void testMultipleDestructuring() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [[1, 2, 3], [4, 5, 6]]; " +
                        "result = 0; " +
                        "for (a, b, c) in &list { " +
                        "  result += &a + &b + &c " +
                        "}; " +
                        "&result");
        assertEquals(21, result.getInterpretResult());
        assertEquals(21, result.getCompileResult());
    }

    @Test
    public void testForLoopWithConditional() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in [1, 2, 3, 4, 5] { " +
                        "  if &i > 2 then result += &i " +
                        "}; " +
                        "&result");
        assertEquals(12, result.getInterpretResult());
        assertEquals(12, result.getCompileResult());
    }

    @Test
    public void testForLoopAccumulation() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = []; " +
                        "for i in [10, 20, 30] { " +
                        "  result += &i * 2 " +
                        "}; " +
                        "&result");
        assertEquals("[20, 40, 60]", result.getInterpretResult().toString());
        assertEquals("[20, 40, 60]", result.getCompileResult().toString());
    }

    @Test
    public void testForLoopFiltering() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = []; " +
                        "for i in [1, 2, 3, 4, 5, 6] { " +
                        "  if &i % 2 == 0 then result += &i " +
                        "}; " +
                        "&result");
        assertEquals("[2, 4, 6]", result.getInterpretResult().toString());
        assertEquals("[2, 4, 6]", result.getCompileResult().toString());
    }

    @Test
    public void testForLoopWithContextCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "texts = ['hello', 'world']; " +
                        "result = []; " +
                        "for t in &texts { " +
                        "  result += &t::uppercase() " +
                        "}; " +
                        "&result");
        assertEquals("[HELLO, WORLD]", result.getInterpretResult().toString());
        assertEquals("[HELLO, WORLD]", result.getCompileResult().toString());
    }

    @Test
    public void testForLoopWithIndexAccess() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "matrix = [[1, 2], [3, 4]]; " +
                        "result = 0; " +
                        "for row in &matrix { " +
                        "  result += &row[0] + &row[1] " +
                        "}; " +
                        "&result");
        assertEquals(10, result.getInterpretResult());
        assertEquals(10, result.getCompileResult());
    }

    @Test
    public void testForLoopWithCompoundAssignment() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [1, 2, 3]; " +
                        "result = 1; " +
                        "for i in &list { " +
                        "  result *= &i " +
                        "}; " +
                        "&result");
        assertEquals(6, result.getInterpretResult());
        assertEquals(6, result.getCompileResult());
    }

    @Test
    public void testForLoopAccessOuterVariable() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "multiplier = 10; " +
                        "result = 0; " +
                        "for i in [1, 2, 3] { " +
                        "  result += &i * &multiplier " +
                        "}; " +
                        "&result");
        assertEquals(60, result.getInterpretResult());
        assertEquals(60, result.getCompileResult());
    }

    @Test
    public void testForLoopModifyOuterVariable() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "total = 0; " +
                        "for i in 1..5 { " +
                        "  total += &i " +
                        "}; " +
                        "&total");
        assertEquals(15, result.getInterpretResult());
        assertEquals(15, result.getCompileResult());
    }

    @Test
    public void testForLoopSingleElement() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in [42] { " +
                        "  result += &i " +
                        "}; " +
                        "&result");
        assertEquals(42, result.getInterpretResult());
        assertEquals(42, result.getCompileResult());
    }

    @Test
    public void testForLoopLargeCollection() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in 1..100 { " +
                        "  result += &i " +
                        "}; " +
                        "&result");
        assertEquals(5050, result.getInterpretResult());
        assertEquals(5050, result.getCompileResult());
    }

    @Test
    public void testForLoopNegativeRange() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in -3..-1 { " +
                        "  result += &i " +
                        "}; " +
                        "&result");
        assertEquals(-6, result.getInterpretResult());
        assertEquals(-6, result.getCompileResult());
    }

    @Test
    public void testForLoopReverseRange() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in 5..1 { " +
                        "  result += &i " +
                        "}; " +
                        "&result");
        assertEquals(15, result.getInterpretResult());
        assertEquals(15, result.getCompileResult());
    }

    @Test
    public void testForLoopExclusiveReverseRange() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in 5..<1 { " +
                        "  result += &i " +
                        "}; " +
                        "&result");
        assertEquals(14, result.getInterpretResult());
        assertEquals(14, result.getCompileResult());
    }

    @Test
    public void testForLoopSingleValueRange() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = 0; " +
                        "for i in 0..0 { " +
                        "  result += &i + 1 " +
                        "}; " +
                        "&result");
        assertEquals(1, result.getInterpretResult());
        assertEquals(1, result.getCompileResult());
    }

    @Test
    public void testForLoopRangeContinueKeepsIteratorProgress() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = ''\n" +
                        "for i in 1..5 {\n" +
                        "  if &i % 2 == 0 {\n" +
                        "    continue\n" +
                        "  }\n" +
                        "  result = &result + &i\n" +
                        "}\n" +
                        "&result");
        assertEquals("135", result.getInterpretResult());
        assertEquals("135", result.getCompileResult());
    }

    @Test
    public void testNestedForLoopRangeBreakAndContinueStayScoped() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = ''\n" +
                        "for i in 1..3 {\n" +
                        "  for j in 1..4 {\n" +
                        "    if &j == 2 {\n" +
                        "      continue\n" +
                        "    }\n" +
                        "    if &j == 4 {\n" +
                        "      break\n" +
                        "    }\n" +
                        "    result = &result + &i + ':' + &j + ','\n" +
                        "  }\n" +
                        "}\n" +
                        "&result");
        assertEquals("1:1,1:3,2:1,2:3,3:1,3:3,", result.getInterpretResult());
        assertEquals("1:1,1:3,2:1,2:3,3:1,3:3,", result.getCompileResult());
    }

    @Test
    public void testCompiledIntRangeLoopSkipsIteratorCreation() {
        CompileResult result = Fluxon.compile(
                "result = 0\n" +
                        "for i in 1..5 {\n" +
                        "  result += &i\n" +
                        "}\n" +
                        "&result",
                "ForRangeShapeTest"
        );
        assertFalse(hasMethodInvocation(result, Intrinsics.TYPE.getPath(), "createIterator"));
    }

    @Test
    public void testForLoopPrimitiveCompoundAssignmentAvoidsOperationsAdd() {
        String source = "sum = 0\n" +
                "for i in 1..10 { sum += &i }\n" +
                "&sum";
        FluxonTestUtil.TestResult runResult = FluxonTestUtil.runSilent(source);
        FluxonTestUtil.assertBothEqual(55, runResult);
        CompileResult result = Fluxon.compile(source, "ForPrimitiveCompoundShapeTest");
        assertFalse(hasMethodInvocation(result, Operations.TYPE.getPath(), "add"));
    }

    @Test
    public void testForLoopRangeWithRootConstantUsesSingleDirectionLoop() {
        String source = "LIMIT = 10\n" +
                "sum = 0\n" +
                "for i in 1..&LIMIT { sum += &i }\n" +
                "&sum";
        FluxonTestUtil.TestResult runResult = FluxonTestUtil.runSilent(source);
        FluxonTestUtil.assertBothEqual(55, runResult);
        CompileResult result = Fluxon.compile(source, "ForRootConstantRangeShapeTest");
        assertFalse(hasMethodInvocation(result, Intrinsics.TYPE.getPath(), "createIterator"));
        assertFalse(hasMethodInvocation(result, Operations.TYPE.getPath(), "add"));
        assertFalse(hasJumpOpcode(result, Opcodes.IFLE));
    }

    @Test
    public void testCompiledStringPlusPrimitiveSkipsOperationsAdd() {
        String source = "sum = 55\n" +
                "\"Sum: \" + &sum";
        FluxonTestUtil.TestResult runResult = FluxonTestUtil.runSilent(source);
        FluxonTestUtil.assertBothEqual("Sum: 55", runResult);
        CompileResult result = Fluxon.compile(source, "StringPlusPrimitiveShapeTest");
        assertFalse(hasMethodInvocation(result, Operations.TYPE.getPath(), "add"));
    }

    @Test
    public void testCompiledRangeExpressionUsesPrimitiveCreation() {
        CompileResult result = Fluxon.compile(
                "range = 1..5\n" +
                        "&range::size()",
                "RangePrimitiveShapeTest"
        );
        assertFalse(hasMethodInvocation(result, Intrinsics.TYPE.getPath(), "createRange", "(Ljava/lang/Object;Ljava/lang/Object;Z)"));
    }

    @Test
    public void testForLoopListBuilding() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = []; " +
                        "for i in 1..5 { " +
                        "  result += &i " +
                        "}; " +
                        "&result");
        assertEquals("[1, 2, 3, 4, 5]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3, 4, 5]", result.getCompileResult().toString());
    }

    @Test
    public void testForLoopMapBuilding() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = [:]; " +
                        "for i in [1, 2, 3] { " +
                        "  &result['key' + &i::toString()] = &i * 10 " +
                        "}; " +
                        "&result");
        assertNotNull(result.getInterpretResult());
        assertNotNull(result.getCompileResult());
    }

    @Test
    public void testForLoopStringBuilding() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = ''; " +
                        "for i in [1, 2, 3] { " +
                        "  result += &i::toString() " +
                        "}; " +
                        "&result");
        assertEquals("123", result.getInterpretResult());
        assertEquals("123", result.getCompileResult());
    }

    @Test
    public void testComplexNestedStructure() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "data = [[1, 2], [3, 4], [5, 6]]; " +
                        "result = 0; " +
                        "for row in &data { " +
                        "  for col in &row { " +
                        "    result += &col " +
                        "  } " +
                        "}; " +
                        "&result");
        assertEquals(21, result.getInterpretResult());
        assertEquals(21, result.getCompileResult());
    }

    @Test
    public void testForLoopWithBreakSimulation() {
        // 使用条件语句模拟 break 的效果
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = []; " +
                        "for i in [1, 2, 3, 4, 5] { " +
                        "  if &i > 3 then { result } else { result += &i } " +
                        "}; " +
                        "&result");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3]", result.getCompileResult().toString());
    }

    private static boolean hasMethodInvocation(CompileResult result, String owner, String method) {
        return hasMethodInvocation(result, owner, method, null);
    }

    private static boolean hasMethodInvocation(CompileResult result, String owner, String method, String descriptor) {
        boolean[] matched = {false};
        ClassReader reader = new ClassReader(result.getMainClass());
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, visitor) {
                    @Override
                    public void visitMethodInsn(int opcode, String actualOwner, String actualName, String actualDescriptor, boolean isInterface) {
                        if (owner.equals(actualOwner) && method.equals(actualName) && (descriptor == null || descriptor.equals(actualDescriptor))) {
                            matched[0] = true;
                        }
                        super.visitMethodInsn(opcode, actualOwner, actualName, actualDescriptor, isInterface);
                    }
                };
            }
        }, 0);
        return matched[0];
    }

    private static boolean hasJumpOpcode(CompileResult result, int expectedOpcode) {
        boolean[] matched = {false};
        ClassReader reader = new ClassReader(result.getMainClass());
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, visitor) {
                    @Override
                    public void visitJumpInsn(int opcode, org.objectweb.asm.Label label) {
                        if (opcode == expectedOpcode) {
                            matched[0] = true;
                        }
                        super.visitJumpInsn(opcode, label);
                    }
                };
            }
        }, 0);
        return matched[0];
    }
}
