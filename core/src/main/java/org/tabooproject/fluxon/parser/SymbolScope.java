package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.Symbolic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 作用域类
 * 用于管理单个作用域内的符号（函数和变量）
 */
public class SymbolScope {

    // 函数符号表
    private final Map<String, SymbolFunction> functions = new HashMap<>();
    // 变量符号表
    private final Set<String> variables = new HashSet<>();
    // 扩展函数符号表
    private final Set<SymbolFunction> extensionFunctions = new HashSet<>();
    // 父作用域
    private final SymbolScope parent;

    // 是否可以应用 break 语句
    private boolean breakable = true;
    // 是否可以应用 continue 语句
    private boolean continuable = true;

    /**
     * 创建顶层作用域（全局作用域）
     */
    public SymbolScope() {
        this.parent = null;
    }

    /**
     * 创建子作用域
     *
     * @param parent 父作用域
     */
    public SymbolScope(SymbolScope parent) {
        this.parent = parent;
    }

    /**
     * 获取父作用域
     *
     * @return 父作用域
     */
    public SymbolScope getParent() {
        return parent;
    }

    /**
     * 判断是否为全局作用域
     *
     * @return 是否为全局作用域
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * 设置是否可以应用 break 语句
     *
     * @param breakable 是否可以应用 break 语句
     */
    public void setBreakable(boolean breakable) {
        this.breakable = breakable;
    }

    /**
     * 设置是否可以应用 continue 语句
     *
     * @param continuable 是否可以应用 continue 语句
     */
    public void setContinuable(boolean continuable) {
        this.continuable = continuable;
    }

    /**
     * 判断是否可以应用 break 语句（进行递归检查）
     *
     * @return 是否可以应用 break 语句
     */
    public boolean isBreakable() {
        if (breakable) {
            return true;
        }
        return parent != null && parent.isBreakable();
    }

    /**
     * 判断是否可以应用 continue 语句（进行递归检查）
     *
     * @return 是否可以应用 continue 语句
     */
    public boolean isContinuable() {
        if (continuable) {
            return true;
        }
        return parent != null && parent.isContinuable();
    }

    /**
     * 在当前作用域中定义函数
     *
     * @param name 函数名
     * @param info 函数信息
     */
    public void defineFunction(String name, SymbolFunction info) {
        functions.put(name, info);
    }

    /**
     * 在当前作用域中定义变量
     *
     * @param name 变量名
     */
    public void defineVariable(String name) {
        variables.add(name);
    }

    /**
     * 在当前作用域中定义扩展函数
     *
     * @param info 函数信息
     */
    public void defineExtensionFunction(SymbolFunction info) {
        extensionFunctions.add(info);
    }

    /**
     * 在当前作用域中定义变量（批量）
     *
     * @param variables 变量
     */
    public void defineVariables(Map<String, Object> variables) {
        this.variables.addAll(variables.keySet());
    }

    /**
     * 在当前作用域中定义函数（批量）
     *
     * @param functions 函数映射
     */
    public void defineFunctions(Map<String, Function> functions) {
        for (Map.Entry<String, Function> entry : functions.entrySet()) {
            Function function = entry.getValue();
            if (function instanceof Symbolic) {
                this.functions.put(entry.getKey(), ((Symbolic) function).getInfo());
            } else {
                this.functions.put(entry.getKey(), SymbolFunction.of(function));
            }
        }
    }

    /**
     * 在当前作用域中定义扩展函数（批量）
     *
     * @param extensionFunctions 扩展函数映射
     */
    public void defineExtensionFunctions(Map<Class<?>, Map<String, Function>> extensionFunctions) {
        for (Map.Entry<Class<?>, Map<String, Function>> classEntry : extensionFunctions.entrySet()) {
            for (Map.Entry<String, Function> functionEntry : classEntry.getValue().entrySet()) {
                Function function = functionEntry.getValue();
                if (function instanceof Symbolic) {
                    this.extensionFunctions.add(((Symbolic) function).getInfo());
                } else {
                    this.extensionFunctions.add(SymbolFunction.of(function));
                }
            }
        }
    }

    /**
     * 获取函数信息（递归查找所有父作用域）
     *
     * @param name 函数名
     * @return 函数信息，如果不存在则返回null
     */
    public SymbolFunction getFunction(String name) {
        SymbolFunction info = functions.get(name);
        if (info != null) {
            return info;
        }
        return parent != null ? parent.getFunction(name) : null;
    }

    /**
     * 获取扩展函数信息（递归查找所有父作用域）
     *
     * @param name 函数名
     * @return 扩展函数信息集合
     */
    public Set<SymbolFunction> getExtensionFunctions(String name) {
        Set<SymbolFunction> result = new HashSet<>();
        // 在当前作用域中查找匹配的扩展函数
        for (SymbolFunction extensionFunction : extensionFunctions) {
            if (extensionFunction.getName().equals(name)) {
                result.add(extensionFunction);
            }
        }
        // 递归查找父作用域
        if (parent != null) {
            result.addAll(parent.getExtensionFunctions(name));
        }
        return result;
    }

    /**
     * 变量是否存在（递归查找所有父作用域）
     *
     * @param name 变量名
     * @return 是否存在
     */
    public boolean hasVariable(String name) {
        if (variables.contains(name)) {
            return true;
        }
        return parent != null && parent.hasVariable(name);
    }

    /**
     * 获取所有函数（仅当前作用域）
     */
    public Map<String, SymbolFunction> getFunctions() {
        return functions;
    }

    /**
     * 获取所有变量（仅当前作用域）
     */
    public Set<String> getVariables() {
        return variables;
    }

    /**
     * 获取所有扩展函数（仅当前作用域）
     */
    public Set<SymbolFunction> getExtensionFunctions() {
        return extensionFunctions;
    }

    /**
     * 获取所有变量（递归查找所有父作用域）
     *
     * @return 所有变量
     */
    public Set<String> getAllVariables() {
        Set<String> allVariables = new HashSet<>(variables);
        if (parent != null) {
            allVariables.addAll(parent.getAllVariables());
        }
        return allVariables;
    }

    @Override
    public String toString() {
        return "SymbolScope{" +
                "variables=" + variables +
                ", functions=" + functions +
                ", extensionFunctions=" + extensionFunctions +
                '}';
    }
}