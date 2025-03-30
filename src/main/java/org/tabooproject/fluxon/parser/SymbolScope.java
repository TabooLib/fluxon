package org.tabooproject.fluxon.parser;

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
    // 父作用域
    private final SymbolScope parent;

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
    public boolean isGlobal() {
        return parent == null;
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
     * 获取函数信息（仅在当前作用域中查找）
     *
     * @param name 函数名
     * @return 函数信息，如果不存在则返回null
     */
    public SymbolFunction getFunctionLocal(String name) {
        return functions.get(name);
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
     *
     * @return 函数符号表
     */
    public Map<String, SymbolFunction> getFunctions() {
        return functions;
    }

    /**
     * 获取所有变量（仅当前作用域）
     *
     * @return 变量符号表
     */
    public Set<String> getVariables() {
        return variables;
    }

    @Override
    public String toString() {
        return "SymbolScope{" +
                "variables=" + variables +
                ", functions=" + functions +
                '}';
    }
}