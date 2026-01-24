package org.tabooproject.fluxon.parser;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionSignature;
import org.tabooproject.fluxon.runtime.Type;

import java.util.Arrays;

/**
 * 函数声明
 * 用于在编译阶段检测合法函数和函数派发
 */
public class SymbolFunction implements Callable {

    private final String namespace;
    private final String name;
    private final FunctionSignature signature;

    public SymbolFunction(String namespace, String name, FunctionSignature signature) {
        this.namespace = namespace;
        this.name = name;
        this.signature = signature;
    }

    public SymbolFunction(String namespace, String name, int parameterCount) {
        this.namespace = namespace;
        this.name = name;
        // 创建一个只有参数数量的签名（用于用户函数，没有类型信息）
        Type[] params = new Type[parameterCount];
        Arrays.fill(params, Type.OBJECT);
        this.signature = FunctionSignature.returnsObject().params(params);
    }

    @Nullable
    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public FunctionSignature getSignature() {
        return signature;
    }

    @Override
    public int getParameterCount() {
        return signature != null ? signature.getParameterCount() : 0;
    }

    /**
     * 获取返回类型
     */
    public Type getReturnType() {
        return signature != null ? signature.getReturnType() : Type.OBJECT;
    }

    /**
     * 获取参数类型
     */
    public Type[] getParameterTypes() {
        return signature != null ? signature.getParameterTypes() : new Type[0];
    }

    /**
     * 从 Function 创建符号函数
     */
    public static SymbolFunction of(Function function) {
        return new SymbolFunction(function.getNamespace(), function.getName(), function.getSignature());
    }

    /**
     * 创建一个支持任意参数数量的符号函数
     * 用于动态注册的函数，没有明确的参数签名
     */
    public static SymbolFunction varargs(String name) {
        return new VarargsSymbolFunction(null, name);
    }

    @Override
    public String toString() {
        return "SymbolFunction{" +
                "namespace='" + namespace + '\'' +
                ", name='" + name + '\'' +
                ", signature=" + signature +
                '}';
    }
}
