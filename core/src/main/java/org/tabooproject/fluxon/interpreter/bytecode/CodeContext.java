package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.Label;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.Definition;
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

    // 局部变量表
    private int localVarIndex = 0;

    // 循环标签栈管理
    private final Stack<LoopContext> loopStack = new Stack<>();

    /**
     * 循环上下文，包含 break 和 continue 的跳转标签
     */
    public static class LoopContext {
        private final Label breakLabel;
        private final Label continueLabel;

        public LoopContext(Label breakLabel, Label continueLabel) {
            this.breakLabel = breakLabel;
            this.continueLabel = continueLabel;
        }

        public Label getBreakLabel() {
            return breakLabel;
        }

        public Label getContinueLabel() {
            return continueLabel;
        }
    }

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
}
