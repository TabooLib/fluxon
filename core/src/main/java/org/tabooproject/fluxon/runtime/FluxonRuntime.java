package org.tabooproject.fluxon.runtime;

import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.runtime.function.FunctionMath;
import org.tabooproject.fluxon.runtime.function.FunctionSystem;
import org.tabooproject.fluxon.runtime.function.FunctionTime;
import org.tabooproject.fluxon.runtime.function.FunctionType;
import org.tabooproject.fluxon.runtime.function.extension.ExtensionList;
import org.tabooproject.fluxon.runtime.function.extension.ExtensionObject;

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
    private final Map<Class<?>, Map<String, Function>> extensionFunctions = new HashMap<>();

    /**
     * 获取单例实例
     *
     * @return 注册中心实例
     */
    public static FluxonRuntime getInstance() {
        return INSTANCE;
    }

    /**
     * 私有构造函数，初始化系统函数
     */
    private FluxonRuntime() {
        FunctionMath.init(this);
        FunctionSystem.init(this);
        FunctionTime.init(this);
        FunctionType.init(this);
        ExtensionList.init(this);
        ExtensionObject.init(this);
    }

    /**
     * 注册函数
     *
     * @param name 函数名
     * @param paramCount 参数数量
     * @param implementation 函数实现
     */
    public void registerFunction(String name, int paramCount, NativeFunction.NativeCallable implementation) {
        systemFunctions.put(name, new NativeFunction(new SymbolFunction(name, paramCount), implementation));
    }

    /**
     * 注册函数
     *
     * @param name 函数名
     * @param paramCounts 可能的参数数量列表
     * @param implementation 函数实现
     */
    public void registerFunction(String name, List<Integer> paramCounts, NativeFunction.NativeCallable implementation) {
        systemFunctions.put(name, new NativeFunction(new SymbolFunction(name, paramCounts), implementation));
    }

    /**
     * 注册异步函数
     *
     * @param name 函数名
     * @param paramCount 参数数量
     * @param implementation 函数实现
     */
    public void registerAsyncFunction(String name, int paramCount, NativeFunction.NativeCallable implementation) {
        systemFunctions.put(name, new NativeFunction(new SymbolFunction(name, paramCount), implementation, true));
    }

    /**
     * 注册异步函数
     *
     * @param name 函数名
     * @param paramCounts 可能的参数数量列表
     * @param implementation 函数实现
     */
    public void registerAsyncFunction(String name, List<Integer> paramCounts, NativeFunction.NativeCallable implementation) {
        systemFunctions.put(name, new NativeFunction(new SymbolFunction(name, paramCounts), implementation, true));
    }

    /**
     * 注册变量
     *
     * @param name 变量名
     * @param value 变量值
     */
    public void registerVariable(String name, Object value) {
        systemVariables.put(name, value);
    }

    /**
     * 注册扩展函数
     */
    public void registerExtensionFunction(Class<?> extensionClass, String name, int paramCount, NativeFunction.NativeCallable implementation) {
        extensionFunctions.computeIfAbsent(extensionClass, k -> new HashMap<>())
                .put(name, new NativeFunction(new SymbolFunction(name, paramCount), implementation));
    }

    /**
     * 注册扩展函数
     */
    public void registerExtensionFunction(Class<?> extensionClass, String name, List<Integer> paramCounts, NativeFunction.NativeCallable implementation) {
        extensionFunctions.computeIfAbsent(extensionClass, k -> new HashMap<>())
                .put(name, new NativeFunction(new SymbolFunction(name, paramCounts), implementation));
    }

    /**
     * 获取所有函数信息
     */
    public Map<String, Function> getSystemFunctions() {
        return new HashMap<>(systemFunctions);
    }

    /**
     * 获取所有变量信息
     */
    public Map<String, Object> getSystemVariables() {
        return new HashMap<>(systemVariables);
    }

    /**
     * 获取所有扩展函数信息
     */
    public Map<Class<?>, Map<String, Function>> getExtensionFunctions() {
        return new HashMap<>(extensionFunctions);
    }

    /**
     * 初始化解释器环境
     */
    public Environment newEnvironment() {
        return new Environment(systemFunctions, systemVariables, extensionFunctions);
    }

    /**
     * 初始化解释器环境
     *
     * @param environment 要初始化的环境
     */
    public void initializeEnvironment(Environment environment) {
        for (Map.Entry<String, Function> entry : systemFunctions.entrySet()) {
            environment.defineFunction(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Object> entry : systemVariables.entrySet()) {
            environment.defineVariable(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Class<?>, Map<String, Function>> entry : extensionFunctions.entrySet()) {
            environment.defineExtensionFunction(entry.getKey(), entry.getValue());
        }
    }
} 