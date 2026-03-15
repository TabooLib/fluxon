package org.tabooproject.fluxon.runtime;

import java.lang.reflect.Method;

/**
 * 直接绑定信息
 * 将 Fluxon 函数映射到 JVM 静态方法，编译器可直接生成 INVOKESTATIC 跳过函数调用框架
 *
 * @author sky
 */
public final class DirectBinding {

    final String owner;
    final String method;
    final String descriptor;

    public DirectBinding(String owner, String method, String descriptor) {
        this.owner = owner;
        this.method = method;
        this.descriptor = descriptor;
    }

    /**
     * 从 Class 和方法名自动生成绑定，根据函数签名推导 JVM descriptor
     */
    public static DirectBinding of(Class<?> clazz, String methodName, FunctionSignature signature) {
        String owner = clazz.getName().replace('.', '/');
        String descriptor = buildDescriptor(signature);
        return new DirectBinding(owner, methodName, descriptor);
    }

    /**
     * 从 Java Method 反射信息直接生成绑定
     * descriptor 精确匹配 Java 方法签名（包括 boolean/int 的区分）
     */
    public static DirectBinding ofMethod(Class<?> clazz, Method method) {
        String owner = clazz.getName().replace('.', '/');
        String descriptor = org.objectweb.asm.Type.getMethodDescriptor(method);
        return new DirectBinding(owner, method.getName(), descriptor);
    }

    public String getOwner() {
        return owner;
    }

    public String getMethod() {
        return method;
    }

    public String getDescriptor() {
        return descriptor;
    }

    private static String buildDescriptor(FunctionSignature signature) {
        StringBuilder sb = new StringBuilder("(");
        for (Type paramType : signature.getParameterTypes()) {
            sb.append(toJvmType(paramType));
        }
        sb.append(")");
        sb.append(toJvmType(signature.getReturnType()));
        return sb.toString();
    }

    private static String toJvmType(Type type) {
        if (type == Type.I || type == Type.Z) return "I";
        if (type == Type.J) return "J";
        if (type == Type.D) return "D";
        if (type == Type.F) return "F";
        if (type == Type.VOID) return "V";
        return "Ljava/lang/Object;";
    }

    @Override
    public String toString() {
        return owner + "." + method + descriptor;
    }
}
