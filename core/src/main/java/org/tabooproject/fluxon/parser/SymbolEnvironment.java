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

    /**
     * 函数作用域快照
     * 用于保存和恢复函数解析上下文
     */
    public static class FunctionScopeSnapshot {
        private final String currentFunction;
        private final String parentFunction;
        private final boolean isBreakable;
        private final boolean isContinuable;
        private final boolean isContextCall;

        public FunctionScopeSnapshot(String currentFunction, String parentFunction, boolean isBreakable, boolean isContinuable, boolean isContextCall) {
            this.currentFunction = currentFunction;
            this.parentFunction = parentFunction;
            this.isBreakable = isBreakable;
            this.isContinuable = isContinuable;
            this.isContextCall = isContextCall;
        }

        public String getCurrentFunction() {
            return currentFunction;
        }

        public String getParentFunction() {
            return parentFunction;
        }

        public boolean isBreakable() {
            return isBreakable;
        }

        public boolean isContinuable() {
            return isContinuable;
        }

        public boolean isContextCall() {
            return isContextCall;
        }
    }

    // 用户定义的函数
    private final Map<String, SymbolFunction> userFunctions = new HashMap<>();

    // 全局变量符号表
    private final Set<String> rootVariables = new LinkedHashSet<>();
    // 局部变量符号表
    private final Map<String, Set<String>> localVariables = new HashMap<>();

    // 当前函数
    @Nullable
    private String currentFunction;
    
    // 父函数（用于支持嵌套作用域）
    @Nullable
    private String parentFunction;

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
        
        // 沿着作用域链向上查找
        String searchFunction = currentFunction;
        while (searchFunction != null) {
            Set<String> locals = localVariables.get(searchFunction);
            if (locals != null && locals.contains(name)) {
                return true;
            }
            // 查找父作用域
            if (searchFunction.equals(currentFunction) && parentFunction != null) {
                searchFunction = parentFunction;
            } else {
                // 没有更多父作用域
                break;
            }
        }
        return false;
    }

    /**
     * 获取局部变量的位置
     *
     * @param name 变量名
     * @return 返回变量索引，如果是外层作用域的变量返回 -1（表示需要通过捕获访问）
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
            // 检查是否在父作用域中（用于闭包捕获）
            // 注意：这里返回 -1 表示是捕获的变量，运行时需要从环境链中查找
            if (parentFunction != null) {
                Set<String> parentLocals = this.localVariables.get(parentFunction);
                if (parentLocals != null && parentLocals.contains(name)) {
                    return -1; // 标记为捕获变量
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

    /**
     * 创建当前作用域快照
     * 用于解析嵌套函数（如 lambda）时保存外层上下文
     */
    public FunctionScopeSnapshot pushFunctionScope(String functionName) {
        FunctionScopeSnapshot snapshot = new FunctionScopeSnapshot(
                currentFunction,
                parentFunction,
                isBreakable,
                isContinuable,
                isContextCall
        );
        // 设置新函数的父作用域为当前函数
        this.parentFunction = currentFunction;
        this.currentFunction = functionName;
        this.isBreakable = false;
        this.isContinuable = false;
        this.isContextCall = false;
        return snapshot;
    }

    /**
     * 恢复作用域快照
     * 在解析完嵌套函数后恢复外层上下文
     */
    public void popFunctionScope(FunctionScopeSnapshot snapshot) {
        this.currentFunction = snapshot.getCurrentFunction();
        this.parentFunction = snapshot.getParentFunction();
        this.isBreakable = snapshot.isBreakable();
        this.isContinuable = snapshot.isContinuable();
        this.isContextCall = snapshot.isContextCall();
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