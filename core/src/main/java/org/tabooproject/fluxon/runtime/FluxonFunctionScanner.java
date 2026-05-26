package org.tabooproject.fluxon.runtime;

import org.tabooproject.fluxon.interpreter.bytecode.Primitives;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;

/**
 * Fluxon 函数扫描器
 * 扫描类中 @FluxonFunction 标注的静态方法，自动注册为系统函数或扩展函数
 * 同时扫描 @FluxonOperator，把扩展函数旁边声明的运算符重载注册到复合赋值路径。
 * 每个方法同时生成 NativeFunction（解释器桥接）和 DirectBinding（编译器 INVOKESTATIC）
 * 解释器桥接通过 LambdaMetafactory 生成直接调用，运行时零反射开销
 *
 * @author sky
 */
public class FluxonFunctionScanner {

    /**
     * 扫描指定类，注册所有 @FluxonFunction 方法
     */
    public static void register(FluxonRuntime runtime, Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            FluxonFunction annotation = method.getAnnotation(FluxonFunction.class);
            FluxonOperator operator = method.getAnnotation(FluxonOperator.class);
            if (annotation == null && operator == null) continue;
            requirePublicStatic(method, annotation != null ? "@FluxonFunction" : "@FluxonOperator");
            if (operator != null) {
                registerOperator(method, operator);
            }
            if (annotation == null) {
                continue;
            }
            Class<?> target = annotation.target();
            if (target == FluxonFunction.SystemFunction.class) {
                registerSystemFunction(runtime, clazz, method, annotation);
            } else {
                registerExtensionFunction(runtime, clazz, method, annotation, target);
            }
        }
    }

    static void requirePublicStatic(Method method, String annotationName) {
        if (!Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())) {
            throw new IllegalStateException(annotationName + " 方法必须为 public static: " + method);
        }
    }

    static void registerOperator(Method method, FluxonOperator operator) {
        OperatorOverloadRegistry.register(operator.value(), operator.target(), method, operator.returnsTarget());
    }

    /**
     * 注册系统函数
     * 若方法第一个参数为 FunctionContext，视为上下文注入参数，不计入 Fluxon 函数签名，
     * 同时禁用 DirectBinding（走框架路径）
     */
    private static void registerSystemFunction(FluxonRuntime runtime, Class<?> clazz, Method method, FluxonFunction annotation) {
        String name = annotation.value().isEmpty() ? method.getName() : annotation.value();
        String namespace = annotation.namespace().isEmpty() ? null : annotation.namespace();
        boolean needsContext = hasContextParam(method, 0);
        int skipParams = needsContext ? 1 : 0;
        FunctionSignature signature = buildSignature(method, skipParams);
        DirectBinding binding = needsContext ? null : DirectBinding.ofMethod(clazz, method);
        NativeFunction.NativeCallable<?> callable = needsContext
                ? buildContextAwareCallable(method, false)
                : buildSystemCallable(method);
        if (namespace != null) {
            runtime.registerFunction(new NativeFunction<>(namespace, name, signature, callable, false, false, binding));
        } else {
            runtime.registerFunction(new NativeFunction<>(name, signature, callable, binding));
        }
    }

    /**
     * 注册扩展函数
     * 方法第一个参数为 target 类型，FunctionSignature 从第二个参数开始构建
     * 若第二个参数为 FunctionContext，视为上下文注入参数，额外跳过，禁用 DirectBinding
     * DirectBinding 包含 target 参数（编译器直接 load target + INVOKESTATIC）
     * NativeCallable 桥接时第一个参数从 context.getTarget() 取
     */
    @SuppressWarnings("unchecked")
    private static void registerExtensionFunction(FluxonRuntime runtime, Class<?> clazz, Method method, FluxonFunction annotation, Class<?> target) {
        Class<?>[] paramClasses = method.getParameterTypes();
        if (paramClasses.length == 0 || !paramClasses[0].isAssignableFrom(target)) {
            throw new IllegalStateException(
                    "@FluxonFunction 扩展函数第一个参数必须为 target 类型 (" + target.getName() + "): " + method
            );
        }
        String name = annotation.value().isEmpty() ? method.getName() : annotation.value();
        String namespace = annotation.namespace().isEmpty() ? null : annotation.namespace();
        boolean needsContext = hasContextParam(method, 1);
        int skipParams = needsContext ? 2 : 1;
        FunctionSignature signature = buildSignature(method, skipParams);
        DirectBinding binding = needsContext ? null : DirectBinding.ofMethod(clazz, method);
        NativeFunction.NativeCallable<?> callable = needsContext
                ? buildContextAwareCallable(method, true)
                : buildExtensionCallable(method);
        @SuppressWarnings("rawtypes")
        NativeFunction nf = new NativeFunction<>(namespace, name, signature, (NativeFunction.NativeCallable) callable, false, false, binding);
        runtime.registerExtensionFunction((Class) target, nf);
    }

    /**
     * 从 Java 方法签名推导 FunctionSignature
     *
     * @param skipParams 跳过前 N 个参数（扩展函数跳过 target 参数，上下文注入时额外跳过 FunctionContext）
     */
    private static FunctionSignature buildSignature(Method method, int skipParams) {
        Class<?>[] paramClasses = method.getParameterTypes();
        int userParamCount = paramClasses.length - skipParams;
        Type[] paramTypes = new Type[userParamCount];
        for (int i = 0; i < userParamCount; i++) {
            paramTypes[i] = Type.fromClass(paramClasses[i + skipParams]);
        }
        Type returnType = Type.fromClass(method.getReturnType());
        return FunctionSignature.returns(returnType).paramsWithMin(requiredParameterCount(method, skipParams), paramTypes);
    }

    /**
     * @Optional 只允许省略尾部参数，注册签名时同步暴露最少实参数。
     */
    private static int requiredParameterCount(Method method, int skipParams) {
        Parameter[] parameters = method.getParameters();
        int required = parameters.length - skipParams;
        for (int i = parameters.length - 1; i >= skipParams; i--) {
            if (!parameters[i].isAnnotationPresent(org.tabooproject.fluxon.runtime.java.Optional.class)) {
                break;
            }
            required--;
        }
        return required;
    }

    /**
     * 检测方法在 offset 位置的参数是否为 FunctionContext 类型
     */
    private static boolean hasContextParam(Method method, int offset) {
        Class<?>[] params = method.getParameterTypes();
        return params.length > offset && FunctionContext.class.isAssignableFrom(params[offset]);
    }

    /**
     * 生成需要 FunctionContext 注入的 NativeCallable 桥接
     * 通过 MethodHandle 调用，FunctionContext 作为第一个（系统函数）或第二个（扩展函数）参数
     * 不使用 LambdaMetafactory，因为上下文参数不来自 getArgBoxed
     *
     * @param isExtension 是否为扩展函数（第一个参数为 target）
     */
    private static NativeFunction.NativeCallable<?> buildContextAwareCallable(Method method, boolean isExtension) {
        try {
            MethodHandle mh = MethodHandles.lookup().unreflect(method);
            int totalParams = method.getParameterCount();
            // 跳过 target（如果是扩展函数）和 FunctionContext
            int skipParams = isExtension ? 2 : 1;
            int userParams = totalParams - skipParams;
            ReturnWriter writer = returnWriter(method.getReturnType());
            return ctx -> {
                Object[] args = new Object[totalParams];
                int idx = 0;
                if (isExtension) args[idx++] = ctx.getTarget();
                args[idx++] = ctx;
                for (int i = 0; i < userParams; i++) {
                    args[idx++] = ctx.getArgBoxed(i);
                }
                try {
                    Object result = mh.invokeWithArguments(args);
                    writer.write(ctx, result);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("无法访问 @FluxonFunction 方法: " + method, e);
        }
    }

    // region LambdaMetafactory 直接调用

    // 按参数数量定义的通用调用接口，LambdaMetafactory 自动处理类型转换（装箱/拆箱/类型强转）
    @FunctionalInterface public interface Invoker0 { Object invoke(); }
    @FunctionalInterface public interface Invoker1 { Object invoke(Object a); }
    @FunctionalInterface public interface Invoker2 { Object invoke(Object a, Object b); }
    @FunctionalInterface public interface Invoker3 { Object invoke(Object a, Object b, Object c); }
    @FunctionalInterface public interface Invoker4 { Object invoke(Object a, Object b, Object c, Object d); }
    @FunctionalInterface public interface Invoker5 { Object invoke(Object a, Object b, Object c, Object d, Object e); }

    // void 返回类型的调用接口
    @FunctionalInterface public interface VoidInvoker0 { void invoke(); }
    @FunctionalInterface public interface VoidInvoker1 { void invoke(Object a); }
    @FunctionalInterface public interface VoidInvoker2 { void invoke(Object a, Object b); }
    @FunctionalInterface public interface VoidInvoker3 { void invoke(Object a, Object b, Object c); }
    @FunctionalInterface public interface VoidInvoker4 { void invoke(Object a, Object b, Object c, Object d); }
    @FunctionalInterface public interface VoidInvoker5 { void invoke(Object a, Object b, Object c, Object d, Object e); }

    private static final Class<?>[] INVOKER_CLASSES = {
            Invoker0.class, Invoker1.class, Invoker2.class,
            Invoker3.class, Invoker4.class, Invoker5.class
    };

    private static final Class<?>[] VOID_INVOKER_CLASSES = {
            VoidInvoker0.class, VoidInvoker1.class, VoidInvoker2.class,
            VoidInvoker3.class, VoidInvoker4.class, VoidInvoker5.class
    };

    /**
     * 通过 LambdaMetafactory 为静态方法生成直接调用的 invoker
     * 注册时一次性生成，运行时零反射开销，性能等同手写 lambda
     * 超过 5 个参数时返回 null，由调用方使用 MethodHandle fallback
     */
    private static Object createInvoker(Method method) {
        int arity = method.getParameterCount();
        if (arity >= INVOKER_CLASSES.length) return null;
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle mh = lookup.unreflect(method);
            boolean isVoid = method.getReturnType() == void.class;
            Class<?> invokerClass = isVoid ? VOID_INVOKER_CLASSES[arity] : INVOKER_CLASSES[arity];
            // SAM 类型：void 方法返回 void，否则全 Object
            MethodType samType;
            if (isVoid) {
                Class<?>[] objectParams = new Class<?>[arity];
                Arrays.fill(objectParams, Object.class);
                samType = MethodType.methodType(void.class, objectParams);
            } else {
                samType = MethodType.genericMethodType(arity);
            }
            // instantiatedMethodType 必须用装箱类型（原始类型不是 Object 子类型，LambdaMetafactory 校验会失败）
            MethodType instantiatedType = boxMethodType(mh.type());
            CallSite site = LambdaMetafactory.metafactory(
                    lookup, "invoke",
                    MethodType.methodType(invokerClass),
                    samType, mh, instantiatedType
            );
            return site.getTarget().invoke();
        } catch (Throwable e) {
            throw new IllegalStateException("无法为 @FluxonFunction 创建 invoker: " + method, e);
        }
    }

    /**
     * 将 MethodType 中的原始类型参数和返回值替换为对应装箱类型
     * void 返回值保持不变
     */
    private static MethodType boxMethodType(MethodType type) {
        Class<?> returnType = type.returnType();
        if (returnType.isPrimitive() && returnType != void.class) {
            returnType = Primitives.boxToClass(returnType);
        }
        Class<?>[] params = type.parameterArray();
        for (int i = 0; i < params.length; i++) {
            if (params[i].isPrimitive()) {
                params[i] = Primitives.boxToClass(params[i]);
            }
        }
        return MethodType.methodType(returnType, params);
    }

    /**
     * MethodHandle fallback：超过 5 个参数时使用
     * 比 Method.invoke 快（无安全检查），但不如 LambdaMetafactory
     */
    private static NativeFunction.NativeCallable<?> buildMethodHandleFallback(Method method, int skipParams) {
        try {
            MethodHandle mh = MethodHandles.lookup().unreflect(method);
            int totalParams = method.getParameterCount();
            int userParams = totalParams - skipParams;
            ReturnWriter writer = returnWriter(method.getReturnType());
            boolean isExtension = skipParams > 0;
            return ctx -> {
                Object[] args = new Object[totalParams];
                if (isExtension) args[0] = ctx.getTarget();
                for (int i = 0; i < userParams; i++) {
                    args[i + skipParams] = ctx.getArgBoxed(i);
                }
                try {
                    Object result = mh.invokeWithArguments(args);
                    writer.write(ctx, result);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("无法访问 @FluxonFunction 方法: " + method, e);
        }
    }

    /**
     * 生成系统函数的 NativeCallable 桥接（解释器使用）
     * 从 FunctionContext 取参数，通过 LambdaMetafactory 生成的 invoker 直接调用静态方法
     */
    private static NativeFunction.NativeCallable<?> buildSystemCallable(Method method) {
        Object invoker = createInvoker(method);
        if (invoker == null) return buildMethodHandleFallback(method, 0);
        int arity = method.getParameterCount();
        if (method.getReturnType() == void.class) {
            return buildVoidSystemCallable(invoker, arity);
        }
        ReturnWriter writer = returnWriter(method.getReturnType());
        switch (arity) {
            case 0: { Invoker0 f = (Invoker0) invoker; return ctx -> writer.write(ctx, f.invoke()); }
            case 1: { Invoker1 f = (Invoker1) invoker; return ctx -> writer.write(ctx, f.invoke(ctx.getArgBoxed(0))); }
            case 2: { Invoker2 f = (Invoker2) invoker; return ctx -> writer.write(ctx, f.invoke(ctx.getArgBoxed(0), ctx.getArgBoxed(1))); }
            case 3: { Invoker3 f = (Invoker3) invoker; return ctx -> writer.write(ctx, f.invoke(ctx.getArgBoxed(0), ctx.getArgBoxed(1), ctx.getArgBoxed(2))); }
            case 4: { Invoker4 f = (Invoker4) invoker; return ctx -> writer.write(ctx, f.invoke(ctx.getArgBoxed(0), ctx.getArgBoxed(1), ctx.getArgBoxed(2), ctx.getArgBoxed(3))); }
            case 5: { Invoker5 f = (Invoker5) invoker; return ctx -> writer.write(ctx, f.invoke(ctx.getArgBoxed(0), ctx.getArgBoxed(1), ctx.getArgBoxed(2), ctx.getArgBoxed(3), ctx.getArgBoxed(4))); }
            default: throw new IllegalStateException("不支持的参数数量: " + arity);
        }
    }

    private static NativeFunction.NativeCallable<?> buildVoidSystemCallable(Object invoker, int arity) {
        switch (arity) {
            case 0: { VoidInvoker0 f = (VoidInvoker0) invoker; return ctx -> f.invoke(); }
            case 1: { VoidInvoker1 f = (VoidInvoker1) invoker; return ctx -> f.invoke(ctx.getArgBoxed(0)); }
            case 2: { VoidInvoker2 f = (VoidInvoker2) invoker; return ctx -> f.invoke(ctx.getArgBoxed(0), ctx.getArgBoxed(1)); }
            case 3: { VoidInvoker3 f = (VoidInvoker3) invoker; return ctx -> f.invoke(ctx.getArgBoxed(0), ctx.getArgBoxed(1), ctx.getArgBoxed(2)); }
            case 4: { VoidInvoker4 f = (VoidInvoker4) invoker; return ctx -> f.invoke(ctx.getArgBoxed(0), ctx.getArgBoxed(1), ctx.getArgBoxed(2), ctx.getArgBoxed(3)); }
            case 5: { VoidInvoker5 f = (VoidInvoker5) invoker; return ctx -> f.invoke(ctx.getArgBoxed(0), ctx.getArgBoxed(1), ctx.getArgBoxed(2), ctx.getArgBoxed(3), ctx.getArgBoxed(4)); }
            default: throw new IllegalStateException("不支持的参数数量: " + arity);
        }
    }

    /**
     * 生成扩展函数的 NativeCallable 桥接（解释器使用）
     * 第一个参数从 context.getTarget() 取，其余从 context.getArgBoxed(index) 取
     */
    private static NativeFunction.NativeCallable<?> buildExtensionCallable(Method method) {
        Object invoker = createInvoker(method);
        if (invoker == null) return buildMethodHandleFallback(method, 1);
        int totalArity = method.getParameterCount();
        if (method.getReturnType() == void.class) {
            return buildVoidExtensionCallable(invoker, totalArity);
        }
        ReturnWriter writer = returnWriter(method.getReturnType());
        switch (totalArity) {
            case 1: { Invoker1 f = (Invoker1) invoker; return ctx -> writer.write(ctx, f.invoke(ctx.getTarget())); }
            case 2: { Invoker2 f = (Invoker2) invoker; return ctx -> writer.write(ctx, f.invoke(ctx.getTarget(), ctx.getArgBoxed(0))); }
            case 3: { Invoker3 f = (Invoker3) invoker; return ctx -> writer.write(ctx, f.invoke(ctx.getTarget(), ctx.getArgBoxed(0), ctx.getArgBoxed(1))); }
            case 4: { Invoker4 f = (Invoker4) invoker; return ctx -> writer.write(ctx, f.invoke(ctx.getTarget(), ctx.getArgBoxed(0), ctx.getArgBoxed(1), ctx.getArgBoxed(2))); }
            case 5: { Invoker5 f = (Invoker5) invoker; return ctx -> writer.write(ctx, f.invoke(ctx.getTarget(), ctx.getArgBoxed(0), ctx.getArgBoxed(1), ctx.getArgBoxed(2), ctx.getArgBoxed(3))); }
            default: throw new IllegalStateException("不支持的扩展函数参数数量: " + totalArity);
        }
    }

    private static NativeFunction.NativeCallable<?> buildVoidExtensionCallable(Object invoker, int totalArity) {
        switch (totalArity) {
            case 1: { VoidInvoker1 f = (VoidInvoker1) invoker; return ctx -> f.invoke(ctx.getTarget()); }
            case 2: { VoidInvoker2 f = (VoidInvoker2) invoker; return ctx -> f.invoke(ctx.getTarget(), ctx.getArgBoxed(0)); }
            case 3: { VoidInvoker3 f = (VoidInvoker3) invoker; return ctx -> f.invoke(ctx.getTarget(), ctx.getArgBoxed(0), ctx.getArgBoxed(1)); }
            case 4: { VoidInvoker4 f = (VoidInvoker4) invoker; return ctx -> f.invoke(ctx.getTarget(), ctx.getArgBoxed(0), ctx.getArgBoxed(1), ctx.getArgBoxed(2)); }
            case 5: { VoidInvoker5 f = (VoidInvoker5) invoker; return ctx -> f.invoke(ctx.getTarget(), ctx.getArgBoxed(0), ctx.getArgBoxed(1), ctx.getArgBoxed(2), ctx.getArgBoxed(3)); }
            default: throw new IllegalStateException("不支持的扩展函数参数数量: " + totalArity);
        }
    }

    // endregion

    private static ReturnWriter returnWriter(Class<?> type) {
        if (type == void.class) return (ctx, v) -> {};
        if (type == int.class) return (ctx, v) -> ctx.setReturnInt((Integer) v);
        if (type == long.class) return (ctx, v) -> ctx.setReturnLong((Long) v);
        if (type == double.class) return (ctx, v) -> ctx.setReturnDouble((Double) v);
        if (type == float.class) return (ctx, v) -> ctx.setReturnFloat((Float) v);
        if (type == boolean.class) return (ctx, v) -> ctx.setReturnBool((Boolean) v);
        return FunctionContext::setReturnRef;
    }

    @FunctionalInterface
    private interface ReturnWriter {
        void write(FunctionContext<?> ctx, Object value);
    }
}
