package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.parser.definition.Annotation;

import java.util.Collections;
import java.util.List;

/**
 * 原生函数类
 * 表示由 Java 实现的内置函数
 */
public class NativeFunction<Target> implements Function, Symbolic {

    private final String namespace;
    private final String name;
    private final FunctionSignature signature;
    private final NativeCallable<Target> callable;
    private final boolean isAsync;
    private final boolean isPrimarySync;
    private final DirectBinding directBinding;

    public NativeFunction(
            String namespace,
            String name,
            FunctionSignature signature,
            NativeCallable<Target> callable,
            boolean isAsync,
            boolean isPrimarySync) {
        this(namespace, name, signature, callable, isAsync, isPrimarySync, null);
    }

    public NativeFunction(
            String namespace,
            String name,
            FunctionSignature signature,
            NativeCallable<Target> callable,
            boolean isAsync,
            boolean isPrimarySync,
            DirectBinding directBinding) {
        this.namespace = namespace;
        this.name = name;
        this.signature = signature;
        this.callable = callable;
        this.isAsync = isAsync;
        this.isPrimarySync = isPrimarySync;
        this.directBinding = directBinding;
    }

    public NativeFunction(String name, FunctionSignature signature, NativeCallable<Target> callable) {
        this(null, name, signature, callable, false, false, null);
    }

    public NativeFunction(String name, FunctionSignature signature, NativeCallable<Target> callable, DirectBinding directBinding) {
        this(null, name, signature, callable, false, false, directBinding);
    }

    public NativeFunction(String namespace, String name, FunctionSignature signature, NativeCallable<Target> callable) {
        this(namespace, name, signature, callable, false, false, null);
    }

    @Nullable
    @Override
    public String getNamespace() {
        return namespace;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public FunctionSignature getSignature() {
        return signature;
    }

    @Override
    public boolean isAsync() {
        return isAsync;
    }

    @Override
    public boolean isPrimarySync() {
        return isPrimarySync;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void call(@NotNull final FunctionContext<?> context) {
        callable.call((FunctionContext<Target>) context);
    }

    @Override
    public SymbolFunction getInfo() {
        return new SymbolFunction(namespace, name, signature != null ? signature.getParameterCount() : 0);
    }

    public NativeCallable<Target> getCallable() {
        return callable;
    }

    @Override
    public DirectBinding getDirectBinding() {
        return directBinding;
    }

    @Override
    public String toString() {
        return "NativeFunction{" +
                "name='" + name + '\'' +
                ", signature=" + signature +
                ", isAsync=" + isAsync +
                '}';
    }

    /**
     * 原生函数接口
     */
    @FunctionalInterface
    public interface NativeCallable<Target> {

        /**
         * 调用原生函数
         *
         * @param context 函数上下文，包含调用目标、参数列表和环境
         */
        void call(@NotNull FunctionContext<Target> context);
    }
}
