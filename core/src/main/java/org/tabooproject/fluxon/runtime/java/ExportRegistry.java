package org.tabooproject.fluxon.runtime.java;

import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.interpreter.bytecode.emitter.BridgeClassEmitter;
import org.tabooproject.fluxon.interpreter.bytecode.emitter.EmitResult;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.FunctionSignature;
import org.tabooproject.fluxon.runtime.NativeFunction;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.sharing.SharedFunctionRegistry;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;
import org.tabooproject.fluxon.util.StringUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Export 注册中心
 * 负责扫描和注册带有 @Export 注解的方法到 Fluxon 运行时
 */
@SuppressWarnings("UnusedReturnValue")
public class ExportRegistry {

    private static final ConcurrentHashMap<Class<?>, ClassBridge> generatedBridges = new ConcurrentHashMap<>();
    private static final FluxonClassLoader fluxonClassLoader = new FluxonClassLoader(ExportRegistry.class.getClassLoader());
    private final FluxonRuntime runtime;

    public ExportRegistry(FluxonRuntime runtime) {
        this.runtime = runtime;
    }

    public <T> ClassBridge registerClass(Class<T> clazz) {
        return registerClass(clazz, null);
    }

    public <T> ClassBridge registerClass(Class<T> clazz, String namespace) {
        Method[] methods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Export.class))
                .toArray(Method[]::new);
        if (methods.length == 0) {
            throw new IllegalStateException("类 " + clazz.getName() + " 没有 @Export 方法");
        }
        ExportMethod[] exportMethods = convertToExportMethods(methods);
        return registerClassMethods(clazz, namespace, exportMethods);
    }

    public <T> ClassBridge registerClassMethods(Class<T> clazz, String namespace, ExportMethod[] exportMethods) {
        Method[] methods = Arrays.stream(exportMethods).map(ExportMethod::getMethod).toArray(Method[]::new);
        ClassBridge bridge = generateClassBridge(clazz, methods);
        registerClassMethods(clazz, namespace, exportMethods, bridge);
        return bridge;
    }

    public <T> ClassBridge registerClassMethods(Class<T> clazz, String namespace, Method[] methods) {
        ExportMethod[] exportMethods = convertToExportMethods(methods);
        return registerClassMethods(clazz, namespace, exportMethods);
    }

    private <T> void registerClassMethods(Class<T> clazz, String namespace, Method[] methods, ClassBridge bridge) {
        ExportMethod[] exportMethods = convertToExportMethods(methods);
        registerClassMethods(clazz, namespace, exportMethods, bridge);
    }

    private <T> void registerClassMethods(Class<T> clazz, String namespace, ExportMethod[] exportMethods, ClassBridge bridge) {
        Map<String, Long> overloadCounts = Arrays.stream(exportMethods).collect(Collectors.groupingBy(ExportMethod::getTransformedName, Collectors.counting()));
        for (int methodIndex = 0; methodIndex < exportMethods.length; methodIndex++) {
            ExportMethod exportMethod = exportMethods[methodIndex];
            Method method = exportMethod.getMethod();
            String methodName = exportMethod.getTransformedName();
            Class<?> returnClass = method.getReturnType();
            int bridgeMethodIndex = methodIndex;
            boolean overloaded = overloadCounts.getOrDefault(methodName, 0L) > 1L;
            NativeFunction.NativeCallable<T> callable = overloaded
                    ? context -> callOverloadedExport(context, bridge, methodName, returnClass)
                    : context -> {
                        // OverloadSet 已经根据签名选中具体方法，非重载热路径直接使用注册索引进入桥接类。
                        bridge.call(bridgeMethodIndex, context);
                    };
            Class<?>[] parameterTypes = method.getParameterTypes();
            Type[] paramTypes = new Type[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                paramTypes[i] = Type.fromClass(parameterTypes[i]);
            }
            FunctionSignature signature = FunctionSignature.returns(Type.fromClass(method.getReturnType())).paramsWithMin(requiredParameterCount(method), paramTypes);
            boolean isAsync = exportMethod.isAsync();
            boolean isSync = exportMethod.isSync();
            runtime.registerExtensionFunction(clazz, namespace, methodName, signature, callable, isAsync, isSync);
            // 自动导出 shared=true 的方法到全局共享注册表
            if (exportMethod.isShared() && runtime.getSharingIdentity() != null) {
                try {
                    MethodHandle mh = MethodHandles.publicLookup().unreflect(method);
                    SharedFunctionRegistry.registerExtension(runtime.getSharingIdentity(), methodName, mh, clazz);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to export shared method: " + methodName, e);
                }
            }
        }
    }

    private void callOverloadedExport(FunctionContext<?> context, ClassBridge bridge, String methodName, Class<?> returnClass) {
        int argCount = context.getArgumentCount();
        Object[] args = new Object[argCount];
        for (int i = 0; i < argCount; i++) args[i] = context.getArgBoxed(i);
        Object target = context.getTarget();
        Intrinsics.checkArgumentTypes(context, bridge.getParameterTypes(methodName, target, args), args);
        Object result = bridge.invoke(methodName, target, args);
        // 重载路径保留 ClassBridge 的运行时特异性分发，同时保持 primitive 返回值写回协议。
        if (returnClass == long.class) {
            context.setReturnLong((Long) result);
        } else if (returnClass == int.class) {
            context.setReturnInt((Integer) result);
        } else if (returnClass == double.class) {
            context.setReturnDouble((Double) result);
        } else if (returnClass == float.class) {
            context.setReturnFloat((Float) result);
        } else if (returnClass == boolean.class) {
            context.setReturnBool((Boolean) result);
        } else {
            context.setReturnRef(result);
        }
    }

    private int requiredParameterCount(Method method) {
        Parameter[] parameters = method.getParameters();
        int required = parameters.length;
        for (int i = parameters.length - 1; i >= 0; i--) {
            if (!parameters[i].isAnnotationPresent(Optional.class)) {
                break;
            }
            required--;
        }
        return required;
    }

    private ExportMethod[] convertToExportMethods(Method[] methods) {
        Set<String> originalNames = Arrays.stream(methods).map(Method::getName).collect(Collectors.toSet());
        ExportMethod[] exportMethods = new ExportMethod[methods.length];
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String transformedName = StringUtils.transformMethodName(method.getName());
            if (originalNames.contains(transformedName) && !transformedName.equals(method.getName())) {
                transformedName = method.getName();
            }
            exportMethods[i] = new ExportMethod(method, transformedName);
        }
        return exportMethods;
    }

    @SuppressWarnings("unchecked")
    public static ClassBridge generateClassBridge(Class<?> targetClass, Method[] exportMethods) {
        ClassBridge cached = generatedBridges.get(targetClass);
        if (cached != null) {
            return cached;
        }
        try {
            BridgeClassEmitter emitter = new BridgeClassEmitter(exportMethods, targetClass.getClassLoader());
            EmitResult result = emitter.emit();
            Class<? extends ClassBridge> bridgeClass = (Class<? extends ClassBridge>) fluxonClassLoader.defineClass(emitter.getClassName().replace('/', '.'), result.getBytecode());
            ClassBridge bridge = bridgeClass.getDeclaredConstructor(String[].class).newInstance((Object) emitter.getMethodNames());
            generatedBridges.put(targetClass, bridge);
            return bridge;
        } catch (Exception e) {
            throw new RuntimeException("为类 " + targetClass.getName() + " 生成字节码桥接器失败", e);
        }
    }

    public static ClassBridge getClassBridge(Class<?> targetClass) {
        return generatedBridges.get(targetClass);
    }

    public static void clearCache() {
        generatedBridges.clear();
    }

    public static int getCacheSize() {
        return generatedBridges.size();
    }
}
