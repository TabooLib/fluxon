package org.tabooproject.fluxon.interpreter.bytecode.emitter;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeGenerator;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.definition.MethodDefinition;
import org.tabooproject.fluxon.parser.expression.AnonymousClassExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FunctionContextPool;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.reflection.ReflectionHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.OBJECT;

/**
 * 匿名类生成器
 * 生成继承指定类或实现指定接口的匿名类
 *
 * @author sky
 */
public class AnonymousClassEmitter extends ClassEmitter {

    private final AnonymousClassExpression expression;
    private final BytecodeGenerator generator;
    private final Class<?> superClass;
    private final List<Class<?>> interfaceClasses;

    /**
     * 创建匿名类生成器
     *
     * @param expression      匿名类表达式
     * @param parentClassName 父类名
     * @param anonymousIndex  匿名类索引
     * @param generator       字节码生成器
     * @param classLoader     类加载器
     * @return 生成器实例
     */
    public static AnonymousClassEmitter create(
            AnonymousClassExpression expression,
            String parentClassName,
            int anonymousIndex,
            BytecodeGenerator generator,
            ClassLoader classLoader
    ) {
        // 解析类型
        Class<?> superClass = Object.class;
        List<Class<?>> interfaces = new ArrayList<>();
        String declaredSuper = expression.getSuperClass();
        List<String> declaredIfaces = expression.getInterfaces();
        // 显式声明父类
        if (declaredSuper != null) {
            superClass = loadClass(declaredSuper, classLoader);
            for (String name : declaredIfaces) {
                Class<?> c = loadClass(name, classLoader);
                if (c.isInterface()) interfaces.add(c);
            }
        }
        // 仅声明接口列表，第一个可能是类
        else if (!declaredIfaces.isEmpty()) {
            Class<?> first = loadClass(declaredIfaces.get(0), classLoader);
            if (first.isInterface()) {
                interfaces.add(first);
            } else {
                superClass = first;
            }
            for (int i = 1; i < declaredIfaces.size(); i++) {
                Class<?> c = loadClass(declaredIfaces.get(i), classLoader);
                if (c.isInterface()) interfaces.add(c);
            }
        }
        return new AnonymousClassEmitter(expression, parentClassName, anonymousIndex, generator, classLoader, superClass, interfaces);
    }

    private AnonymousClassEmitter(
            AnonymousClassExpression expression,
            String parentClassName,
            int anonymousIndex,
            BytecodeGenerator generator,
            ClassLoader classLoader,
            Class<?> superClass,
            List<Class<?>> interfaces
    ) {
        super(parentClassName + "$" + anonymousIndex, superClass.getName().replace('.', '/'), toInternalNames(interfaces), classLoader);
        this.expression = expression;
        this.generator = generator;
        this.superClass = superClass;
        this.interfaceClasses = interfaces;
    }

    @Override
    public EmitResult emit() {
        List<LambdaFunctionDefinition> lambdaDefinitions = new ArrayList<>();
        beginClass(ACC_PUBLIC);
        emitField(ACC_PRIVATE | ACC_FINAL, "environment", Environment.TYPE.getDescriptor(), null);
        emitConstructor();
        for (MethodDefinition method : expression.getMethods()) {
            emitMethod(method, lambdaDefinitions);
        }
        return new EmitResult(endClass(), lambdaDefinitions);
    }

    // ========== 构造函数生成 ==========

    private void emitConstructor() {
        int argCount = getConstructorArgCount();
        Constructor<?> superCtor = findSuperConstructor(argCount);
        if (superCtor == null && argCount > 0) {
            throw new RuntimeException("Cannot find constructor with " + argCount + " parameters in " + superClass.getName());
        }
        String envDesc = Environment.TYPE.getDescriptor();
        String ctorDesc = argCount > 0 ? "(" + envDesc + "[" + OBJECT + ")V" : "(" + envDesc + ")V";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", ctorDesc, null, null);
        mv.visitCode();
        // this.environment = env
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, className, "environment", envDesc);
        // super(...)
        mv.visitVarInsn(ALOAD, 0);
        if (argCount > 0) {
            emitSuperConstructorArgs(mv, superCtor);
            mv.visitMethodInsn(INVOKESPECIAL, superClassName, "<init>", toMethodDescriptor(superCtor), false);
        } else {
            mv.visitMethodInsn(INVOKESPECIAL, superClassName, "<init>", "()V", false);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitSuperConstructorArgs(MethodVisitor mv, Constructor<?> ctor) {
        Class<?>[] paramTypes = ctor.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            mv.visitVarInsn(ALOAD, 2);
            mv.visitLdcInsn(i);
            mv.visitInsn(AALOAD);
            if (paramTypes[i].isPrimitive()) {
                Instructions.emitTypeConversion(mv, paramTypes[i]);
            } else if (paramTypes[i] != Object.class) {
                mv.visitTypeInsn(CHECKCAST, paramTypes[i].getName().replace('.', '/'));
            }
        }
    }

    // ========== 方法生成 ==========

    private void emitMethod(MethodDefinition methodDef, List<LambdaFunctionDefinition> lambdaDefinitions) {
        Method target = ReflectionHelper.findOverridableMethod(superClass, interfaceClasses, methodDef.getName(), methodDef.getParameterNames().size());
        if (target == null) {
            throw new RuntimeException("Cannot find method to override: " + methodDef.getName());
        }
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodDef.getName(), org.objectweb.asm.Type.getMethodDescriptor(target), null, null);
        mv.visitCode();
        // 初始化上下文
        CodeContext ctx = new CodeContext(className, superClassName);
        ctx.allocateLocalVar(OBJECT); // slot 0: this
        ctx.setExpectedReturnType(target.getReturnType());
        // 分配参数槽位
        Class<?>[] paramTypes = target.getParameterTypes();
        List<String> paramNames = methodDef.getParameterNames();
        for (int i = 0; i < paramNames.size() && i < paramTypes.length; i++) {
            ctx.allocateLocalVar(new Type(paramTypes[i]));
        }
        // 获取 FunctionContextPool.local() 用于函数调用
        mv.visitMethodInsn(INVOKESTATIC, FunctionContextPool.TYPE.getPath(), "local", "()" + FunctionContextPool.TYPE.getDescriptor(), false);
        int poolSlot = ctx.allocateLocalVar(OBJECT);
        mv.visitVarInsn(ASTORE, poolSlot);
        ctx.setPoolLocalSlot(poolSlot);
        // 绑定参数到子环境
        int localVarCount = methodDef.getLocalVariables().size();
        Instructions.emitChildEnvironmentWithParams(mv, ctx, className, paramNames, paramTypes, 1, localVarCount);
        // 生成方法体
        Type returnType = Instructions.emitMethodBody(generator, methodDef.getBody(), ctx, mv);
        Instructions.emitReturn(mv, target.getReturnType(), returnType);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        lambdaDefinitions.addAll(ctx.getLambdaDefinitions());
    }

    // ========== 反射查找 ==========

    private Constructor<?> findSuperConstructor(int argCount) {
        if (argCount == 0) {
            try {
                return superClass.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
        Constructor<?> best = null;
        int bestScore = -1;
        for (Constructor<?> ctor : superClass.getDeclaredConstructors()) {
            if (ctor.getParameterCount() == argCount && !Modifier.isPrivate(ctor.getModifiers())) {
                int score = scoreConstructor(ctor);
                if (score > bestScore) {
                    bestScore = score;
                    best = ctor;
                }
            }
        }
        return best;
    }

    private int scoreConstructor(Constructor<?> ctor) {
        int score = 0;
        for (Class<?> t : ctor.getParameterTypes()) {
            if (t == String.class) score += 10;
            else if (t == Object.class) score += 5;
            else if (t == CharSequence.class) score += 8;
            else if (t.isInterface()) score += 1;
            else score += 3;
        }
        return score;
    }

    // ========== 工具方法 ==========

    private static Class<?> loadClass(String name, ClassLoader cl) {
        try {
            return Class.forName(name, false, cl);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot find class: " + name, e);
        }
    }

    private static String[] toInternalNames(List<Class<?>> classes) {
        if (classes.isEmpty()) return null;
        String[] names = new String[classes.size()];
        for (int i = 0; i < classes.size(); i++) {
            names[i] = classes.get(i).getName().replace('.', '/');
        }
        return names;
    }

    private static String toMethodDescriptor(Constructor<?> ctor) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> t : ctor.getParameterTypes()) {
            sb.append(org.objectweb.asm.Type.getDescriptor(t));
        }
        return sb.append(")V").toString();
    }

    public int getConstructorArgCount() {
        return expression.getSuperArgs() != null ? expression.getSuperArgs().size() : 0;
    }
}
