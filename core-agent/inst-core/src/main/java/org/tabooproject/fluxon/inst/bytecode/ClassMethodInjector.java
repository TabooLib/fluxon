package org.tabooproject.fluxon.inst.bytecode;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;
import org.tabooproject.fluxon.inst.CallbackDispatcher;
import org.tabooproject.fluxon.inst.InjectionSpec;
import org.tabooproject.fluxon.inst.InjectionType;

import java.util.List;

import static org.tabooproject.fluxon.runtime.Type.OBJECT;
import static org.tabooproject.fluxon.runtime.Type.STRING;

/**
 * 方法注入器。
 * 使用 ASM 在目标方法中注入回调代码。
 */
public class ClassMethodInjector {

    /**
     * 对类字节码应用注入。
     */
    public static byte[] inject(byte[] originalBytecode, List<InjectionSpec> specs) {
        ClassReader reader = new ClassReader(originalBytecode);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new InjectionClassVisitor(writer, specs);
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static class InjectionClassVisitor extends ClassVisitor {
        private final List<InjectionSpec> specs;

        InjectionClassVisitor(ClassVisitor cv, List<InjectionSpec> specs) {
            super(Opcodes.ASM9, cv);
            this.specs = specs;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            for (InjectionSpec spec : specs) {
                if (spec.matchesMethod(name, descriptor)) {
                    mv = new InjectionMethodAdapter(mv, access, name, descriptor, spec);
                }
            }
            return mv;
        }
    }

    private static class InjectionMethodAdapter extends AdviceAdapter {
        private final InjectionSpec spec;
        private final String methodDescriptor;

        InjectionMethodAdapter(MethodVisitor mv, int access, String name, String descriptor, InjectionSpec spec) {
            super(Opcodes.ASM9, mv, access, name, descriptor);
            this.spec = spec;
            this.methodDescriptor = descriptor;
        }

        @Override
        protected void onMethodEnter() {
            if (spec.getType() == InjectionType.BEFORE) {
                injectBeforeAdvice();
            } else if (spec.getType() == InjectionType.REPLACE) {
                injectReplaceAdvice();
            }
        }

        private void injectBeforeAdvice() {
            // 创建参数数组并调用 dispatchBefore
            createArgsArray();
            push(spec.getId());
            swap();
            invokeStatic(Type.getObjectType(CallbackDispatcher.TYPE.getPath()), new Method("dispatchBefore", "(" + STRING + "[" + OBJECT + ")Z"));
            pop();
        }

        private void injectReplaceAdvice() {
            // 创建参数数组并调用 dispatchReplace
            createArgsArray();
            push(spec.getId());
            swap();
            invokeStatic(Type.getObjectType(CallbackDispatcher.TYPE.getPath()), new Method("dispatchReplace", "(" + STRING + "[" + OBJECT + ")" + OBJECT));
            // 根据返回类型处理
            Type returnType = Type.getReturnType(methodDescriptor);
            if (returnType.getSort() == Type.VOID) {
                pop();
                returnValue();
            } else if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
                // 引用类型：直接强制转换
                checkCast(returnType);
                returnValue();
            } else {
                // 原始类型：拆箱
                unbox(returnType);
                returnValue();
            }
        }

        private void createArgsArray() {
            Type[] argTypes = Type.getArgumentTypes(methodDescriptor);
            boolean isStatic = (methodAccess & Opcodes.ACC_STATIC) != 0;
            int arraySize = argTypes.length + (isStatic ? 0 : 1);

            // 创建 Object[] 数组
            push(arraySize);
            newArray(Type.getType(Object.class));

            int arrayIndex = 0;
            int localIndex = 0;

            // 实例方法添加 this
            if (!isStatic) {
                dup();
                push(arrayIndex++);
                loadThis();
                arrayStore(Type.getType(Object.class));
                localIndex++;
            }

            // 添加所有参数（使用继承的 box 方法装箱）
            for (Type argType : argTypes) {
                dup();
                push(arrayIndex++);
                loadArg(localIndex - (isStatic ? 0 : 1));
                box(argType);
                arrayStore(Type.getType(Object.class));
                localIndex++;
            }
        }
    }
}
