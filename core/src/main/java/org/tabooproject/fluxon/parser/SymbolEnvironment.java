package org.tabooproject.fluxon.parser;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.expression.literal.Literal;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.OverloadSet;
import org.tabooproject.fluxon.runtime.Symbolic;
import org.tabooproject.fluxon.runtime.Type;

import java.util.*;

/**
 * 符号环境（编译期环境）
 * 用于管理编译期间的函数和变量
 */
public class SymbolEnvironment {

    // 根层级局部变量的特殊 key
    private static final String ROOT_LOCAL_KEY = "";

    // 用户定义的函数
    private final Map<String, SymbolFunction> userFunctions = new HashMap<>();
    // 全局变量符号表（变量名 -> 类型）
    private final Map<String, Type> rootVariables = new LinkedHashMap<>();
    // 局部变量符号表
    private final Map<String, Set<String>> localVariables = new HashMap<>();
    // 常量符号表（名称 -> 字面量 AST 节点）
    private final Map<String, Literal> constants = new LinkedHashMap<>();

    // 当前函数
    @Nullable
    private String currentFunction;
    // 强制所有根层级变量使用 localVariables 存储
    private boolean forceLocalVariables = false;

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
            // 根层级：强制模式或 _ 前缀变量使用 localVariables（临时变量）
            if (forceLocalVariables || (!name.isEmpty() && name.charAt(0) == '_')) {
                localVariables.computeIfAbsent(ROOT_LOCAL_KEY, i -> new LinkedHashSet<>()).add(name);
            } else {
                rootVariables.putIfAbsent(name, Type.OBJECT);
            }
        } else {
            localVariables.computeIfAbsent(currentFunction, i -> new LinkedHashSet<>()).add(name);
        }
    }

    /**
     * 强制定义为局部变量（用于 for 循环、try-catch 等临时变量）
     * 无论是否在函数内部，无论是否有 _ 前缀，都放入 localVariables
     *
     * @param name 变量名
     */
    public void defineLocalVariable(String name) {
        String key = currentFunction != null ? currentFunction : ROOT_LOCAL_KEY;
        localVariables.computeIfAbsent(key, i -> new LinkedHashSet<>()).add(name);
    }

    /**
     * 定义单个 root 变量（带类型）
     *
     * @param name 变量名
     * @param type 类型
     */
    public void defineRootVariable(String name, Type type) {
        rootVariables.put(name, type);
    }

    /**
     * 批量定义 root 变量（带类型）
     *
     * @param variables 变量映射
     */
    public void defineRootVariables(Map<String, Type> variables) {
        rootVariables.putAll(variables);
    }

    /**
     * 从值推断类型并定义 root 变量
     *
     * @param variables 变量
     */
    public void defineRootVariablesFromValues(Map<String, Object> variables) {
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            Object value = entry.getValue();
            Type type = value != null ? Type.fromClass(value.getClass()) : Type.OBJECT;
            rootVariables.put(entry.getKey(), type);
        }
    }

    /**
     * 在根作用域中定义函数（批量）
     *
     * @param functions 函数映射
     */
    public void defineUserFunctions(Map<String, OverloadSet> functions) {
        for (Map.Entry<String, OverloadSet> entry : functions.entrySet()) {
            String name = entry.getKey();
            Function function = entry.getValue().first();
            if (function == null) continue;
            SymbolFunction symbolFunc = null;
            // 优先使用 Symbolic 接口获取符号信息
            if (function instanceof Symbolic) {
                symbolFunc = ((Symbolic) function).getInfo();
            }
            // 如果没有符号信息，尝试从 Function 接口构建
            if (symbolFunc == null) {
                try {
                    symbolFunc = SymbolFunction.of(function);
                } catch (Exception e) {
                    // 如果无法从 Function 构建，使用 map 的 key 作为函数名
                    // 创建一个支持任意参数数量的符号函数
                    symbolFunc = SymbolFunction.varargs(name);
                }
            }
            userFunctions.put(name, symbolFunc);
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
        // 检查普通根变量
        if (rootVariables.containsKey(name)) return true;
        // 检查局部变量（函数内或根层级 _ 前缀变量）
        String key = currentFunction != null ? currentFunction : ROOT_LOCAL_KEY;
        Set<String> vars = localVariables.get(key);
        return vars != null && vars.contains(name);
    }

    /**
     * 获取局部变量的位置
     *
     * @param name 变量名
     * @return 返回变量索引
     */
    public int getLocalVariable(String name) {
        // 确定要查找的 key：根层级用 ROOT_LOCAL_KEY，函数内用函数名
        String key = currentFunction != null ? currentFunction : ROOT_LOCAL_KEY;
        Set<String> vars = this.localVariables.get(key);
        if (vars != null) {
            int index = 0;
            for (String var : vars) {
                if (var.equals(name)) {
                    return index;
                }
                index++;
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
    public Map<String, Type> getRootVariables() {
        return rootVariables;
    }

    /**
     * 获取 root 变量类型
     *
     * @param name 变量名
     * @return 类型，不存在则返回 null
     */
    @Nullable
    public Type getRootVariableType(String name) {
        return rootVariables.get(name);
    }

    /**
     * 获取局部变量符号表
     */
    public Map<String, Set<String>> getLocalVariables() {
        return localVariables;
    }

    /**
     * 获取根层级局部变量数量（_ 前缀变量）
     */
    public int getRootLocalVariableCount() {
        Set<String> rootLocals = localVariables.get(ROOT_LOCAL_KEY);
        return rootLocals != null ? rootLocals.size() : 0;
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
     * 设置是否强制所有根层级变量使用 localVariables 存储
     *
     * @param forceLocalVariables true 表示强制使用临时变量
     */
    public void setForceLocalVariables(boolean forceLocalVariables) {
        this.forceLocalVariables = forceLocalVariables;
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
     * 检查标识符是否为常量命名（全大写）
     * 模式：[A-Z][A-Z0-9_]*
     *
     * @param name 标识符名称
     * @return 是否为常量命名
     */
    public static boolean isConstantName(String name) {
        if (name == null || name.isEmpty()) return false;
        char first = name.charAt(0);
        if (first < 'A' || first > 'Z') return false;
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')) {
                return false;
            }
        }
        return true;
    }

    /**
     * 定义常量
     *
     * @param name  常量名
     * @param value 字面量值
     */
    public void defineConstant(String name, Literal value) {
        constants.put(name, value);
    }

    /**
     * 检查是否为已定义的常量
     *
     * @param name 名称
     * @return 是否为常量
     */
    public boolean isConstant(String name) {
        return constants.containsKey(name);
    }

    /**
     * 获取常量的字面量值
     *
     * @param name 常量名
     * @return 字面量 AST 节点，不存在则返回 null
     */
    @Nullable
    public Literal getConstantLiteral(String name) {
        return constants.get(name);
    }

    /**
     * 获取所有常量
     */
    public Map<String, Literal> getConstants() {
        return constants;
    }

    @Override
    public String toString() {
        return "SymbolEnvironment{" +
                "userFunctions=" + userFunctions +
                ", rootVariables=" + rootVariables +
                ", localVariables=" + localVariables +
                ", constants=" + constants +
                ", currentFunction='" + currentFunction + '\'' +
                ", isBreakable=" + isBreakable +
                ", isContinuable=" + isContinuable +
                ", isContextCall=" + isContextCall +
                '}';
    }
}