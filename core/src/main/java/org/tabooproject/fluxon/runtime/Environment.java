package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.interpreter.error.FunctionNotFoundException;
import org.tabooproject.fluxon.interpreter.error.VariableNotFoundException;

import java.util.HashMap;
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

    // 扩展函数
    private final Map<Class<?>, Map<String, Function>> extensionFunctions = new HashMap<>();

    // 父环境，用于实现作用域链
    private final Environment parent;

    /**
     * 创建顶层环境（全局环境）
     */
    public Environment() {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    /**
     * 创建顶层环境（全局环境）
     */
    public Environment(Map<String, Function> functions, Map<String, Object> values, Map<Class<?>, Map<String, Function>> extensionFunctions) {
        this.parent = null;
        this.functions.putAll(functions);
        this.variables.putAll(values);
        this.extensionFunctions.putAll(extensionFunctions);
    }

    /**
     * 创建子环境
     *
     * @param parent 父环境
     */
    public Environment(Environment parent) {
        this.parent = parent;
    }

    /**
     * 获取父环境
     *
     * @return 父环境
     */
    public Environment getParent() {
        return parent;
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
        extensionFunctions.computeIfAbsent(extensionClass, k -> new HashMap<>()).put(name, value);
    }

    /**
     * 在当前环境中定义扩展函数
     *
     * @param extensionClass 扩展类
     * @param functions      函数映射
     */
    public void defineExtensionFunction(Class<?> extensionClass, Map<String, Function> functions) {
        extensionFunctions.put(extensionClass, functions);
    }

    /**
     * 获取函数（递归查找所有父环境）
     *
     * @param name 函数名
     * @return 函数值
     * @throws RuntimeException 如果变量不存在
     */
    @NotNull
    public Function getFunction(String name) {
        if (functions.containsKey(name)) {
            return functions.get(name);
        }
        if (parent != null) {
            return parent.getFunction(name);
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
        // 首先在当前环境中查找精确匹配的扩展函数
        Map<String, Function> exactExtensionFunctions = this.extensionFunctions.get(extensionClass);
        if (exactExtensionFunctions != null && exactExtensionFunctions.containsKey(name)) {
            return exactExtensionFunctions.get(name);
        }
        // 如果没有找到精确匹配，尝试模糊匹配（查找兼容的类型）
        for (Map.Entry<Class<?>, Map<String, Function>> entry : this.extensionFunctions.entrySet()) {
            // 检查是否为兼容类型（父类或接口）
            if (entry.getKey().isAssignableFrom(extensionClass)) {
                Map<String, Function> compatibleFunctions = entry.getValue();
                if (compatibleFunctions.containsKey(name)) {
                    return compatibleFunctions.get(name);
                }
            }
        }
        if (parent != null) {
            return parent.getExtensionFunction(extensionClass, name);
        }
        throw new FunctionNotFoundException(name);
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
     * @throws RuntimeException 如果变量不存在
     */
    @NotNull
    public Object get(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent != null) {
            return parent.get(name);
        }
        throw new VariableNotFoundException(name);
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
        if (variables.containsKey(name)) {
            return this;
        }
        if (parent != null) {
            return parent.getEnvironment(name);
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
        return new HashMap<>(extensionFunctions);
    }
}