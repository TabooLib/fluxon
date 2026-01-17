package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.Label;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.expression.AnonymousClassExpression;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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

    // 方法期望的返回类型（用于匿名类方法等场景）
    private Class<?> expectedReturnType = null;

    // 循环标签栈管理
    private final Stack<LoopContext> loopStack = new Stack<>();

    // Command 解析数据（运行时通过 index 访问）
    private final List<Object> commandDataList = new ArrayList<>();

    public CodeContext(String className, String superClassName) {
        this.className = className;
        this.superClassName = superClassName;
    }

    @SuppressWarnings("CopyConstructorMissesField")
    public CodeContext(CodeContext parent) {
        this.className = parent.className;
        this.superClassName = parent.superClassName;
        this.definitions.addAll(parent.definitions);
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
        String descriptor = type.getDescriptor();
        // 根据类型增加索引
        switch (descriptor) {
            case "J":
            case "D":
                localVarIndex += 2;
                break;
            default:
                localVarIndex += 1;
                break;
        }
        return localVarIndex;
    }

    public int getLocalVarIndex() {
        return localVarIndex;
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
}
