package org.tabooproject.fluxon.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.Symbolic;

import java.util.*;

/**
 * 作用域类
 * 用于管理单个作用域内的符号（函数和变量）
 */
public class SymbolScope {

    // 用户定义的函数符号表
    private final Map<String, SymbolFunction> userFunctions = new HashMap<>();

    // 全局变量符号表
    private final Set<String> rootVariables = new LinkedHashSet<>();
    // 局部变量符号表
    private final Set<String> localVariables = new LinkedHashSet<>();

    // 层级
    private final int level;

    // 父作用域
    @Nullable
    private final SymbolScope parent;
    // 根作用域
    @NotNull
    private final SymbolScope root;

    // 是否可以应用 break 语句
    private boolean breakable = false;
    // 是否可以应用 continue 语句
    private boolean continuable = false;
    // 是否在上下文调用环境
    private boolean isContextCall = false;

    /**
     * 创建顶层作用域（全局作用域）
     */
    public SymbolScope() {
        this.level = 0;
        this.parent = null;
        this.root = this;
    }

    /**
     * 创建子作用域
     *
     * @param parent 父作用域
     */
    public SymbolScope(@NotNull SymbolScope parent, @NotNull SymbolScope root) {
        this.level = parent.level + 1;
        this.parent = parent;
        this.root = root;
    }

    /**
     * 获取层级
     */
    public int getLevel() {
        return level;
    }

    /**
     * 获取父作用域
     *
     * @return 父作用域
     */
    @Nullable
    public SymbolScope getParent() {
        return parent;
    }

    /**
     * 获取根作用域
     *
     * @return 根作用域
     */
    @NotNull
    public SymbolScope getRoot() {
        return root;
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
        SymbolScope current = this;
        while (current != null) {
            if (current.breakable) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }

    /**
     * 判断是否可以应用 continue 语句（进行递归检查）
     *
     * @return 是否可以应用 continue 语句
     */
    public boolean isContinuable() {
        SymbolScope current = this;
        while (current != null) {
            if (current.continuable) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }

    /**
     * 设置是否在上下文调用环境
     *
     * @param isContextCall 是否在上下文调用环境
     */
    public void setContextCall(boolean isContextCall) {
        this.isContextCall = isContextCall;
    }

    /**
     * 判断是否在上下文调用环境（递归查找）
     *
     * @return 是否在上下文调用环境
     */
    public boolean isContextCall() {
        SymbolScope current = this;
        while (current != null) {
            if (current.isContextCall) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }

    /**
     * 在根作用域中定义用户函数
     *
     * @param name 函数名
     * @param info 函数信息
     */
    public void defineUserFunction(String name, SymbolFunction info) {
        root.userFunctions.put(name, info);
    }

    /**
     * 在当前作用域中定义变量
     *
     * @param name 变量名
     */
    public void defineVariable(String name) {
        // 根层级
        if (level == 0) {
            root.rootVariables.add(name);
        } else {
            localVariables.add(name);
        }
    }

    /**
     * 在当前作用域中定义变量（批量）
     *
     * @param variables 变量
     */
    public void defineRootVariables(Map<String, Object> variables) {
        root.rootVariables.addAll(variables.keySet());
    }

    /**
     * 在根作用域中定义函数（批量）
     *
     * @param functions 函数映射
     */
    public void defineUserFunctions(Map<String, Function> functions) {
        for (Map.Entry<String, Function> entry : functions.entrySet()) {
            Function function = entry.getValue();
            if (function instanceof Symbolic) {
                root.userFunctions.put(entry.getKey(), ((Symbolic) function).getInfo());
            } else {
                root.userFunctions.put(entry.getKey(), SymbolFunction.of(function));
            }
        }
    }

    /**
     * 获取函数信息
     *
     * @param name 函数名
     * @return 函数信息，如果不存在则返回 null
     */
    public SymbolFunction getUserFunction(String name) {
        return root.userFunctions.get(name);
    }

    /**
     * 变量是否存在（递归查找所有父作用域）
     *
     * @param name 变量名
     * @return 是否存在
     */
    public boolean hasVariable(String name) {
        if (root.rootVariables.contains(name)) return true;
        SymbolScope current = this;
        while (current != null) {
            if (current.localVariables.contains(name)) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }

    /**
     * 获取局部变量的位置
     *
     * @param name 变量名
     * @return 返回变量所在的层级，以及索引
     */
    @Nullable
    public VariablePosition getLocalVariable(String name) {
        SymbolScope current = this;
        while (current != null) {
            if (current.localVariables.contains(name)) {
                // 计算在当前层级中的索引位置
                int index = 0;
                for (String var : current.localVariables) {
                    if (var.equals(name)) {
                        break;
                    }
                    index++;
                }
                return new VariablePosition(current.level, index);
            }
            current = current.parent;
        }
        return null;
    }

    /**
     * 获取所有函数（仅当前作用域）
     */
    public Map<String, SymbolFunction> getUserFunctions() {
        return userFunctions;
    }

    /**
     * 获取所有变量（仅当前作用域）
     */
    public Set<String> getRootVariables() {
        return rootVariables;
    }

    /**
     * 获取所有局部变量（仅当前作用域）
     */
    public Set<String> getLocalVariables() {
        return localVariables;
    }

    /**
     * 获取所有变量（递归查找所有父作用域）
     *
     * @return 所有变量
     */
    public Set<String> getAllVariables() {
        Set<String> allVariables = new HashSet<>(root.rootVariables);
        SymbolScope current = this;
        while (current != null) {
            allVariables.addAll(current.localVariables);
            current = current.parent;
        }
        return allVariables;
    }

    @Override
    public String toString() {
        return "SymbolScope{" +
                "rootVariables=" + rootVariables +
                ", localVariables=" + localVariables +
                ", userFunctions=" + userFunctions +
                '}';
    }
}