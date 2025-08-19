package org.tabooproject.fluxon.parser;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.Symbolic;

import java.util.*;

/**
 * 符号环境（编译期环境）
 * 用于管理编译期间的函数和变量
 */
public class SymbolEnvironment {

    // 用户定义的函数
    private final Map<String, SymbolFunction> userFunctions = new HashMap<>();

    // 全局变量符号表
    private final Set<String> rootVariables = new LinkedHashSet<>();
    // 局部变量符号表
    private final Map<String, Set<String>> localVariables = new HashMap<>();

    // 当前函数
    @Nullable
    private String currentFunction;

    // 是否可以应用 break 语句
    private boolean isBreakable = false;
    // 是否可以应用 continue 语句
    private boolean isContinuable = false;
    // 是否在上下文调用环境
    private boolean isContextCall = false;

    /**
     * 定义用户函数
     *
     * @param name 函数名
     * @param info 函数信息
     */
    public void defineUserFunction(String name, SymbolFunction info) {
        userFunctions.put(name, info);
    }

    /**
     * 定义变量
     *
     * @param name 变量名
     */
    public void defineVariable(String name) {
        if (currentFunction == null) {
            rootVariables.add(name);
        } else {
            localVariables.computeIfAbsent(currentFunction, i -> new LinkedHashSet<>()).add(name);
        }
    }

    /**
     * 定义全局变量
     *
     * @param variables 变量
     */
    public void defineRootVariables(Map<String, Object> variables) {
        rootVariables.addAll(variables.keySet());
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
                userFunctions.put(entry.getKey(), ((Symbolic) function).getInfo());
            } else {
                userFunctions.put(entry.getKey(), SymbolFunction.of(function));
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
        return userFunctions.get(name);
    }

    /**
     * 变量是否存在
     *
     * @param name 变量名
     * @return 是否存在
     */
    public boolean hasVariable(String name) {
        if (rootVariables.contains(name)) return true;
        if (currentFunction != null) {
            return localVariables.containsKey(currentFunction) && localVariables.get(currentFunction).contains(name);
        }
        return false;
    }

    /**
     * 获取局部变量的位置
     *
     * @param name 变量名
     * @return 返回变量索引
     */
    public int getLocalVariable(String name) {
        if (currentFunction != null) {
            Set<String> localVariables = this.localVariables.get(currentFunction);
            if (localVariables != null) {
                int index = 0;
                for (String var : localVariables) {
                    if (var.equals(name)) {
                        return index;
                    }
                    index++;
                }
            }
        }
        return -1;
    }

    /**
     * 获取用户定义的函数
     */
    public Map<String, SymbolFunction> getUserFunctions() {
        return userFunctions;
    }

    /**
     * 获取全局变量符号表
     */
    public Set<String> getRootVariables() {
        return rootVariables;
    }

    /**
     * 获取局部变量符号表
     */
    public Map<String, Set<String>> getLocalVariables() {
        return localVariables;
    }

    /**
     * 获取当前函数名
     */
    @Nullable
    public String getCurrentFunction() {
        return currentFunction;
    }

    /**
     * 设置当前函数名
     */
    public void setCurrentFunction(@Nullable String currentFunction) {
        this.currentFunction = currentFunction;
    }

    /**
     * 设置是否可以应用 break 语句
     */
    public void setBreakable(boolean breakable) {
        this.isBreakable = breakable;
    }

    /**
     * 设置是否可以应用 continue 语句
     */
    public void setContinuable(boolean continuable) {
        this.isContinuable = continuable;
    }

    /**
     * 判断是否可以应用 break 语句
     */
    public boolean isBreakable() {
        return isBreakable;
    }

    /**
     * 判断是否可以应用 continue 语句
     */
    public boolean isContinuable() {
        return isContinuable;
    }

    /**
     * 设置是否在上下文调用环境
     */
    public void setContextCall(boolean isContextCall) {
        this.isContextCall = isContextCall;
    }

    /**
     * 判断是否在上下文调用环境
     */
    public boolean isContextCall() {
        return isContextCall;
    }

    @Override
    public String toString() {
        return "SymbolEnvironment{" +
                "userFunctions=" + userFunctions +
                ", rootVariables=" + rootVariables +
                ", localVariables=" + localVariables +
                ", currentFunction='" + currentFunction + '\'' +
                ", isBreakable=" + isBreakable +
                ", isContinuable=" + isContinuable +
                ", isContextCall=" + isContextCall +
                '}';
    }
}