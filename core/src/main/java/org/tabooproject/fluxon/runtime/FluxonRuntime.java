package org.tabooproject.fluxon.runtime;

import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.runtime.function.*;
import org.tabooproject.fluxon.runtime.function.extension.ExtensionClass;
import org.tabooproject.fluxon.runtime.function.extension.ExtensionConstructor;
import org.tabooproject.fluxon.runtime.function.extension.ExtensionField;
import org.tabooproject.fluxon.runtime.function.extension.ExtensionCollection;
import org.tabooproject.fluxon.runtime.function.extension.ExtensionMap;
import org.tabooproject.fluxon.runtime.function.extension.ExtensionMethod;
import org.tabooproject.fluxon.runtime.function.extension.ExtensionObject;
import org.tabooproject.fluxon.runtime.function.extension.ExtensionString;
import org.tabooproject.fluxon.runtime.java.ExportRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 原生函数和符号注册中心
 * 用于统一管理解析阶段和执行阶段的内置函数和符号
 */
public class FluxonRuntime {

    // 单例实例
    private static final FluxonRuntime INSTANCE = new FluxonRuntime();

    // 系统函数
    private final Map<String, Function> systemFunctions = new HashMap<>();
    // 系统变量
    private final Map<String, Object> systemVariables = new HashMap<>();
    // 扩展函数
    private final Map<String, Map<Class<?>, Function>> extensionFunctions = new HashMap<>();

    // Export 注册中心
    private final ExportRegistry exportRegistry = new ExportRegistry(this);

    /**
     * 获取单例实例
     */
    public static FluxonRuntime getInstance() {
        return INSTANCE;
    }

    /**
     * 获取 Export 注册中心
     */
    public ExportRegistry getExportRegistry() {
        return exportRegistry;
    }

    /**
     * 私有构造函数，初始化系统函数
     */
    private FluxonRuntime() {
        FunctionMath.init(this);
        FunctionSystem.init(this);
        FunctionTime.init(this);
        FunctionType.init(this);
        FunctionEncode.init(this);
        FunctionEnvironment.init(this);
        ExtensionClass.init(this);
        ExtensionCollection.init(this);
        ExtensionConstructor.init(this);
        ExtensionField.init(this);
        ExtensionCollection.init(this);
        ExtensionMap.init(this);
        ExtensionMethod.init(this);
        ExtensionObject.init(this);
        ExtensionString.init(this);
    }

    /**
     * 注册函数
     *
     * @param name           函数名
     * @param paramCount     参数数量
     * @param implementation 函数实现
     */
    public void registerFunction(String name, int paramCount, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(new SymbolFunction(name, paramCount), implementation));
    }

    /**
     * 注册函数
     *
     * @param name           函数名
     * @param paramCounts    可能的参数数量列表
     * @param implementation 函数实现
     */
    public void registerFunction(String name, List<Integer> paramCounts, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(new SymbolFunction(name, paramCounts), implementation));
    }

    /**
     * 注册异步函数
     *
     * @param name           函数名
     * @param paramCount     参数数量
     * @param implementation 函数实现
     */
    public void registerAsyncFunction(String name, int paramCount, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(new SymbolFunction(name, paramCount), implementation, true));
    }

    /**
     * 注册异步函数
     *
     * @param name           函数名
     * @param paramCounts    可能的参数数量列表
     * @param implementation 函数实现
     */
    public void registerAsyncFunction(String name, List<Integer> paramCounts, NativeFunction.NativeCallable<?> implementation) {
        systemFunctions.put(name, new NativeFunction<>(new SymbolFunction(name, paramCounts), implementation, true));
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
     * 注册扩展函数
     */
    public <Target> void registerExtensionFunction(Class<Target> extensionClass, String name, int paramCount, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(new SymbolFunction(name, paramCount), implementation));
    }

    /**
     * 注册扩展函数
     */
    public <Target> void registerExtensionFunction(Class<Target> extensionClass, String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        extensionFunctions.computeIfAbsent(name, k -> new HashMap<>()).put(extensionClass, new NativeFunction<>(new SymbolFunction(name, paramCounts), implementation));
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
     * 初始化解释器环境
     */
    public Environment newEnvironment() {
        return new Environment(systemFunctions, systemVariables, extensionFunctions);
    }
}