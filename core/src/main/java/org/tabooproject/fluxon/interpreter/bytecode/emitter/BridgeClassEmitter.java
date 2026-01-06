package org.tabooproject.fluxon.interpreter.bytecode.emitter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeUtils;
import org.tabooproject.fluxon.runtime.java.ClassBridge;
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
    private final String[] methodNames;

    /**
     * 构造 Bridge 类生成器
     *
     * @param exportMethods 导出方法数组
     * @param classLoader   类加载器
     */
    public BridgeClassEmitter(Method[] exportMethods, ClassLoader classLoader) {
        super(generateClassName(), ClassBridge.TYPE.getPath(), classLoader);
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

    // ========== 方法生成 ==========

    /**
     * 生成 invoke 方法
     */
    private void emitInvokeMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "invoke", "(" + STRING + OBJECT + "[" + OBJECT + ")" + OBJECT, null, new String[]{"java/lang/Exception"});
        mv.visitCode();
        if (!dispatchStrategy.hasExportMethods()) {
            BytecodeUtils.emitThrowException(mv, "java/lang/IllegalArgumentException", "No exported methods available");
        } else {
            dispatchStrategy.emit(mv, this::emitSingleMethodCall);
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
            BytecodeUtils.emitThrowException(mv, "java/lang/IllegalArgumentException", "No exported methods available");
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
            BytecodeUtils.generateSafeParameterAccess(mv, i, paramTypes[i], parameters[i]);
        }
        // 调用目标方法并处理返回值
        BytecodeUtils.emitMethodCallWithReturn(mv, method);
        mv.visitInsn(ARETURN);
    }

    /**
     * 生成单个方法的参数类型返回代码
     */
    private void emitSingleMethodParameterTypes(MethodVisitor mv, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        BytecodeUtils.emitClassArray(mv, paramTypes);
        mv.visitInsn(ARETURN);
    }
}
