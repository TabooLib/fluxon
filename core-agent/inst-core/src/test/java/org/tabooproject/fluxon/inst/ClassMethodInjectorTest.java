package org.tabooproject.fluxon.inst;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.inst.bytecode.ClassMethodInjector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MethodInjector 字节码转换测试。
 */
class ClassMethodInjectorTest {

    @Test
    void testInjectBefore() throws Exception {
        // 获取测试类的字节码
        byte[] original = getClassBytes(TargetClass.class);
        
        // 创建注入规格
        InjectionSpec spec = new InjectionSpec(
                "org/tabooproject/fluxon/inst/MethodInjectorTest$TargetClass",
                "testMethod",
                null,
                InjectionType.BEFORE
        );

        // 执行注入
        byte[] modified = ClassMethodInjector.inject(original, Collections.singletonList(spec));
        
        // 验证字节码被修改
        assertNotNull(modified);
        assertTrue(modified.length > original.length, "修改后的字节码应该更大（包含注入代码）");

        // 验证注入的方法调用存在
        assertTrue(containsDispatcherCall(modified, "dispatchBefore"));
    }

    @Test
    void testInjectReplace() throws Exception {
        byte[] original = getClassBytes(TargetClass.class);
        
        InjectionSpec spec = new InjectionSpec(
                "org/tabooproject/fluxon/inst/MethodInjectorTest$TargetClass",
                "testMethod",
                null,
                InjectionType.REPLACE
        );

        byte[] modified = ClassMethodInjector.inject(original, Collections.singletonList(spec));
        
        assertNotNull(modified);
        assertTrue(containsDispatcherCall(modified, "dispatchReplace"));
    }

    @Test
    void testInjectWithDescriptor() throws Exception {
        byte[] original = getClassBytes(TargetClass.class);
        
        // 只注入特定签名的方法
        InjectionSpec spec = new InjectionSpec(
                "org/tabooproject/fluxon/inst/MethodInjectorTest$TargetClass",
                "overloadedMethod",
                "(I)V",
                InjectionType.BEFORE
        );

        byte[] modified = ClassMethodInjector.inject(original, Collections.singletonList(spec));
        assertNotNull(modified);
    }

    @Test
    void testMultipleInjections() throws Exception {
        byte[] original = getClassBytes(TargetClass.class);
        
        List<InjectionSpec> specs = new ArrayList<>();
        specs.add(new InjectionSpec(
                "org/tabooproject/fluxon/inst/MethodInjectorTest$TargetClass",
                "testMethod",
                null,
                InjectionType.BEFORE
        ));
        specs.add(new InjectionSpec(
                "org/tabooproject/fluxon/inst/MethodInjectorTest$TargetClass",
                "anotherMethod",
                null,
                InjectionType.BEFORE
        ));

        byte[] modified = ClassMethodInjector.inject(original, specs);
        assertNotNull(modified);
    }

    @Test
    void testNoMatchingMethod() throws Exception {
        byte[] original = getClassBytes(TargetClass.class);
        
        InjectionSpec spec = new InjectionSpec(
                "org/tabooproject/fluxon/inst/MethodInjectorTest$TargetClass",
                "nonExistentMethod",
                null,
                InjectionType.BEFORE
        );

        byte[] modified = ClassMethodInjector.inject(original, Collections.singletonList(spec));
        
        // 没有匹配的方法，字节码应该基本不变
        assertNotNull(modified);
    }

    /**
     * 获取类的字节码。
     */
    private byte[] getClassBytes(Class<?> clazz) throws Exception {
        String resourceName = clazz.getName().replace('.', '/') + ".class";
        java.io.InputStream is = clazz.getClassLoader().getResourceAsStream(resourceName);
        assertNotNull(is, "无法加载类资源: " + resourceName);
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toByteArray();
        } finally {
            is.close();
        }
    }

    /**
     * 检查字节码中是否包含对 CallbackDispatcher 的指定方法调用。
     */
    private boolean containsDispatcherCall(byte[] bytecode, String methodName) {
        ClassReader reader = new ClassReader(bytecode);
        boolean[] found = {false};
        
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (owner.contains("CallbackDispatcher") && name.equals(methodName)) {
                            found[0] = true;
                        }
                    }
                };
            }
        }, 0);
        
        return found[0];
    }

    /**
     * 测试用目标类。
     */
    public static class TargetClass {
        public void testMethod() {
            System.out.println("original testMethod");
        }

        public String anotherMethod(String input) {
            return "result: " + input;
        }

        public void overloadedMethod() {
            System.out.println("no args");
        }

        public void overloadedMethod(int value) {
            System.out.println("int arg: " + value);
        }

        public void overloadedMethod(String value) {
            System.out.println("string arg: " + value);
        }
    }
}
