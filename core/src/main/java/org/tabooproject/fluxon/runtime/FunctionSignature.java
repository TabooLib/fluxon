package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;

/**
 * 函数签名类
 * 包含返回类型和参数类型信息，用于类型推断
 *
 * @author sky
 */
public final class FunctionSignature {

    private static final Type[] EMPTY_TYPES = new Type[0];

    private final Type returnType;
    private final Type[] parameterTypes;

    private FunctionSignature(Type returnType, Type[] parameterTypes) {
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
    }

    @NotNull
    public Type getReturnType() {
        return returnType;
    }

    @NotNull
    public Type[] getParameterTypes() {
        return parameterTypes;
    }

    public int getParameterCount() {
        return parameterTypes.length;
    }

    /**
     * DSL 构建起点：指定返回类型
     */
    public static Builder returns(Type returnType) {
        return new Builder(returnType);
    }

    /**
     * 创建一个返回 OBJECT 类型的签名
     */
    public static Builder returnsObject() {
        return new Builder(Type.OBJECT);
    }

    /**
     * 创建一个返回 void 的签名
     */
    public static Builder returnsVoid() {
        return new Builder(Type.VOID);
    }

    /**
     * 函数签名构建器
     */
    public static class Builder {
        private final Type returnType;

        Builder(Type returnType) {
            this.returnType = returnType;
        }

        /**
         * 指定参数类型
         */
        public FunctionSignature params(Type... types) {
            return new FunctionSignature(returnType, types);
        }

        /**
         * 无参函数
         */
        public FunctionSignature noParams() {
            return new FunctionSignature(returnType, EMPTY_TYPES);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameterTypes[i]);
        }
        sb.append(") -> ").append(returnType);
        return sb.toString();
    }
}
