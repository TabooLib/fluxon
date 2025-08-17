package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.interpreter.error.FunctionNotFoundException;
import org.tabooproject.fluxon.interpreter.error.VariableNotFoundException;
import org.tabooproject.fluxon.parser.VariablePosition;
import org.tabooproject.fluxon.util.KV;

import java.util.*;

/**
 * 环境类
 * 用于管理运行时的变量和函数
 */
public class Environment {

    // 类型
    public static final Type TYPE = new Type(Environment.class);

    // 函数
    @Nullable
    private final Map<String, Function> functions;
    @Nullable
    private final Function[] systemFunctions;

    // 扩展函数
    @Nullable
    private final Map<String, Map<Class<?>, Function>> extensionFunctions;
    @Nullable
    private final KV<Class<?>, Function>[][] systemExtensionFunctions;

    // 根变量
    @Nullable
    private final Map<String, Object> rootVariables;
    // 局部变量
    @Nullable
    private final Object[] localVariables;
    // 局部变量对照表
    @Nullable
    private final String[] localVariableNames;

    // 层级
    private final int level;

    // 父环境
    @Nullable
    private final Environment parent;

    // 根环境
    @NotNull
    private final Environment root;

    /**
     * 创建顶层环境（全局环境）
     */
    @SuppressWarnings({"unchecked"})
    public Environment(@NotNull Map<String, Function> functions, @NotNull Map<String, Object> values, @NotNull Map<String, Map<Class<?>, Function>> extensionFunctions) {
        this.level = 0;
        this.parent = null;
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
     * 创建子环境
     *
     * @param parent 父环境
     */
    public Environment(@NotNull Environment parent, @NotNull Environment root, int localVariables) {
        this.level = parent.level + 1;
        this.parent = parent;
        this.root = root;
        this.functions = null;
        this.systemFunctions = null;
        this.extensionFunctions = null;
        this.systemExtensionFunctions = null;
        this.rootVariables = null;
        this.localVariables = localVariables > 0 ? new Object[localVariables] :  null;
        this.localVariableNames = localVariables > 0 ? new String[localVariables] : null;
    }

    /**
     * 获取层级
     */
    public int getLevel() {
        return level;
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
     * 如果自己是根环境，则返回自己
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
    @NotNull
    public Function getFunction(String name) {
        Function function = Objects.requireNonNull(root.functions).get(name);
        if (function != null) {
            return function;
        }
        throw new FunctionNotFoundException(name, root.functions);
    }

    /**
     * 获取函数（只查找根环境）
     *
     * @param name 函数名
     * @return 函数值
     */
    @Nullable
    public Function getFunctionOrNull(String name) {
        return Objects.requireNonNull(root.functions).get(name);
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
    public Function getExtensionFunction(Class<?> extensionClass, String name, int index) {
        Function function = getExtensionFunctionOrNull(extensionClass, name, index);
        if (function != null) {
            return function;
        }
        throw new FunctionNotFoundException(name, Objects.requireNonNull(root.functions));
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
            if (classFunctionMap != null) {
                for (KV<Class<?>, Function> entry : classFunctionMap) {
                    if (entry.getKey().isAssignableFrom(extensionClass)) {
                        return entry.getValue();
                    }
                }
            }
        } else {
            Map<Class<?>, Function> classFunctionMap = Objects.requireNonNull(root.extensionFunctions).get(name);
            if (classFunctionMap != null) {
                // 查找兼容的类型
                for (Map.Entry<Class<?>, Function> entry : classFunctionMap.entrySet()) {
                    if (entry.getKey().isAssignableFrom(extensionClass)) {
                        return entry.getValue();
                    }
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
     * 获取变量值
     * 根据 position 参数决定更新局部变量还是根变量
     *
     * @param name 变量名
     * @param level 层级（-1 表示根变量）
     * @param index 索引（-1 索引表示根变量）
     *
     * @return 变量值
     * @throws VariableNotFoundException 如果变量不存在
     */
    @NotNull
    public Object get(@NotNull String name, int level, int index) {
        Object value = getOrNull(name, level, index);
        if (value != null) {
            return value;
        }
        throw new VariableNotFoundException(name);
    }

    /**
     * 获取变量值
     * 根据 position 参数决定更新局部变量还是根变量
     *
     * @param name 变量名
     * @param level 层级（-1 表示根变量）
     * @param index 索引（-1 索引表示根变量）
     *
     * @return 变量值
     */
    @Nullable
    public Object getOrNull(@NotNull String name, int level, int index) {
        if (level == -1 || index == -1) {
            return Objects.requireNonNull(root.rootVariables).get(name);
        } else {
            Environment targetEnv = getEnvironment(level);
            if (targetEnv != null) {
                return targetEnv.localVariables[index];
            }
        }
        return null;
    }

    /**
     * 更新变量值
     * 根据 position 参数决定更新局部变量还是根变量
     *
     * @param name  变量名
     * @param value 新的变量值
     */
    @Nullable
    public Environment assign(@NotNull String name, @Nullable Object value, int level, int index) {
        if (level == -1 || index == -1) {
            Objects.requireNonNull(root.rootVariables).put(name, value);
            return this;
        } else {
            // 根据层级找到对应的环境并更新局部变量
            Environment targetEnv = getEnvironment(level);
            if (targetEnv != null) {
                targetEnv.localVariables[index] = value;
                targetEnv.localVariableNames[index] = name;
            } else {
                throw new VariableNotFoundException(name);
            }
            return targetEnv;
        }
    }

    /**
     * 获取环境
     */
    @Nullable
    public Environment getEnvironment(int level) {
        Environment current = this;
        while (current != null && current.level != level) {
            current = current.parent;
        }
        return current;
    }

    /**
     * 获取根环境中的所有变量
     */
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
     * 获取根环境中的所有函数
     */
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
     * 从根环境到当前环境，dump 所有变量，展示层级关系
     * 
     * @return 包含层级关系的变量信息字符串
     */
    public String dumpVariables() {
        StringBuilder sb = new StringBuilder();
        dumpVariablesRecursive(sb, 0);
        return sb.toString();
    }

    /**
     * 递归地dump变量，从根环境开始
     * 
     * @param sb 字符串构建器
     * @param currentDepth 当前深度（用于缩进）
     */
    private void dumpVariablesRecursive(StringBuilder sb, int currentDepth) {
        // 先处理父环境（从根开始）
        if (parent != null) {
            parent.dumpVariablesRecursive(sb, currentDepth);
        }
        
        // 添加当前环境的信息
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < currentDepth; i++) {
            indentBuilder.append("\t");
        }
        String indent = indentBuilder.toString();
        sb.append(indent).append("Environment Level ").append(level).append(":\n");
        
        // 如果是 0 级（根环境），输出根变量
        if (level == 0 && !Objects.requireNonNull(rootVariables).isEmpty()) {
            for (Map.Entry<String, Object> entry : rootVariables.entrySet()) {
                Object value = entry.getValue();
                sb.append(indent).append("\t").append(entry.getKey()).append(" = ");
                if (value == null) {
                    sb.append("null");
                } else {
                    sb.append(value.getClass().getSimpleName()).append(": ").append(value);
                }
                sb.append("\n");
            }
        } else if (localVariables != null && localVariables.length > 0) {
            // 非根环境输出局部变量
            for (int i = 0; i < localVariables.length; i++) {
                Object value = localVariables[i];
                // 获取变量名
                String varName = (localVariableNames != null && i < localVariableNames.length) 
                    ? localVariableNames[i] 
                    : "[" + i + "]";
                sb.append(indent).append("\t").append(i).append(":").append(varName).append(" = ");
                if (value == null) {
                    sb.append("null");
                } else {
                    sb.append(value.getClass().getSimpleName()).append(": ").append(value);
                }
                sb.append("\n");
            }
        } else {
            sb.append(indent).append("\t(no variables)\n");
        }
        sb.append("\n");
    }
}