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
                    boolean isAfter = "AFTER".equalsIgnoreCase(type);
                    boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
                    mv = new InjectingMethodAdapter(mv, access, name, descriptor, id, isReplace, isAfter, isStatic);
                    break;
                }
            }
            return mv;
        }
    }

    private static class InjectingMethodAdapter extends AdviceAdapter {
        private final String specId;
        private final boolean isReplace;
        private final boolean isAfter;
        private final boolean isStatic;
        private final String methodDescriptor;
        
        // AFTER 模式需要保存参数
        private int[] savedArgLocals;

        InjectingMethodAdapter(MethodVisitor mv, int access, String name, String descriptor, String specId, boolean isReplace, boolean isAfter, boolean isStatic) {
            super(Opcodes.ASM9, mv, access, name, descriptor);
            this.specId = specId;
            this.isReplace = isReplace;
            this.isAfter = isAfter;
            this.isStatic = isStatic;
            this.methodDescriptor = descriptor;
        }

        @Override
        protected void onMethodEnter() {
            // AFTER 模式在方法退出时处理，需要保存参数以便后续使用
            if (isAfter) {
                saveArgsForAfter();
                return;
            }
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
        
        @Override
        protected void onMethodExit(int opcode) {
            if (!isAfter) {
                return;
            }
            Type returnType = Type.getReturnType(methodDescriptor);
            Type[] argTypes = Type.getArgumentTypes(methodDescriptor);
            // 数组大小：this(可选) + 参数 + 返回值/异常
            int arraySize = argTypes.length + (isStatic ? 0 : 1) + 1;
            // 保存原始返回值到局部变量，并从栈上移除
            int resultLocal = -1;
            if (opcode == ATHROW) {
                // 异常退出：栈顶是异常对象
                resultLocal = newLocal(Type.getType(Object.class));
                storeLocal(resultLocal);  // 保存并移除栈顶异常
            } else {
                // 正常返回：保持原始类型
                if (returnType.getSort() != Type.VOID) {
                    resultLocal = newLocal(returnType);
                    storeLocal(resultLocal);  // 保存并移除栈顶返回值
                }
            }
            // 此时栈为空
            // 构建参数数组：[this?, arg1, arg2, ..., returnValue/exception]
            push(arraySize);
            newArray(Type.getType(Object.class));
            int arrayIndex = 0;
            // 添加 this（如果是实例方法）
            if (!isStatic) {
                dup();
                push(arrayIndex++);
                loadLocal(savedArgLocals[0]);
                arrayStore(Type.getType(Object.class));
            }
            // 添加所有参数
            int savedArgIndex = isStatic ? 0 : 1;
            for (int i = 0; i < argTypes.length; i++) {
                dup();
                push(arrayIndex++);
                loadLocal(savedArgLocals[savedArgIndex++]);
                arrayStore(Type.getType(Object.class));
            }
            // 添加返回值或异常（需要 box）
            dup();
            push(arrayIndex);
            if (opcode == ATHROW) {
                loadLocal(resultLocal);
            } else if (returnType.getSort() != Type.VOID) {
                loadLocal(resultLocal);
                box(returnType);
            } else {
                visitInsn(ACONST_NULL);
            }
            arrayStore(Type.getType(Object.class));
            // 调用 CallbackDispatcher.dispatchAfter
            push(specId);
            swap();
            invokeStatic(Type.getObjectType(DISPATCHER), new Method("dispatchAfter", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;"));
            // 处理回调返回值
            if (opcode == ATHROW) {
                // 异常退出：如果回调返回非 null，则替换异常
                dup();
                Label skipReplace = new Label();
                ifNull(skipReplace);
                // 替换异常：将回调返回值作为新异常抛出
                checkCast(Type.getType(Throwable.class));
                throwException();
                mark(skipReplace);
                pop(); // 弹出 null
                // 恢复原异常并抛出
                loadLocal(resultLocal);
                throwException();
            } else {
                // 正常返回：如果回调返回非 null，则替换返回值
                if (returnType.getSort() != Type.VOID) {
                    dup();
                    Label skipReplace = new Label();
                    ifNull(skipReplace);
                    // 使用回调返回值替换
                    if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
                        checkCast(returnType);
                    } else {
                        unbox(returnType);
                    }
                    Label end = new Label();
                    goTo(end);
                    mark(skipReplace);
                    pop(); // 弹出 null
                    loadLocal(resultLocal); // 加载原始返回值（已经是正确类型）
                    mark(end);
                } else {
                    pop(); // void 方法，丢弃回调返回值
                }
            }
        }
        
        /**
         * 保存方法参数到局部变量（用于 AFTER 模式）
         */
        private void saveArgsForAfter() {
            Type[] argTypes = Type.getArgumentTypes(methodDescriptor);
            int savedCount = argTypes.length + (isStatic ? 0 : 1);
            savedArgLocals = new int[savedCount];
            int savedIndex = 0;
            // 保存 this（如果是实例方法）
            if (!isStatic) {
                savedArgLocals[savedIndex] = newLocal(Type.getType(Object.class));
                loadThis();
                storeLocal(savedArgLocals[savedIndex]);
                savedIndex++;
            }
            // 保存所有参数
            for (int i = 0; i < argTypes.length; i++) {
                savedArgLocals[savedIndex] = newLocal(Type.getType(Object.class));
                loadArg(i);
                box(argTypes[i]);
                storeLocal(savedArgLocals[savedIndex]);
                savedIndex++;
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
