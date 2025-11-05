package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.interpreter.error.FunctionNotFoundException;
import org.tabooproject.fluxon.interpreter.error.VariableNotFoundException;
import org.tabooproject.fluxon.runtime.java.Export;
import org.tabooproject.fluxon.util.KV;

import java.util.*;

/**
 * 运行时环境
 * 用于管理运行时期间的函数和变量
 */
public class Environment {

    // 类型
    public static final Type TYPE = new Type(Environment.class);

    // 函数
    @Nullable
    protected final Map<String, Function> functions;
    @Nullable
    protected final Function[] systemFunctions;

    // 扩展函数
    @Nullable
    protected final Map<String, Map<Class<?>, Function>> extensionFunctions;
    @Nullable
    protected final KV<Class<?>, Function>[][] systemExtensionFunctions;

    // 根变量
    @Nullable
    protected final Map<String, Object> rootVariables;
    // 局部变量
    @Nullable
    protected final Object[] localVariables;
    // 局部变量对照表
    @Nullable
    protected final String[] localVariableNames;
    // 上下文目标
    @Nullable
    protected Object target;

    // 根环境
    @NotNull
    protected final Environment root;

    /**
     * 创建顶层环境（全局环境）
     */
    @SuppressWarnings({"unchecked"})
    public Environment(@NotNull Map<String, Function> functions, @NotNull Map<String, Object> values, @NotNull Map<String, Map<Class<?>, Function>> extensionFunctions) {
        this.root = this;
        this.functions = new HashMap<>(functions);
        this.systemFunctions = functions.values().toArray(new Function[0]);
        this.extensionFunctions = extensionFunctions;
        List<KV<Class<?>, Function>[]> systemExtensionFunctions = new ArrayList<>();
        for (Map.Entry<String, Map<Class<?>, Function>> entry : extensionFunctions.entrySet()) {
            List<KV<Class<?>, Function>> classFunctionMap = new ArrayList<>();
            for (Map.Entry<Class<?>, Function> entry2 : entry.getValue().entrySet()) {
                classFunctionMap.add(new KV<>(entry2.getKey(), entry2.getValue()));
            }
            systemExtensionFunctions.add(classFunctionMap.toArray(new KV[0]));
        }
        this.systemExtensionFunctions = systemExtensionFunctions.toArray(new KV[0][]);
        this.rootVariables = new HashMap<>(values);
        this.localVariables = null;
        this.localVariableNames = null;
    }

    /**
     * 创建子环境（函数环境）
     *
     * @param parentEnv 父环境
     */
    public Environment(@NotNull Environment parentEnv, int localVariables) {
        this.root = parentEnv.root;
        this.functions = null;
        this.systemFunctions = null;
        this.extensionFunctions = null;
        this.systemExtensionFunctions = null;
        this.rootVariables = null;
        this.localVariables = localVariables > 0 ? new Object[localVariables] : null;
        this.localVariableNames = localVariables > 0 ? new String[localVariables] : null;
    }

    /**
     * 获取根环境
     * 如果自己是根环境，则返回自己
     */
    @Export
    @NotNull
    public Environment getRoot() {
        return root;
    }

    /**
     * 在根环境中定义函数
     *
     * @param name  函数名
     * @param value 函数对象
     */
    public void defineRootFunction(String name, Function value) {
        Objects.requireNonNull(root.functions).put(name, value);
    }

    /**
     * 在根环境中定义扩展函数
     *
     * @param extensionClass 扩展类
     * @param name           函数名
     * @param value          函数对象
     */
    public void defineRootExtensionFunction(Class<?> extensionClass, String name, Function value) {
        Objects.requireNonNull(root.extensionFunctions).computeIfAbsent(name, k -> new LinkedHashMap<>()).put(extensionClass, value);
    }

    /**
     * 获取函数（只查找根环境）
     *
     * @param name 函数名
     * @return 函数值
     * @throws RuntimeException 如果变量不存在
     */
    @Export
    @NotNull
    public Function getFunction(String name) {
        Function function = Objects.requireNonNull(root.functions).get(name);
        if (function != null) {
            return function;
        }
        throw new FunctionNotFoundException(name);
    }

    /**
     * 获取函数（只查找根环境）
     *
     * @param name 函数名
     * @return 函数值
     */
    @Export
    @Nullable
    public Function getFunctionOrNull(String name) {
        return Objects.requireNonNull(root.functions).get(name);
    }

    /**
     * 获取扩展函数（只查找根环境）
     *
     * @param extensionClass 扩展类
     * @param name           函数名
     * @return 函数值
     * @throws RuntimeException 如果变量不存在
     */
    @NotNull
    public Function getExtensionFunction(Class<?> extensionClass, String name, int index) {
        Function function = getExtensionFunctionOrNull(extensionClass, name, index);
        if (function != null) {
            return function;
        }
        throw new FunctionNotFoundException(name, extensionClass, index);
    }

    /**
     * 获取扩展函数（只查找根环境）
     *
     * @param extensionClass 扩展类
     * @param name           函数名
     * @return 函数值
     */
    @Nullable
    public Function getExtensionFunctionOrNull(Class<?> extensionClass, String name, int index) {
        if (index != -1) {
            KV<Class<?>, Function>[] classFunctionMap = Objects.requireNonNull(root.systemExtensionFunctions)[index];
            // 查找兼容的类型
            for (KV<Class<?>, Function> entry : classFunctionMap) {
                if (entry.getKey() == extensionClass) return entry.getValue();
            }
            for (KV<Class<?>, Function> entry : classFunctionMap) {
                if (entry.getKey().isAssignableFrom(extensionClass)) return entry.getValue();
            }
        }
        // 回退逻辑，使用名称检索
        // 效率低于索引逻辑
        else {
            Map<Class<?>, Function> classFunctionMap = Objects.requireNonNull(root.extensionFunctions).get(name);
            if (classFunctionMap != null) {
                // 查找兼容的类型
                Set<Map.Entry<Class<?>, Function>> entries = classFunctionMap.entrySet();
                for (Map.Entry<Class<?>, Function> entry : entries) {
                    if (entry.getKey() == extensionClass) return entry.getValue();
                }
                for (Map.Entry<Class<?>, Function> entry : entries) {
                    if (entry.getKey().isAssignableFrom(extensionClass)) return entry.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 在根环境中定义变量
     *
     * @param name  变量名
     * @param value 变量值
     */
    public void defineRootVariable(@NotNull String name, @Nullable Object value) {
        Objects.requireNonNull(root.rootVariables).put(name, value);
    }

    /**
     * 判断变量是否存在
     *
     * @param name  变量名
     * @param index 索引（-1 索引表示根变量）
     * @return 存在与否
     */
    public boolean has(@NotNull String name, int index) {
        if (index == -1) {
            return Objects.requireNonNull(root.rootVariables).containsKey(name);
        } else {
            return true;
        }
    }

    /**
     * 获取变量值
     * 根据 position 参数决定更新局部变量还是根变量
     *
     * @param name  变量名
     * @param index 索引（-1 索引表示根变量）
     * @return 变量值
     */
    @Nullable
    public Object get(@NotNull String name, int index) {
        if (index == -1) {
            return Objects.requireNonNull(root.rootVariables).get(name);
        } else {
            return localVariables[index];
        }
    }

    /**
     * 更新变量值
     * 根据 position 参数决定更新局部变量还是根变量
     *
     * @param name  变量名
     * @param value 新的变量值
     * @param index 索引（-1 索引表示根变量）
     */
    public void assign(@NotNull String name, @Nullable Object value, int index) {
        if (index == -1) {
            Objects.requireNonNull(root.rootVariables).put(name, value);
        } else {
            if (index < localVariables.length) {
                localVariables[index] = value;
                localVariableNames[index] = name;
            } else {
                throw new VariableNotFoundException(name + ", index: " + index, Arrays.asList(localVariableNames));
            }
        }
    }

    /**
     * 获取根环境中的所有函数
     */
    @Export
    public Map<String, Function> getRootFunctions() {
        return root.functions;
    }

    /**
     * 获取根环境中的所有系统函数
     */
    public Function[] getRootSystemFunctions() {
        return root.systemFunctions;
    }

    /**
     * 获取根环境中的所有扩展函数
     */
    @Export
    public Map<String, Map<Class<?>, Function>> getRootExtensionFunctions() {
        return root.extensionFunctions;
    }

    /**
     * 获取根环境中的所有系统扩展函数
     */
    public KV<Class<?>, Function>[][] getRootSystemExtensionFunctions() {
        return root.systemExtensionFunctions;
    }

    /**
     * 获取根环境中的所有变量
     */
    @Export
    public Map<String, Object> getRootVariables() {
        return root.rootVariables;
    }

    /**
     * 获取当前环境中的所有局部变量
     */
    @Nullable
    public Object[] getLocalVariables() {
        return localVariables;
    }

    /**
     * 获取当前环境中的所有局部变量名
     */
    public String[] getLocalVariableNames() {
        return localVariableNames;
    }

    /**
     * 获取当前环境中的目标对象
     */
    public @Nullable Object getTarget() {
        return target;
    }

    /**
     * 设置当前环境中的目标对象
     */
    public void setTarget(@Nullable Object target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return "Environment{" +
                "target=" + target +
                ", functions=" + functions +
                ", systemFunctions=" + Arrays.toString(systemFunctions) +
                ", extensionFunctions=" + extensionFunctions +
                ", systemExtensionFunctions=" + Arrays.toString(systemExtensionFunctions) +
                ", rootVariables=" + rootVariables +
                '}';
    }
}