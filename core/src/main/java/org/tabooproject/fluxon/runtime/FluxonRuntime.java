package org.tabooproject.fluxon.runtime;

import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.runtime.function.*;
import org.tabooproject.fluxon.runtime.function.extension.*;
import org.tabooproject.fluxon.runtime.function.extension.reflect.ExtensionClass;
import org.tabooproject.fluxon.runtime.function.extension.reflect.ExtensionConstructor;
import org.tabooproject.fluxon.runtime.function.extension.reflect.ExtensionField;
import org.tabooproject.fluxon.runtime.function.extension.reflect.ExtensionMethod;
import org.tabooproject.fluxon.runtime.java.ExportRegistry;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 原生函数和符号注册中心
 * 用于统一管理解析阶段和执行阶段的内置函数和符号
 */
public class FluxonRuntime {

    // 单例实例
    private static final FluxonRuntime INSTANCE = new FluxonRuntime();

    // 系统函数
    private final Map<String, Function> systemFunctions = new LinkedHashMap<>();
    // 系统变量
    private final Map<String, Object> systemVariables = new HashMap<>();
    // 扩展函数
    private final Map<String, Map<Class<?>, Function>> extensionFunctions = new LinkedHashMap<>();

    // Export 注册中心
    private final ExportRegistry exportRegistry = new ExportRegistry(this);

    // 主线程执行器
    private Executor primaryThreadExecutor = Executors.newSingleThreadExecutor();

    /**
     * 获取单例实例
     */
    public static FluxonRuntime getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化系统函数
     */
    private FluxonRuntime() {
        // reflect
        ExtensionClass.init(this);
        ExtensionConstructor.init(this);
        ExtensionField.init(this);
        ExtensionMethod.init(this);
        // Extension
        ExtensionCollection.init(this);
        ExtensionIterable.init(this);
        ExtensionList.init(this);
        ExtensionMap.init(this);
        ExtensionMapEntry.init(this);
        ExtensionObject.init(this);
        ExtensionString.init(this);
        ExtensionThrowable.init(this);
        // Function
        FunctionCrypto.init(this);
        FunctionEnvironment.init(this);
        FunctionMath.init(this);
        FunctionSystem.init(this);
        FunctionTime.init(this);
        FunctionType.init(this);
    }

    /**
     * 初始化解释器环境
     */
    public Environment newEnvironment() {
        return new Environment(systemFunctions, systemVariables, extensionFunctions);
    }

    /**
     * 注册变量
     *
     * @param name  变量名
     * @param value 变量值
     */
    public void registerVariable(String name, Object value) {
        systemVariables.put(name, value);
    }

    /**
     * 注册函数
     *
     * @param name           函数名
     * @param paramCount     参数数量
     * @param implementation 函数实现
     */
    public void registerFunction(String name, int paramCount, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(new SymbolFunction(null, name, paramCount), implementation));
    }

    /**
     * 注册函数
     *
     * @param name           函数名
     * @param paramCounts    可能的参数数量列表
     * @param implementation 函数实现
     */
    public void registerFunction(String name, List<Integer> paramCounts, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(new SymbolFunction(null, name, paramCounts), implementation));
    }

    /**
     * 注册函数
     *
     * @param namespace      命名空间
     * @param name           函数名
     * @param paramCount     参数数量
     * @param implementation 函数实现
     */
    public void registerFunction(String namespace, String name, int paramCount, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCount), implementation));
    }

    /**
     * 注册函数
     *
     * @param namespace      命名空间
     * @param name           函数名
     * @param paramCounts    可能的参数数量列表
     * @param implementation 函数实现
     */
    public void registerFunction(String namespace, String name, List<Integer> paramCounts, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCounts), implementation));
    }

    /**
     * 注册异步函数
     *
     * @param name           函数名
     * @param paramCount     参数数量
     * @param implementation 函数实现
     */
    public void registerAsyncFunction(String name, int paramCount, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(new SymbolFunction(null, name, paramCount), implementation, true, false));
    }

    /**
     * 注册异步函数
     *
     * @param name           函数名
     * @param paramCounts    可能的参数数量列表
     * @param implementation 函数实现
     */
    public void registerAsyncFunction(String name, List<Integer> paramCounts, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(new SymbolFunction(null, name, paramCounts), implementation, true, false));
    }

    /**
     * 注册异步函数
     *
     * @param namespace      命名空间
     * @param name           函数名
     * @param paramCounts    可能的参数数量列表
     * @param implementation 函数实现
     */
    public void registerAsyncFunction(String namespace, String name, List<Integer> paramCounts, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCounts), implementation, true, false));
    }

    /**
     * 注册异步函数
     *
     * @param namespace      命名空间
     * @param name           函数名
     * @param paramCount     参数数量
     * @param implementation 函数实现
     */
    public void registerAsyncFunction(String namespace, String name, int paramCount, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCount), implementation, true, false));
    }

    /**
     * 注册主线程同步函数
     *
     * @param name           函数名
     * @param paramCount     参数数量
     * @param implementation 函数实现
     */
    public void registerPrimarySyncFunction(String name, int paramCount, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(new SymbolFunction(null, name, paramCount), implementation, false, true));
    }

    /**
     * 注册主线程同步函数
     *
     * @param name           函数名
     * @param paramCounts    可能的参数数量列表
     * @param implementation 函数实现
     */
    public void registerPrimarySyncFunction(String name, List<Integer> paramCounts, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(new SymbolFunction(null, name, paramCounts), implementation, false, true));
    }

    /**
     * 注册主线程同步函数
     *
     * @param namespace      命名空间
     * @param name           函数名
     * @param paramCounts    可能的参数数量列表
     * @param implementation 函数实现
     */
    public void registerPrimarySyncFunction(String namespace, String name, List<Integer> paramCounts, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCounts), implementation, false, true));
    }

    /**
     * 注册主线程同步函数
     *
     * @param namespace      命名空间
     * @param name           函数名
     * @param paramCount     参数数量
     * @param implementation 函数实现
     */
    public void registerPrimarySyncFunction(String namespace, String name, int paramCount, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCount), implementation, false, true));
    }

    /**
     * 注册扩展函数
     */
    public <Target> ExtensionBuilder<Target> registerExtension(Class<Target> extensionClass) {
        return new ExtensionBuilder<>(this, extensionClass, null);
    }

    /**
     * 注册扩展函数
     */
    public <Target> ExtensionBuilder<Target> registerExtension(Class<Target> extensionClass, String namespace) {
        return new ExtensionBuilder<>(this, extensionClass, namespace);
    }

    /**
     * 注册扩展函数
     */
    public <Target> void registerExtensionFunction(Class<Target> extensionClass, String name, int paramCount, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(new SymbolFunction(null, name, paramCount), implementation));
    }

    /**
     * 注册扩展函数
     */
    public <Target> void registerExtensionFunction(Class<Target> extensionClass, String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(new SymbolFunction(null, name, paramCounts), implementation));
    }

    /**
     * 注册扩展函数
     */
    public <Target> void registerExtensionFunction(Class<Target> extensionClass, String namespace, String name, int paramCount, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCount), implementation));
    }

    /**
     * 注册扩展函数
     */
    public <Target> void registerExtensionFunction(Class<Target> extensionClass, String namespace, String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCounts), implementation));
    }

    /**
     * 注册异步扩展函数
     */
    public <Target> void registerAsyncExtensionFunction(Class<Target> extensionClass, String namespace, String name, int paramCount, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCount), implementation, true, false));
    }

    /**
     * 注册异步扩展函数
     */
    public <Target> void registerAsyncExtensionFunction(Class<Target> extensionClass, String namespace, String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCounts), implementation, true, false));
    }

    /**
     * 注册主线程同步扩展函数
     */
    public <Target> void registerSyncExtensionFunction(Class<Target> extensionClass, String namespace, String name, int paramCount, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCount), implementation, false, true));
    }

    /**
     * 注册主线程同步扩展函数
     */
    public <Target> void registerSyncExtensionFunction(Class<Target> extensionClass, String namespace, String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCounts), implementation, false, true));
    }

    /**
     * 获取所有函数信息
     */
    public Map<String, Function> getSystemFunctions() {
        return systemFunctions;
    }

    /**
     * 获取所有变量信息
     */
    public Map<String, Object> getSystemVariables() {
        return systemVariables;
    }

    /**
     * 获取所有扩展函数信息
     */
    public Map<String, Map<Class<?>, Function>> getExtensionFunctions() {
        return extensionFunctions;
    }

    /**
     * 获取 Export 注册中心
     */
    public ExportRegistry getExportRegistry() {
        return exportRegistry;
    }

    /**
     * 获取主线程执行器
     */
    public Executor getPrimaryThreadExecutor() {
        return primaryThreadExecutor;
    }

    /**
     * 设置主线程执行器
     */
    public void setPrimaryThreadExecutor(Executor primaryThreadExecutor) {
        this.primaryThreadExecutor = primaryThreadExecutor;
    }
}