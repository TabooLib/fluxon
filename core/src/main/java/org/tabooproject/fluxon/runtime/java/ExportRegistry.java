package org.tabooproject.fluxon.runtime.java;

import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.interpreter.bytecode.emitter.BridgeClassEmitter;
import org.tabooproject.fluxon.interpreter.bytecode.emitter.EmitResult;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.FunctionSignature;
import org.tabooproject.fluxon.runtime.NativeFunction;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;
import org.tabooproject.fluxon.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        for (ExportMethod exportMethod : exportMethods) {
            Method method = exportMethod.getMethod();
            String methodName = exportMethod.getTransformedName();
            NativeFunction.NativeCallable<T> callable = context -> {
                int argCount = context.getArgumentCount();
                Object[] args = new Object[argCount];
                for (int i = 0; i < argCount; i++) args[i] = context.getArgBoxed(i);
                Object target = context.getTarget();
                Intrinsics.checkArgumentTypes(context, bridge.getParameterTypes(methodName, target, args), args);
                context.setReturnRef(bridge.invoke(methodName, target, args));
            };
            List<Integer> supportedCounts = analyzeMethodParameterCounts(method);
            FunctionSignature signature = FunctionSignature.returnsObject().varParams(supportedCounts);
            boolean isAsync = exportMethod.isAsync();
            boolean isSync = exportMethod.isSync();
            runtime.registerExtensionFunction(clazz, namespace, methodName, signature, callable, isAsync, isSync);
        }
    }

    private List<Integer> analyzeMethodParameterCounts(Method method) {
        Parameter[] parameters = method.getParameters();
        int requiredCount = 0;
        int totalCount = parameters.length;
        for (int i = 0; i < parameters.length; i++) {
            if (!parameters[i].isAnnotationPresent(Optional.class)) {
                requiredCount = i + 1;
            }
        }
        List<Integer> counts = new ArrayList<>();
        for (int i = requiredCount; i <= totalCount; i++) {
            counts.add(i);
        }
        return counts;
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
