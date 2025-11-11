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
    // 局部变量符号表：函数名 -> (变量名 -> 槽位索引)
    private final Map<String, LinkedHashMap<String, Integer>> localVariables = new HashMap<>();
    // 函数父子关系：子函数 -> 父函数
    private final Map<String, String> functionParents = new HashMap<>();
    // 捕获变量：函数名 -> 捕获信息
    private final Map<String, LinkedHashMap<String, VariableCapture>> functionCaptures = new HashMap<>();

    // 当前函数
    @Nullable
    private String currentFunction;

    // 是否可以应用 break/continue
    private boolean isBreakable = false;
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
            ensureLocalSlot(currentFunction, name);
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
        if (rootVariables.contains(name)) {
            return true;
        }
        return findNearestBinding(name) != null;
    }

    /**
     * 获取局部变量的位置
     *
     * @param name 变量名
     * @return 返回变量索引，如果是外层作用域的变量返回 -1（表示需要通过捕获访问）
     */
    public int getLocalVariable(String name) {
        if (currentFunction == null) {
            return -1;
        }
        LinkedHashMap<String, Integer> locals = this.localVariables.get(currentFunction);
        if (locals == null) {
            return -1;
        }
        Integer index = locals.get(name);
        return index != null ? index : -1;
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
        Map<String, Set<String>> view = new HashMap<>();
        for (Map.Entry<String, LinkedHashMap<String, Integer>> entry : localVariables.entrySet()) {
            view.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue().keySet()));
        }
        return view;
    }

    /**
     * 获取指定函数的局部变量表
     */
    public LinkedHashMap<String, Integer> getLocalVariableTable(String functionName) {
        return localVariables.get(functionName);
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
                isBreakable,
                isContinuable,
                isContextCall
        );
        if (functionName != null) {
            functionParents.put(functionName, currentFunction);
        }
        this.currentFunction = functionName;
        this.isBreakable = false;
        this.isContinuable = false;
        this.isContextCall = false;
        if (functionName != null) {
            functionCaptures.put(functionName, new LinkedHashMap<>());
            localVariables.computeIfAbsent(functionName, key -> new LinkedHashMap<>());
        }
        return snapshot;
    }

    /**
     * 恢复作用域快照
     * 在解析完嵌套函数后恢复外层上下文
     */
    public void popFunctionScope(FunctionScopeSnapshot snapshot) {
        this.currentFunction = snapshot.getPreviousFunction();
        this.isBreakable = snapshot.isBreakable();
        this.isContinuable = snapshot.isContinuable();
        this.isContextCall = snapshot.isContextCall();
    }

    /**
     * 解析变量引用
     * 返回引用类型及索引（捕获变量会被视为当前函数局部变量）
     */
    public ReferenceResolution resolveReference(String name) {
        if (rootVariables.contains(name)) {
            return ReferenceResolution.root();
        }
        ScopeMatch match = findNearestBinding(name);
        if (match == null) {
            return ReferenceResolution.undefined();
        }
        if (Objects.equals(match.functionName, currentFunction)) {
            return ReferenceResolution.local(match.index);
        }
        if (currentFunction == null) {
            return ReferenceResolution.undefined();
        }
        // 捕获外层变量
        LinkedHashMap<String, VariableCapture> captures = functionCaptures.computeIfAbsent(currentFunction, key -> new LinkedHashMap<>());
        VariableCapture capture = captures.get(name);
        if (capture == null) {
            int lambdaIndex = ensureLocalSlot(currentFunction, name);
            capture = new VariableCapture(name, match.index, lambdaIndex);
            captures.put(name, capture);
        }
        return ReferenceResolution.local(capture.lambdaIndex);
    }

    /**
     * 获取并清空指定函数的捕获变量
     */
    public List<CapturedVariable> drainCapturedVariables(String functionName) {
        LinkedHashMap<String, VariableCapture> captures = functionCaptures.remove(functionName);
        if (captures == null || captures.isEmpty()) {
            return Collections.emptyList();
        }
        List<CapturedVariable> list = new ArrayList<>(captures.size());
        for (VariableCapture capture : captures.values()) {
            list.add(new CapturedVariable(capture.name, capture.sourceIndex, capture.lambdaIndex));
        }
        return list;
    }

    private int ensureLocalSlot(String functionName, String name) {
        LinkedHashMap<String, Integer> locals = localVariables.computeIfAbsent(functionName, key -> new LinkedHashMap<>());
        Integer existing = locals.get(name);
        if (existing != null) {
            return existing;
        }
        int index = locals.size();
        locals.put(name, index);
        return index;
    }

    private ScopeMatch findNearestBinding(String name) {
        String function = currentFunction;
        while (function != null) {
            LinkedHashMap<String, Integer> locals = localVariables.get(function);
            if (locals != null) {
                Integer index = locals.get(name);
                if (index != null) {
                    return new ScopeMatch(function, index);
                }
            }
            function = functionParents.get(function);
        }
        return null;
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