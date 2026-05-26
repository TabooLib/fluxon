package org.tabooproject.fluxon.interpreter.bytecode.emitter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.java.ClassBridge;
import org.tabooproject.fluxon.runtime.java.Optional;
import org.tabooproject.fluxon.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.atomic.AtomicLong;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.*;

/**
 * ClassBridge 类生成器
 * 生成实现 ClassBridge 的高性能方法调用器
 */
public class BridgeClassEmitter extends ClassEmitter {

    private static final AtomicLong classCounter = new AtomicLong(0);

    private final DispatchStrategy dispatchStrategy;
    private final Method[] exportMethods;
    private final String[] methodNames;

    /**
     * 构造 Bridge 类生成器
     *
     * @param exportMethods 导出方法数组
     * @param classLoader   类加载器
     */
    public BridgeClassEmitter(Method[] exportMethods, ClassLoader classLoader) {
        super(generateClassName(), ClassBridge.TYPE.getPath(), classLoader);
        this.exportMethods = exportMethods;
        this.dispatchStrategy = new DispatchStrategy(exportMethods);
        this.methodNames = StringUtils.transformMethodNames(exportMethods);
    }

    /**
     * 生成唯一的类名
     */
    private static String generateClassName() {
        return ClassBridge.TYPE.getPath() + classCounter.incrementAndGet();
    }

    @Override
    public EmitResult emit() {
        // 类声明
        beginClass(ACC_PUBLIC | ACC_SUPER);
        // 生成构造函数
        emitConstructorWithSuperArgs("([" + STRING + ")V", "([" + STRING + ")V", mv -> mv.visitVarInsn(ALOAD, 1));
        // 生成 invoke 方法
        emitInvokeMethod();
        // 生成 FunctionContext 直连调用方法
        emitContextCallMethod();
        // 生成 getParameterTypes 方法
        emitGetParameterTypesMethod();
        return new EmitResult(endClass());
    }

    /**
     * 获取方法名数组（用于构造 ClassBridge 实例）
     */
    public String[] getMethodNames() {
        return methodNames;
    }

    /**
     * 生成 invoke 方法
     */
    private void emitInvokeMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "invoke", "(" + STRING + OBJECT + "[" + OBJECT + ")" + OBJECT, null, new String[]{"java/lang/Exception"});
        mv.visitCode();
        if (!dispatchStrategy.hasExportMethods()) {
            Instructions.emitThrowException(mv, "java/lang/IllegalArgumentException", "No exported methods available");
        } else {
            dispatchStrategy.emit(mv, this::emitSingleMethodCall);
        }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * 生成按注册索引直接调用 Export 方法的热路径。
     */
    private void emitContextCallMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", "(I" + FunctionContext.TYPE + ")V", null, null);
        mv.visitCode();
        if (exportMethods.length == 0) {
            Instructions.emitThrowException(mv, "java/lang/IllegalArgumentException", "No exported methods available");
        } else {
            Label defaultLabel = new Label();
            Label endLabel = new Label();
            Label[] labels = new Label[exportMethods.length];
            for (int i = 0; i < labels.length; i++) {
                labels[i] = new Label();
            }
            mv.visitVarInsn(ILOAD, 1);
            mv.visitTableSwitchInsn(0, exportMethods.length - 1, defaultLabel, labels);
            for (int i = 0; i < exportMethods.length; i++) {
                mv.visitLabel(labels[i]);
                emitSingleContextMethodCall(mv, exportMethods[i]);
                mv.visitJumpInsn(GOTO, endLabel);
            }
            mv.visitLabel(defaultLabel);
            Instructions.emitThrowException(mv, "java/lang/IllegalArgumentException", "Unknown export method index");
            mv.visitLabel(endLabel);
            mv.visitInsn(RETURN);
        }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * 生成 getParameterTypes 方法
     */
    private void emitGetParameterTypesMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getParameterTypes", "(" + STRING + OBJECT + "[" + OBJECT + ")[" + CLASS, null, new String[]{"java/lang/Exception"});
        mv.visitCode();
        if (!dispatchStrategy.hasExportMethods()) {
            Instructions.emitThrowException(mv, "java/lang/IllegalArgumentException", "No exported methods available");
        } else {
            dispatchStrategy.emit(mv, this::emitSingleMethodParameterTypes);
        }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * 生成单个方法的调用代码
     */
    private void emitSingleMethodCall(MethodVisitor mv, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Parameter[] parameters = method.getParameters();
        // 加载实例对象并进行类型转换
        mv.visitVarInsn(ALOAD, 2);
        mv.visitTypeInsn(CHECKCAST, Type.getInternalName(method.getDeclaringClass()));
        // 为每个参数安全获取值
        for (int i = 0; i < paramTypes.length; i++) {
            Instructions.emitSafeParameterAccess(mv, i, paramTypes[i], parameters[i]);
        }
        // 调用目标方法并处理返回值
        Instructions.emitMethodCallWithReturn(mv, method);
        mv.visitInsn(ARETURN);
    }

    /**
     * 生成 FunctionContext 直连方法调用代码。
     */
    private void emitSingleContextMethodCall(MethodVisitor mv, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Parameter[] parameters = method.getParameters();
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getTarget", "()" + OBJECT, false);
        mv.visitTypeInsn(CHECKCAST, Type.getInternalName(method.getDeclaringClass()));
        for (int i = 0; i < paramTypes.length; i++) {
            emitContextParameterAccess(mv, i, paramTypes[i], parameters[i]);
        }
        Instructions.emitMethodCall(mv, method);
        emitContextReturn(mv, method.getReturnType());
    }

    /**
     * 从 FunctionContext 读取参数，避免先装箱到 Object[]。
     */
    private void emitContextParameterAccess(MethodVisitor mv, int paramIndex, Class<?> paramType, Parameter parameter) {
        if (parameter.isAnnotationPresent(Optional.class)) {
            Label hasParam = new Label();
            Label endLabel = new Label();
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getArgumentCount", "()I", false);
            mv.visitLdcInsn(paramIndex + 1);
            mv.visitJumpInsn(IF_ICMPGE, hasParam);
            Instructions.emitDefaultValue(mv, paramType);
            mv.visitJumpInsn(GOTO, endLabel);
            mv.visitLabel(hasParam);
            emitContextArgumentValue(mv, paramIndex, paramType);
            mv.visitLabel(endLabel);
            return;
        }
        emitContextArgumentValue(mv, paramIndex, paramType);
    }

    /**
     * 按目标 Java 参数类型读取 context 参数。
     */
    private void emitContextArgumentValue(MethodVisitor mv, int paramIndex, Class<?> paramType) {
        mv.visitVarInsn(ALOAD, 2);
        mv.visitLdcInsn(paramIndex);
        if (paramType == int.class || paramType == byte.class || paramType == short.class || paramType == char.class) {
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsInt", "(I)I", false);
        } else if (paramType == boolean.class) {
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsBoolean", "(I)Z", false);
        } else if (paramType == long.class) {
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsLong", "(I)J", false);
        } else if (paramType == float.class) {
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsFloat", "(I)F", false);
        } else if (paramType == double.class) {
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsDouble", "(I)D", false);
        } else {
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getArgBoxed", "(I)" + OBJECT, false);
            if (paramType != Object.class) {
                mv.visitTypeInsn(CHECKCAST, Type.getInternalName(paramType));
            }
        }
    }

    /**
     * 将目标方法返回值直接写回 FunctionContext。
     */
    private void emitContextReturn(MethodVisitor mv, Class<?> returnType) {
        if (returnType == void.class) {
            mv.visitVarInsn(ALOAD, 2);
            mv.visitInsn(ACONST_NULL);
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "setReturnRef", "(" + OBJECT + ")V", false);
        } else if (returnType == int.class) {
            mv.visitVarInsn(ISTORE, 3);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "setReturnInt", "(I)V", false);
        } else if (returnType == boolean.class) {
            mv.visitVarInsn(ISTORE, 3);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "setReturnBool", "(Z)V", false);
        } else if (returnType == long.class) {
            mv.visitVarInsn(LSTORE, 3);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(LLOAD, 3);
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "setReturnLong", "(J)V", false);
        } else if (returnType == float.class) {
            mv.visitVarInsn(FSTORE, 3);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(FLOAD, 3);
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "setReturnFloat", "(F)V", false);
        } else if (returnType == double.class) {
            mv.visitVarInsn(DSTORE, 3);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(DLOAD, 3);
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "setReturnDouble", "(D)V", false);
        } else {
            if (returnType.isPrimitive()) {
                Instructions.emitBoxing(mv, returnType);
            }
            mv.visitVarInsn(ASTORE, 3);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "setReturnRef", "(" + OBJECT + ")V", false);
        }
    }

    /**
     * 生成单个方法的参数类型返回代码
     */
    private void emitSingleMethodParameterTypes(MethodVisitor mv, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Instructions.emitClassArray(mv, paramTypes);
        mv.visitInsn(ARETURN);
    }
}
