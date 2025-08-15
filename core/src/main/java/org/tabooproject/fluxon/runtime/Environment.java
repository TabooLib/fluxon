package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.interpreter.error.FunctionNotFoundException;
import org.tabooproject.fluxon.interpreter.error.VariableNotFoundException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 环境类
 * 用于管理运行时的变量和函数
 */
public class Environment {

    // 类型
    public static final Type TYPE = new Type(Environment.class);

    // 函数
    private final Map<String, Function> functions = new HashMap<>();
    // 变量
    private final Map<String, Object> variables = new HashMap<>();

    // 扩展函数 - 优化后的数据结构：函数名 -> (类型 -> 函数实现)
    private final Map<String, Map<Class<?>, Function>> extensionFunctions = new HashMap<>();

    // 父环境，用于实现作用域链
    @Nullable
    private final Environment parent;

    // 根环境
    @NotNull
    private final Environment root;

    /**
     * 创建顶层环境（全局环境）
     */
    public Environment(Map<String, Function> functions, Map<String, Object> values, Map<Class<?>, Map<String, Function>> extensionFunctions) {
        this.parent = null;
        this.root = this;
        this.functions.putAll(functions);
        this.variables.putAll(values);
        this.extensionFunctions.putAll(convertToOptimizedStructure(extensionFunctions));
    }

    /**
     * 创建子环境
     *
     * @param parent 父环境
     */
    public Environment(@Nullable Environment parent, @NotNull Environment root) {
        this.parent = parent;
        this.root = root;
    }

    /**
     * 获取父环境
     */
    @Nullable
    public Environment getParent() {
        return parent;
    }

    /**
     * 获取根环境
     */
    @NotNull
    public Environment getRoot() {
        return root;
    }

    /**
     * 判断是否为全局环境
     *
     * @return 是否为全局环境
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * 在当前环境中定义函数
     *
     * @param name  函数名
     * @param value 函数对象
     */
    public void defineFunction(String name, Function value) {
        functions.put(name, value);
    }

    /**
     * 在当前环境中定义扩展函数
     *
     * @param extensionClass 扩展类
     * @param name           函数名
     * @param value          函数对象
     */
    public void defineExtensionFunction(Class<?> extensionClass, String name, Function value) {
        extensionFunctions.computeIfAbsent(name, k -> new LinkedHashMap<>()).put(extensionClass, value);
    }

    /**
     * 在当前环境中定义扩展函数
     *
     * @param extensionClass 扩展类
     * @param functions      函数映射
     */
    public void defineExtensionFunction(Class<?> extensionClass, Map<String, Function> functions) {
        for (Map.Entry<String, Function> entry : functions.entrySet()) {
            defineExtensionFunction(extensionClass, entry.getKey(), entry.getValue());
        }
    }

    /**
     * 获取函数（只查找根环境）
     *
     * @param name 函数名
     * @return 函数值
     * @throws RuntimeException 如果变量不存在
     */
    @NotNull
    public Function getFunction(String name) {
        Function function = root.functions.get(name);
        if (function != null) {
            return function;
        }
        throw new FunctionNotFoundException(name);
    }

    /**
     * 获取扩展函数（递归查找所有父环境）
     *
     * @param extensionClass 扩展类
     * @param name           函数名
     * @return 函数值
     * @throws RuntimeException 如果变量不存在
     */
    @NotNull
    public Function getExtensionFunction(Class<?> extensionClass, String name) {
        Function function = getExtensionFunctionOrNull(extensionClass, name);
        if (function != null) {
            return function;
        }
        throw new FunctionNotFoundException(name);
    }

    /**
     * 获取扩展函数（只查找根环境）
     *
     * @param extensionClass 扩展类
     * @param name           函数名
     * @return 函数值
     */
    @Nullable
    public Function getExtensionFunctionOrNull(Class<?> extensionClass, String name) {
        Map<Class<?>, Function> classFunctionMap = root.extensionFunctions.get(name);
        if (classFunctionMap != null) {
            // 查找兼容的类型
            for (Map.Entry<Class<?>, Function> entry : classFunctionMap.entrySet()) {
                if (entry.getKey().isAssignableFrom(extensionClass)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 在当前环境中定义变量
     * 仅在特殊情况下使用，例如在函数中定义的变量
     *
     * @param name  变量名
     * @param value 变量值
     */
    public void defineVariable(String name, Object value) {
        variables.put(name, value);
    }

    /**
     * 获取变量值
     * 如果不存在则会从父环境中查找
     *
     * @param name 变量名
     * @return 变量值
     * @throws VariableNotFoundException 如果变量不存在
     */
    @NotNull
    public Object get(String name) {
        Object value = getOrNull(name);
        if (value != null) {
            return value;
        }
        throw new VariableNotFoundException(name);
    }

    @Nullable
    public Object getOrNull(String name) {
        Environment current = this;
        while (current != null) {
            Object var = current.variables.get(name);
            if (var != null) {
                return var;
            }
            current = current.parent;
        }
        return null;
    }

    /**
     * 更新变量值
     * 优先从环境中查找，如果不存在则在当前环境中创建
     *
     * @param name  变量名
     * @param value 新的变量值
     */
    public void assign(String name, Object value) {
        Environment environment = getEnvironment(name);
        if (environment != null) {
            environment.variables.put(name, value);
        } else {
            variables.put(name, value);
        }
    }

    /**
     * 查找变量所在的环境
     *
     * @param name 变量名
     * @return 变量所在的环境，如果不存在则返回null
     */
    @Nullable
    public Environment getEnvironment(String name) {
        Environment current = this;
        while (current != null) {
            if (current.variables.containsKey(name)) {
                return current;
            }
            current = current.parent;
        }
        return null;
    }

    /**
     * 获取当前环境中的所有函数
     */
    public Map<String, Function> getFunctions() {
        return new HashMap<>(functions);
    }

    /**
     * 获取当前环境中的所有变量
     */
    public Map<String, Object> getVariables() {
        return new HashMap<>(variables);
    }

    /**
     * 获取当前环境中的所有扩展函数
     */
    public Map<Class<?>, Map<String, Function>> getExtensionFunctions() {
        return convertToCompatibleStructure(extensionFunctions);
    }

    /**
     * 将 Map<Class, Map<String, Function>> 转换为 Map<String, Map<Class, Function>>
     * 用于优化查找性能
     */
    public static Map<String, Map<Class<?>, Function>> convertToOptimizedStructure(Map<Class<?>, Map<String, Function>> original) {
        Map<String, Map<Class<?>, Function>> optimized = new HashMap<>();
        for (Map.Entry<Class<?>, Map<String, Function>> classEntry : original.entrySet()) {
            Class<?> clazz = classEntry.getKey();
            for (Map.Entry<String, Function> funcEntry : classEntry.getValue().entrySet()) {
                optimized.computeIfAbsent(funcEntry.getKey(), k -> new LinkedHashMap<>())
                        .put(clazz, funcEntry.getValue());
            }
        }
        return optimized;
    }

    /**
     * 将 Map<String, Map<Class, Function>> 转换为 Map<Class, Map<String, Function>>
     * 用于保持向后兼容的API
     */
    public static Map<Class<?>, Map<String, Function>> convertToCompatibleStructure(Map<String, Map<Class<?>, Function>> optimized) {
        Map<Class<?>, Map<String, Function>> compatible = new HashMap<>();
        for (Map.Entry<String, Map<Class<?>, Function>> entry : optimized.entrySet()) {
            String functionName = entry.getKey();
            for (Map.Entry<Class<?>, Function> classEntry : entry.getValue().entrySet()) {
                compatible.computeIfAbsent(classEntry.getKey(), k -> new HashMap<>())
                        .put(functionName, classEntry.getValue());
            }
        }
        return compatible;
    }
}