package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.Label;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.expression.AnonymousClassExpression;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.OverloadSet;
import org.tabooproject.fluxon.runtime.Type;

import java.util.*;

public class CodeContext {

    // 类名和父类名
    private final String className;
    private final String superClassName;

    // 用户定义
    private final List<Definition> definitions = new ArrayList<>();
    private final List<LambdaFunctionDefinition> lambdaDefinitions = new ArrayList<>();
    private final List<AnonymousClassExpression> anonymousClassDefinitions = new ArrayList<>();
    private int anonymousClassIndex = 0;

    // 局部变量表
    private int localVarIndex = 0;

    // environment 局部变量槽位索引 (-1 表示使用字段，>=0 表示使用局部变量)
    private int environmentLocalSlot = -1;

    // FunctionContextPool 局部变量槽位索引（用于避免重复 ThreadLocal.get()）
    private int poolLocalSlot = -1;

    // Environment-free 模式：局部变量存储在 JVM local vars 而非 Environment
    private boolean envFreeMode = false;
    // 脚本变量位置 → JVM 局部变量槽位的映射
    private int[] varPosToJvmSlot;

    // 方法期望的返回类型（用于匿名类方法等场景）
    private Class<?> expectedReturnType = null;

    // 循环标签栈管理
    private final Stack<LoopContext> loopStack = new Stack<>();

    // Command 解析数据（运行时通过 index 访问）
    private final List<Object> commandDataList = new ArrayList<>();

    // 预解析的扩展函数常量池（编译期确定，运行时通过索引访问）
    private final List<ResolvedExtFuncInfo> resolvedExtensionFunctions = new ArrayList<>();

    /**
     * 预解析扩展函数信息
     */
    public static class ResolvedExtFuncInfo {
        public final int dispatchTableIndex;
        public final Class<?> targetClass;
        public final int overloadIndex;
        public ResolvedExtFuncInfo(int dispatchTableIndex, Class<?> targetClass, int overloadIndex) {
            this.dispatchTableIndex = dispatchTableIndex;
            this.targetClass = targetClass;
            this.overloadIndex = overloadIndex;
        }
    }

    // 延迟重载解析的 OverloadSet 常量池
    private final List<OverloadSet> deferredOverloadSets = new ArrayList<>();

    // 用户定义函数注册表：函数名 → 所属类的 JVM 内部名
    private final Map<String, String> userFunctionOwners = new HashMap<>();

    // 类型分析器（用于编译期优化局部变量存储）
    private TypeAnalyzer typeAnalyzer;

    // root 变量缓存作用域：只在已证明循环体不可外部观察时短暂启用
    private final Deque<Map<String, RootVariableCache>> rootVariableCacheScopes = new ArrayDeque<>();

    // 内联函数的局部变量槽位作用域：仅在生成被内联函数体期间可见
    private final Deque<Map<Integer, InlineLocalVariable>> inlineLocalVariableScopes = new ArrayDeque<>();
    // 顶层直接赋值产生的 root 常量，仅用于后续紧邻循环缓存初始化
    private final Map<String, Object> rootConstantValues = new HashMap<>();
    // 同一段纯直线代码中的 root 引用也会读取该表，遇到可观察语义边界时由主类生成器清空。
    private boolean rootConstantReferenceMode = false;
    // 已由当前脚本写入过的 root 变量，即使值未知，也能安全作为循环 cache 的进场读取来源。
    private final Set<String> initializedRootVariables = new HashSet<>();

    public CodeContext(String className, String superClassName) {
        this.className = className;
        this.superClassName = superClassName;
    }

    @SuppressWarnings("CopyConstructorMissesField")
    public CodeContext(CodeContext parent) {
        this.className = parent.className;
        this.superClassName = parent.superClassName;
        this.definitions.addAll(parent.definitions);
        this.userFunctionOwners.putAll(parent.userFunctionOwners);
    }

    public void addDefinition(Definition definition) {
        definitions.add(definition);
    }

    public void addDefinitions(List<Definition> definitions) {
        this.definitions.addAll(definitions);
    }

    public void addLambdaDefinition(LambdaFunctionDefinition definition) {
        lambdaDefinitions.add(definition);
    }

    public List<LambdaFunctionDefinition> getLambdaDefinitions() {
        return lambdaDefinitions;
    }

    public void addAnonymousClassDefinition(AnonymousClassExpression expression) {
        anonymousClassDefinitions.add(expression);
    }

    public List<AnonymousClassExpression> getAnonymousClassDefinitions() {
        return anonymousClassDefinitions;
    }

    public int getAnonymousClassIndex() {
        return anonymousClassIndex;
    }

    public void incrementAnonymousClassIndex() {
        anonymousClassIndex++;
    }

    public int addCommandData(Object data) {
        int index = commandDataList.size();
        commandDataList.add(data);
        return index;
    }

    public List<Object> getCommandDataList() {
        return commandDataList;
    }

    public String getClassName() {
        return className;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public List<Definition> getDefinitions() {
        return definitions;
    }

    public int allocateLocalVar(Type type) {
        int slot = localVarIndex;
        String descriptor = type.getDescriptor();
        // 根据类型增加索引，double/long 占 2 个 slot
        switch (descriptor) {
            case "J":
            case "D":
                localVarIndex += 2;
                break;
            default:
                localVarIndex += 1;
                break;
        }
        return slot;
    }

    public int getLocalVarIndex() {
        return localVarIndex;
    }

    /**
     * 恢复局部变量索引，用于释放临时变量槽位
     */
    public void restoreLocalVarIndex(int savedIndex) {
        this.localVarIndex = savedIndex;
    }

    public Evaluator<ParseResult> getEvaluator(ParseResult result) {
        if (result instanceof Expression) {
            return ((Expression) result).getExpressionType().evaluator;
        } else if (result instanceof Statement) {
            return ((Statement) result).getStatementType().evaluator;
        }
        return null;
    }

    /**
     * 进入循环上下文
     * @param breakLabel break 跳转标签
     * @param continueLabel continue 跳转标签
     */
    public void enterLoop(Label breakLabel, Label continueLabel) {
        loopStack.push(new LoopContext(breakLabel, continueLabel));
    }

    /**
     * 退出循环上下文
     */
    public void exitLoop() {
        if (!loopStack.isEmpty()) {
            loopStack.pop();
        }
    }

    /**
     * 获取当前循环的 break 标签
     * @return break 标签，如果不在循环中则返回 null
     */
    public Label getCurrentBreakLabel() {
        return loopStack.isEmpty() ? null : loopStack.peek().getBreakLabel();
    }

    /**
     * 获取当前循环的 continue 标签
     * @return continue 标签，如果不在循环中则返回 null
     */
    public Label getCurrentContinueLabel() {
        return loopStack.isEmpty() ? null : loopStack.peek().getContinueLabel();
    }

    /**
     * 判断当前是否在循环中
     * @return 是否在循环中
     */
    public boolean isInLoop() {
        return !loopStack.isEmpty();
    }

    /**
     * 设置 environment 局部变量槽位索引
     * @param slot 局部变量槽位索引
     */
    public void setEnvironmentLocalSlot(int slot) {
        this.environmentLocalSlot = slot;
    }

    /**
     * 获取 environment 局部变量槽位索引
     * @return 局部变量槽位索引，-1 表示使用字段
     */
    public int getEnvironmentLocalSlot() {
        return environmentLocalSlot;
    }

    /**
     * 判断是否使用局部变量存储 environment
     * @return true 表示使用局部变量，false 表示使用字段
     */
    public boolean useLocalEnvironment() {
        return environmentLocalSlot >= 0;
    }

    /**
     * 设置 FunctionContextPool 局部变量槽位索引
     * @param slot 局部变量槽位索引
     */
    public void setPoolLocalSlot(int slot) {
        this.poolLocalSlot = slot;
    }

    /**
     * 获取 FunctionContextPool 局部变量槽位索引
     * @return 局部变量槽位索引，-1 表示未设置
     */
    public int getPoolLocalSlot() {
        return poolLocalSlot;
    }

    /**
     * 设置方法期望的返回类型
     * @param returnType 期望的返回类型（null 表示默认为 Object）
     */
    public void setExpectedReturnType(Class<?> returnType) {
        this.expectedReturnType = returnType;
    }

    /**
     * 获取方法期望的返回类型
     * @return 期望的返回类型，null 表示默认为 Object
     */
    public Class<?> getExpectedReturnType() {
        return expectedReturnType;
    }

    /**
     * 设置类型分析器
     * @param typeAnalyzer 类型分析器
     */
    public void setTypeAnalyzer(TypeAnalyzer typeAnalyzer) {
        this.typeAnalyzer = typeAnalyzer;
    }

    /**
     * 获取类型分析器
     * @return 类型分析器，可能为 null
     */
    public TypeAnalyzer getTypeAnalyzer() {
        return typeAnalyzer;
    }

    /**
     * 获取局部变量的类型
     * @param position 变量位置
     * @return 变量类型，如果未知则返回 OBJECT
     */
    public Type getVariableType(int position) {
        if (typeAnalyzer != null) {
            return typeAnalyzer.getVariableType(position);
        }
        return Type.OBJECT;
    }

    /**
     * 获取 root 变量类型
     * @param name 变量名
     * @return 变量类型，如果未知则返回 OBJECT
     */
    public Type getRootVariableType(String name) {
        if (typeAnalyzer != null) {
            return typeAnalyzer.getRootVariableType(name);
        }
        return Type.OBJECT;
    }

    /**
     * root 变量缓存槽位
     */
    public static class RootVariableCache {
        public final String name;
        public final Type type;
        public final int slot;

        public RootVariableCache(String name, Type type, int slot) {
            this.name = name;
            this.type = type;
            this.slot = slot;
        }
    }

    /**
     * 进入 root 变量缓存作用域
     * 缓存只用于循环内不可观察的纯计算段，退出前必须统一写回 Environment。
     */
    public void enterRootVariableCacheScope(Map<String, RootVariableCache> caches) {
        rootVariableCacheScopes.push(caches);
    }

    /**
     * 退出 root 变量缓存作用域
     */
    public void exitRootVariableCacheScope() {
        if (!rootVariableCacheScopes.isEmpty()) {
            rootVariableCacheScopes.pop();
        }
    }

    /**
     * 获取当前可见的 root 变量缓存
     */
    public RootVariableCache getRootVariableCache(String name) {
        for (Map<String, RootVariableCache> scope : rootVariableCacheScopes) {
            RootVariableCache cache = scope.get(name);
            if (cache != null) return cache;
        }
        return null;
    }

    public void recordRootConstantValue(String name, Object value) {
        if (value == null) {
            rootConstantValues.remove(name);
        } else {
            rootConstantValues.put(name, value);
        }
    }

    public Object getRootConstantValue(String name) {
        return rootConstantValues.get(name);
    }

    public void clearRootConstantValues() {
        rootConstantValues.clear();
    }

    public void enterRootConstantReferenceMode() {
        rootConstantReferenceMode = true;
    }

    public void exitRootConstantReferenceMode() {
        rootConstantReferenceMode = false;
    }

    public boolean isRootConstantReferenceMode() {
        return rootConstantReferenceMode;
    }

    public void recordRootVariableInitialized(String name) {
        initializedRootVariables.add(name);
    }

    public boolean isRootVariableInitialized(String name) {
        return initializedRootVariables.contains(name);
    }

    /**
     * 内联函数局部变量槽位
     */
    public static class InlineLocalVariable {
        public final Type type;
        public final int slot;

        public InlineLocalVariable(Type type, int slot) {
            this.type = type;
            this.slot = slot;
        }
    }

    /**
     * 进入内联函数局部变量作用域
     */
    public void enterInlineLocalVariableScope(Map<Integer, InlineLocalVariable> locals) {
        inlineLocalVariableScopes.push(locals);
    }

    /**
     * 退出内联函数局部变量作用域
     */
    public void exitInlineLocalVariableScope() {
        if (!inlineLocalVariableScopes.isEmpty()) {
            inlineLocalVariableScopes.pop();
        }
    }

    /**
     * 获取当前内联函数局部变量槽位
     */
    public InlineLocalVariable getInlineLocalVariable(int position) {
        for (Map<Integer, InlineLocalVariable> scope : inlineLocalVariableScopes) {
            InlineLocalVariable local = scope.get(position);
            if (local != null) return local;
        }
        return null;
    }

    /**
     * 添加预解析的扩展函数到常量池
     * @param dispatchTableIndex 派发表索引
     * @param targetClass 目标类型
     * @param overloadIndex 重载索引
     * @return 函数在常量池中的索引
     */
    public int addResolvedExtensionFunction(int dispatchTableIndex, Class<?> targetClass, int overloadIndex) {
        // 检查是否已存在相同的组合
        for (int i = 0; i < resolvedExtensionFunctions.size(); i++) {
            ResolvedExtFuncInfo info = resolvedExtensionFunctions.get(i);
            if (info.dispatchTableIndex == dispatchTableIndex && info.targetClass == targetClass && info.overloadIndex == overloadIndex) {
                return i;
            }
        }
        int index = resolvedExtensionFunctions.size();
        resolvedExtensionFunctions.add(new ResolvedExtFuncInfo(dispatchTableIndex, targetClass, overloadIndex));
        return index;
    }

    /**
     * 获取预解析的扩展函数常量池
     * @return 预解析函数信息列表
     */
    public List<ResolvedExtFuncInfo> getResolvedExtensionFunctions() {
        return resolvedExtensionFunctions;
    }

    /**
     * 添加延迟重载解析的 OverloadSet 到常量池
     * @param overloadSet 重载集合
     * @return 在常量池中的索引
     */
    public int addDeferredOverloadSet(OverloadSet overloadSet) {
        // 检查是否已存在
        int existing = deferredOverloadSets.indexOf(overloadSet);
        if (existing >= 0) {
            return existing;
        }
        int index = deferredOverloadSets.size();
        deferredOverloadSets.add(overloadSet);
        return index;
    }

    /**
     * 获取延迟重载解析的 OverloadSet 常量池
     * @return OverloadSet 列表
     */
    public List<OverloadSet> getDeferredOverloadSets() {
        return deferredOverloadSets;
    }

    /**
     * 注册用户定义函数（编译期直接引用优化）
     *
     * @param name       函数名
     * @param ownerClass 定义该函数静态字段的类的 JVM 内部名
     */
    public void registerUserFunction(String name, String ownerClass) {
        userFunctionOwners.put(name, ownerClass);
    }

    /**
     * 获取用户定义函数的所属类
     *
     * @param name 函数名
     * @return 所属类的 JVM 内部名，不存在则返回 null
     */
    public String getUserFunctionOwner(String name) {
        return userFunctionOwners.get(name);
    }

    /**
     * 启用 env-free 模式，局部变量存储在 JVM 局部变量而非 Environment
     *
     * @param localVarCount 脚本中的局部变量数量
     */
    public void enableEnvFreeMode(int localVarCount) {
        this.envFreeMode = true;
        this.varPosToJvmSlot = new int[localVarCount];
    }

    public boolean isEnvFreeMode() {
        return envFreeMode;
    }

    /**
     * 记录脚本变量位置到 JVM 槽位的映射
     */
    public void mapVarToJvmSlot(int varPosition, int jvmSlot) {
        varPosToJvmSlot[varPosition] = jvmSlot;
    }

    /**
     * 获取脚本变量对应的 JVM 局部变量槽位
     */
    public int getJvmSlot(int varPosition) {
        return varPosToJvmSlot[varPosition];
    }

    /**
     * 获取类的内部名称
     * @return 内部类名
     */
    public String getInternalName() {
        return className;
    }
}
