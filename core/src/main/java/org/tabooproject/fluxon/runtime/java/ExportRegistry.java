package org.tabooproject.fluxon.runtime.java;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.NativeFunction;
import org.tabooproject.fluxon.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Export 注册中心
 * 负责扫描和注册带有 @Export 注解的方法到 Fluxon 运行时
 */
@SuppressWarnings("UnusedReturnValue")
public class ExportRegistry {

    // Fluxon 运行时实例
    private final FluxonRuntime runtime;

    /**
     * 构造函数
     *
     * @param runtime Fluxon 运行时实例
     */
    public ExportRegistry(FluxonRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * 注册一个类的所有导出方法
     *
     * @param clazz 要注册的类
     * @return ClassBridge
     */
    public <T> ClassBridge registerClass(Class<T> clazz) {
        return registerClass(clazz, null);
    }

    /**
     * 注册一个类的所有导出方法
     *
     * @param namespace 命名空间
     * @param clazz     要注册的类
     * @return ClassBridge
     */
    public <T> ClassBridge registerClass(Class<T> clazz, String namespace) {
        // 收集该类的所有 @Export 方法并封装
        Method[] methods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Export.class))
                .toArray(Method[]::new);
        if (methods.length == 0) {
            throw new IllegalStateException("类 " + clazz.getName() + " 没有 @Export 方法");
        }
        
        // 转换为 ExportMethod 数组
        ExportMethod[] exportMethods = convertToExportMethods(methods);
        return registerClassMethods(clazz, namespace, exportMethods);
    }

    /**
     * 注册一个类的特定导出方法
     *
     * @param namespace     命名空间
     * @param clazz         要注册的类
     * @param exportMethods 要注册的方法（封装后的对象数组）
     * @return ClassBridge
     */
    public <T> ClassBridge registerClassMethods(Class<T> clazz, String namespace, ExportMethod[] exportMethods) {
        // 提取原始方法数组用于字节码生成
        Method[] methods = Arrays.stream(exportMethods)
                .map(ExportMethod::getMethod)
                .toArray(Method[]::new);
        
        // 为整个类生成或获取桥接器
        ClassBridge bridge = ExportBytecodeGenerator.generateClassBridge(clazz, methods, clazz.getClassLoader());
        // 注册该类的所有导出方法
        registerClassMethods(clazz, namespace, exportMethods, bridge);
        return bridge;
    }
    
    /**
     * 兼容旧接口的重载方法
     * 
     * @param namespace     命名空间
     * @param clazz         要注册的类
     * @param methods       要注册的方法（原始方法数组）
     * @return ClassBridge
     */
    public <T> ClassBridge registerClassMethods(Class<T> clazz, String namespace, Method[] methods) {
        ExportMethod[] exportMethods = convertToExportMethods(methods);
        return registerClassMethods(clazz, namespace, exportMethods);
    }

    /**
     * 注册类的所有方法到运行时（兼容旧接口）
     */
    private <T> void registerClassMethods(Class<T> clazz, String namespace, Method[] methods, ClassBridge bridge) {
        ExportMethod[] exportMethods = convertToExportMethods(methods);
        registerClassMethods(clazz, namespace, exportMethods, bridge);
    }
    
    /**
     * 注册类的所有方法到运行时
     */
    private <T> void registerClassMethods(Class<T> clazz, String namespace, ExportMethod[] exportMethods, ClassBridge bridge) {
        for (ExportMethod exportMethod : exportMethods) {
            Method method = exportMethod.getMethod();
            String methodName = exportMethod.getTransformedName();
            NativeFunction.NativeCallable<T> callable = context -> {
                Object[] args = context.getArguments();
                Object target = context.getTarget();
                return bridge.invoke(methodName, target, args);
            };
            
            // 分析方法参数，获取支持的参数数量列表
            List<Integer> supportedCounts = analyzeMethodParameterCounts(method);
            
            // 注册扩展函数，传入支持的参数数量列表
            // 根据 exportMethod.isAsync() 和 exportMethod.isSync() 来处理不同类型的函数
            if (exportMethod.isAsync()) {
                runtime.registerAsyncExtensionFunction(clazz, namespace, methodName, supportedCounts, callable);
            } else if (exportMethod.isSync()) {
                runtime.registerSyncExtensionFunction(clazz, namespace, methodName, supportedCounts, callable);
            } else {
                runtime.registerExtensionFunction(clazz, namespace, methodName, supportedCounts, callable);
            }
        }
    }

    /**
     * 分析方法的参数，获取支持的参数数量列表
     *
     * @param method 要分析的方法
     * @return 支持的参数数量列表
     */
    private List<Integer> analyzeMethodParameterCounts(Method method) {
        Parameter[] parameters = method.getParameters();
        int requiredCount = 0;
        int totalCount = parameters.length;

        // 计算必需参数数量（从后往前找第一个非可选参数）
        for (int i = 0; i < parameters.length; i++) {
            if (!parameters[i].isAnnotationPresent(Optional.class)) {
                requiredCount = i + 1;
            }
        }

        // 生成支持的参数数量列表
        List<Integer> counts = new ArrayList<>();
        for (int i = requiredCount; i <= totalCount; i++) {
            counts.add(i);
        }
        return counts;
    }
    
    /**
     * 将 Method 数组转换为 ExportMethod 数组
     * 
     * @param methods 原始方法数组
     * @return ExportMethod 数组
     */
    private ExportMethod[] convertToExportMethods(Method[] methods) {
        Set<String> originalNames = Arrays.stream(methods)
                .map(Method::getName)
                .collect(Collectors.toSet());
        
        ExportMethod[] exportMethods = new ExportMethod[methods.length];
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String transformedName = StringUtils.transformMethodName(method.getName());
            
            // 如果转换后的名称与其他原始方法名冲突，则使用原始名称
            if (originalNames.contains(transformedName) && !transformedName.equals(method.getName())) {
                transformedName = method.getName();
            }
            
            exportMethods[i] = new ExportMethod(method, transformedName);
        }
        return exportMethods;
    }
}