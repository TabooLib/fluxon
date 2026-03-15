package org.tabooproject.fluxon.runtime;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Fluxon 函数扫描器
 * 扫描类中 @FluxonFunction 标注的静态方法，自动注册为系统函数或扩展函数
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
            if (annotation == null) continue;
            if (!Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())) {
                throw new IllegalStateException("@FluxonFunction 方法必须为 public static: " + method);
            }
            Class<?> target = annotation.target();
            if (target == FluxonFunction.SystemFunction.class) {
                registerSystemFunction(runtime, clazz, method, annotation);
            } else {
                registerExtensionFunction(runtime, clazz, method, annotation, target);
            }
        }
    }

    /**
     * 注册系统函数
     */
    private static void registerSystemFunction(FluxonRuntime runtime, Class<?> clazz, Method method, FluxonFunction annotation) {
        String name = annotation.value().isEmpty() ? method.getName() : annotation.value();
        String namespace = annotation.namespace().isEmpty() ? null : annotation.namespace();
        FunctionSignature signature = buildSignature(method, 0);
        DirectBinding binding = DirectBinding.ofMethod(clazz, method);
        NativeFunction.NativeCallable<?> callable = buildSystemCallable(method);
        if (namespace != null) {
            runtime.registerFunction(new NativeFunction<>(namespace, name, signature, callable, false, false, binding));
        } else {
            runtime.registerFunction(new NativeFunction<>(name, signature, callable, binding));
        }
    }

    /**
     * 注册扩展函数
     * 方法第一个参数为 target 类型，FunctionSignature 从第二个参数开始构建
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
        // 从第二个参数开始构建签名（排除 target）
        FunctionSignature signature = buildSignature(method, 1);
        DirectBinding binding = DirectBinding.ofMethod(clazz, method);
        NativeFunction.NativeCallable<?> callable = buildExtensionCallable(method);
        @SuppressWarnings("rawtypes")
        NativeFunction nf = new NativeFunction<>(namespace, name, signature, (NativeFunction.NativeCallable) callable, false, false, binding);
        runtime.registerExtensionFunction((Class) target, nf);
    }

    /**
     * 从 Java 方法签名推导 FunctionSignature
     *
     * @param skipParams 跳过前 N 个参数（扩展函数跳过 target 参数）
     */
    private static FunctionSignature buildSignature(Method method, int skipParams) {
        Class<?>[] paramClasses = method.getParameterTypes();
        int userParamCount = paramClasses.length - skipParams;
        Type[] paramTypes = new Type[userParamCount];
        for (int i = 0; i < userParamCount; i++) {
            paramTypes[i] = Type.fromClass(paramClasses[i + skipParams]);
        }
        Type returnType = Type.fromClass(method.getReturnType());
        return FunctionSignature.returns(returnType).params(paramTypes);
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
            returnType = boxClass(returnType);
        }
        Class<?>[] params = type.parameterArray();
        for (int i = 0; i < params.length; i++) {
            if (params[i].isPrimitive()) {
                params[i] = boxClass(params[i]);
            }
        }
        return MethodType.methodType(returnType, params);
    }

    private static Class<?> boxClass(Class<?> primitive) {
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == int.class) return Integer.class;
        if (primitive == long.class) return Long.class;
        if (primitive == double.class) return Double.class;
        if (primitive == float.class) return Float.class;
        if (primitive == byte.class) return Byte.class;
        if (primitive == short.class) return Short.class;
        if (primitive == char.class) return Character.class;
        return primitive;
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
