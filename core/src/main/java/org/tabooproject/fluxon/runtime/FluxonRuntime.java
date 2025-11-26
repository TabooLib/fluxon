package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.runtime.function.*;
import org.tabooproject.fluxon.runtime.function.extension.*;
import org.tabooproject.fluxon.runtime.function.extension.reflect.ExtensionClass;
import org.tabooproject.fluxon.runtime.function.extension.reflect.ExtensionConstructor;
import org.tabooproject.fluxon.runtime.function.extension.reflect.ExtensionField;
import org.tabooproject.fluxon.runtime.function.extension.reflect.ExtensionMethod;
import org.tabooproject.fluxon.runtime.java.ExportRegistry;
import org.tabooproject.fluxon.runtime.library.LibraryLoader;
import org.tabooproject.fluxon.runtime.library.LibraryLoader.LibraryLoadResult;
import org.tabooproject.fluxon.util.KV;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executor;
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

    // 缓存的系统函数数组（避免每次创建环境时都转换）
    private volatile Function[] cachedSystemFunctions;
    // 缓存的系统扩展函数数组（避免每次创建环境时都转换）
    private volatile KV<Class<?>, Function>[][] cachedSystemExtensionFunctions;
    // 脏标记：当注册新函数时标记为 true，下次创建环境时会重新构建缓存
    private volatile boolean dirty = false;

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
        // 全局对象
        // 用于应对在上下文环境中使用同名的全局函数
        registerFunction("g", 0, (context) -> GlobalObject.INSTANCE);
        // reflect
        ExtensionClass.init(this);
        ExtensionConstructor.init(this);
        ExtensionField.init(this);
        ExtensionMethod.init(this);
        // Extension
        ExtensionCollection.init(this);
        ExtensionFile.init(this);
        ExtensionIterable.init(this);
        ExtensionList.init(this);
        ExtensionMap.init(this);
        ExtensionMapEntry.init(this);
        ExtensionObject.init(this);
        ExtensionPath.init(this);
        ExtensionString.init(this);
        ExtensionThrowable.init(this);
        // Function
        FunctionCrypto.init(this);
        FunctionEnvironment.init(this);
        FunctionFile.init(this);
        FunctionMath.init(this);
        FunctionSystem.init(this);
        FunctionTime.init(this);
        FunctionType.init(this);
    }

    /**
     * 构建缓存的函数和扩展函数数组
     * 这样每次创建环境时就不需要重新转换了
     */
    @SuppressWarnings("unchecked")
    private void bake() {
        // 构建系统函数数组
        cachedSystemFunctions = systemFunctions.values().toArray(new Function[0]);
        // 构建系统扩展函数数组
        List<KV<Class<?>, Function>[]> systemExtensionFunctionsList = new ArrayList<>();
        for (Map.Entry<String, Map<Class<?>, Function>> entry : extensionFunctions.entrySet()) {
            List<KV<Class<?>, Function>> classFunctionMap = new ArrayList<>();
            for (Map.Entry<Class<?>, Function> entry2 : entry.getValue().entrySet()) {
                classFunctionMap.add(new KV<>(entry2.getKey(), entry2.getValue()));
            }
            systemExtensionFunctionsList.add(classFunctionMap.toArray(new KV[0]));
        }
        cachedSystemExtensionFunctions = systemExtensionFunctionsList.toArray(new KV[0][]);
        // 重置脏标记
        dirty = false;
    }

    /**
     * 初始化解释器环境
     */
    public Environment newEnvironment() {
        // 如果缓存被标记为脏（有新函数注册），则重新构建缓存
        if (dirty) {
            synchronized (this) {
                if (dirty) {
                    bake();
                }
            }
        }
        return new Environment(systemFunctions, cachedSystemFunctions, systemVariables, extensionFunctions, cachedSystemExtensionFunctions);
    }

    /**
     * 池化版本的环境借用，适合在高频创建/销毁环境的场景下减少分配。
     * 调用方需使用 try-with-resources 或显式 close 归还。
     */
    public EnvironmentPool.Lease borrowEnvironment() {
        if (dirty) {
            synchronized (this) {
                if (dirty) {
                    bake();
                }
            }
        }
        return EnvironmentPool.borrow(systemFunctions, cachedSystemFunctions, systemVariables, extensionFunctions, cachedSystemExtensionFunctions);
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
     * 注册已有的函数实例
     *
     * @param function 函数实例
     */
    public void registerFunction(@NotNull Function function) {
        systemFunctions.put(function.getName(), function);
        dirty = true;
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
        dirty = true;
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
        dirty = true;
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
        dirty = true;
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
        dirty = true;
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
        dirty = true;
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
        dirty = true;
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
        dirty = true;
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
        dirty = true;
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
        dirty = true;
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
        dirty = true;
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
        dirty = true;
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
        dirty = true;
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
        dirty = true;
    }

    /**
     * 注册扩展函数，直接使用已有 Function 实例
     */
    public <Target> void registerExtensionFunction(Class<Target> extensionClass, Function function) {
        extensionFunctions.computeIfAbsent(function.getName(), k -> new HashMap<>()).put(extensionClass, function);
        dirty = true;
    }

    /**
     * 注册扩展函数
     */
    public <Target> void registerExtensionFunction(Class<Target> extensionClass, String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(new SymbolFunction(null, name, paramCounts), implementation));
        dirty = true;
    }

    /**
     * 注册扩展函数
     */
    public <Target> void registerExtensionFunction(Class<Target> extensionClass, String namespace, String name, int paramCount, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCount), implementation));
        dirty = true;
    }

    /**
     * 注册扩展函数
     */
    public <Target> void registerExtensionFunction(Class<Target> extensionClass, String namespace, String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCounts), implementation));
        dirty = true;
    }

    /**
     * 注册异步扩展函数
     */
    public <Target> void registerAsyncExtensionFunction(Class<Target> extensionClass, String namespace, String name, int paramCount, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCount), implementation, true, false));
        dirty = true;
    }

    /**
     * 注册异步扩展函数
     */
    public <Target> void registerAsyncExtensionFunction(Class<Target> extensionClass, String namespace, String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCounts), implementation, true, false));
        dirty = true;
    }

    /**
     * 注册主线程同步扩展函数
     */
    public <Target> void registerSyncExtensionFunction(Class<Target> extensionClass, String namespace, String name, int paramCount, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCount), implementation, false, true));
        dirty = true;
    }

    /**
     * 注册主线程同步扩展函数
     */
    public <Target> void registerSyncExtensionFunction(Class<Target> extensionClass, String namespace, String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(namespace, new SymbolFunction(namespace, name, paramCounts), implementation, false, true));
        dirty = true;
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

    /**
     * 编译并加载库文件，将其中的 @api 函数注册到运行时
     *
     * @param path 库文件路径
     * @return 加载结果
     */
    public LibraryLoadResult loadLibrary(Path path) {
        return new LibraryLoader(this).load(path);
    }
}
