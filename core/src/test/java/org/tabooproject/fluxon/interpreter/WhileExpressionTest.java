package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * While 循环表达式测试
 *
 * @author sky
 */
public class WhileExpressionTest {

    @Test
    public void testWhileRootCompoundAssignmentCachesPureBody() {
        String source = "i = 0\n" +
                "sum = 0\n" +
                "while &i < 10 {\n" +
                "  sum += &i\n" +
                "  i += 1\n" +
                "}\n" +
                "&sum";
        FluxonTestUtil.TestResult runResult = FluxonTestUtil.runSilent(source);
        FluxonTestUtil.assertBothEqual(45, runResult);
        CompileResult result = Fluxon.compile(source, "WhileRootCacheShapeTest");
        assertEquals(1, countMethodInvocation(result, Intrinsics.TYPE.getPath(), "getVariable"));
    }

    @Test
    public void testWhileRootCacheSkipsObservableBody() {
        String source = "i = 0\n" +
                "sum = 0\n" +
                "while &i < 3 {\n" +
                "  print(&sum)\n" +
                "  sum += &i\n" +
                "  i += 1\n" +
                "}\n" +
                "&sum";
        FluxonTestUtil.TestResult runResult = FluxonTestUtil.runSilent(source);
        FluxonTestUtil.assertBothEqual(3, runResult);
        CompileResult result = Fluxon.compile(source, "WhileRootCacheObservableShapeTest");
        assertEquals(4, countMethodInvocation(result, Intrinsics.TYPE.getPath(), "getVariable"));
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
