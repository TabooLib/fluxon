package org.tabooproject.fluxon.inst.agent;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;

/**
 * Agent 端字节码注入器。
 */
public class AgentBytecodeInjector {

    private static final String DISPATCHER = "org/tabooproject/fluxon/inst/CallbackDispatcher";

    /**
     * 注入字节码。
     * @param bytecode 原始字节码
     * @param specs 注入规格列表，每项为 [id, methodName, descriptor, type]
     */
    public static byte[] inject(byte[] bytecode, List<String[]> specs) {
        ClassReader reader = new ClassReader(bytecode);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new InjectingClassVisitor(writer, specs);
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static class InjectingClassVisitor extends ClassVisitor {
        private final List<String[]> specs;

        InjectingClassVisitor(ClassVisitor cv, List<String[]> specs) {
            super(Opcodes.ASM9, cv);
            this.specs = specs;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            for (String[] spec : specs) {
                String specMethodName = spec[1];
                String specDescriptor = spec[2];
                // 方法名匹配，且描述符为 null 或匹配
                if (name.equals(specMethodName) && 
                    (specDescriptor == null || specDescriptor.isEmpty() || descriptor.equals(specDescriptor))) {
                    String id = spec[0];
                    String type = spec[3];
                    boolean isReplace = "REPLACE".equalsIgnoreCase(type);
                    boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
                    mv = new InjectingMethodAdapter(mv, access, name, descriptor, id, isReplace, isStatic);
                    break;
                }
            }
            return mv;
        }
    }

    private static class InjectingMethodAdapter extends AdviceAdapter {
        private final String specId;
        private final boolean isReplace;
        private final boolean isStatic;
        private final String methodDescriptor;

        InjectingMethodAdapter(MethodVisitor mv, int access, String name, String descriptor, String specId, boolean isReplace, boolean isStatic) {
            super(Opcodes.ASM9, mv, access, name, descriptor);
            this.specId = specId;
            this.isReplace = isReplace;
            this.isStatic = isStatic;
            this.methodDescriptor = descriptor;
        }

        @Override
        protected void onMethodEnter() {
            // 创建参数数组
            createArgsArray();
            // 调用 CallbackDispatcher
            push(specId);
            swap();
            if (isReplace) {
                invokeStatic(Type.getObjectType(DISPATCHER), new Method("dispatchReplace", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;"));
                // 处理返回值
                Type returnType = Type.getReturnType(methodDescriptor);
                if (returnType.getSort() == Type.VOID) {
                    pop();
                    returnValue();
                } else if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
                    checkCast(returnType);
                    returnValue();
                } else {
                    unbox(returnType);
                    returnValue();
                }
            } else {
                // BEFORE 模式
                invokeStatic(Type.getObjectType(DISPATCHER), new Method("dispatchBefore", "(Ljava/lang/String;[Ljava/lang/Object;)Z"));
                pop(); // 忽略返回值，继续执行原方法
            }
        }

        private void createArgsArray() {
            Type[] argTypes = Type.getArgumentTypes(methodDescriptor);
            int arraySize = argTypes.length + (isStatic ? 0 : 1);
            push(arraySize);
            newArray(Type.getType(Object.class));
            int arrayIndex = 0;
            // 实例方法添加 this
            if (!isStatic) {
                dup();
                push(arrayIndex++);
                loadThis();
                arrayStore(Type.getType(Object.class));
            }
            // 添加所有参数
            for (int i = 0; i < argTypes.length; i++) {
                dup();
                push(arrayIndex++);
                loadArg(i);
                box(argTypes[i]);
                arrayStore(Type.getType(Object.class));
            }
        }
    }
}
