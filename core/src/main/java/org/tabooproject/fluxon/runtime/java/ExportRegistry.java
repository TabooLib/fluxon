package org.tabooproject.fluxon.runtime.java;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
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
    public ClassBridge registerClass(Class<?> clazz) {
        return registerClass(clazz, null);
    }

    /**
     * 注册一个类的所有导出方法
     *
     * @param namespace 命名空间
     * @param clazz     要注册的类
     * @return ClassBridge
     */
    public ClassBridge registerClass(Class<?> clazz, String namespace) {
        // 收集该类的所有 @Export 方法
        Method[] exportMethods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Export.class))
                .toArray(Method[]::new);
        if (exportMethods.length == 0) {
            throw new IllegalStateException("类 " + clazz.getName() + " 没有 @Export 方法");
        }
        return registerClassMethods(clazz, namespace, exportMethods);
    }

    /**
     * 注册一个类的特定导出方法
     *
     * @param namespace     命名空间
     * @param clazz         要注册的类
     * @param exportMethods 要注册的方法
     * @return ClassBridge
     */
    public ClassBridge registerClassMethods(Class<?> clazz, String namespace, Method[] exportMethods) {
        // 为整个类生成或获取桥接器
        ClassBridge bridge = ExportBytecodeGenerator.generateClassBridge(clazz, exportMethods, clazz.getClassLoader());
        // 注册该类的所有导出方法
        registerClassMethods(clazz, namespace, exportMethods, bridge);
        return bridge;
    }

    /**
     * 注册类的所有方法到运行时
     */
    private void registerClassMethods(Class<?> clazz, String namespace, Method[] exportMethods, ClassBridge bridge) {
        for (Method method : exportMethods) {
            Set<String> originalNames = Arrays.stream(exportMethods).map(Method::getName).collect(Collectors.toSet());
            String methodName = StringUtils.transformMethodName(method.getName());
            if (originalNames.contains(methodName)) {
                methodName = method.getName();
            }
            // 分析方法参数，获取支持的参数数量列表
            List<Integer> supportedCounts = analyzeMethodParameterCounts(method);
            // 注册扩展函数，传入支持的参数数量列表
            String finalMethodName = methodName;
            runtime.registerExtensionFunction(clazz, namespace, methodName, supportedCounts, (context) -> {
                Object[] args = context.getArguments();
                Object target = context.getTarget();
                return bridge.invoke(finalMethodName, target, args);
            });
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
}