package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 上下文调用表达式
 * 表示形如 "text" :: replace("a", "b") 或 "text" :: { replace("a", "b"); length } 的表达式
 */
public class ContextCall implements Expression {

    private final ParseResult target;
    private final ParseResult context;
    private final int localVariables;

    /**
     * 构造函数
     *
     * @param target  目标表达式（:: 左侧的值）
     * @param context 上下文表达式（:: 右侧的表达式或块）
     */
    public ContextCall(ParseResult target, ParseResult context, int localVariables) {
        this.target = target;
        this.context = context;
        this.localVariables = localVariables;
    }

    /**
     * 获取目标表达式
     *
     * @return 目标表达式
     */
    public ParseResult getTarget() {
        return target;
    }

    /**
     * 获取上下文表达式
     *
     * @return 上下文表达式
     */
    public ParseResult getContext() {
        return context;
    }

    /**
     * 获取本地变量数量
     */
    public int getLocalVariables() {
        return localVariables;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.CONTEXT_CALL;
    }

    @Override
    public String toString() {
        return "ContextCall{" +
                "target=" + target +
                ", context=" + context +
                '}';
    }

    @Override
    public String toPseudoCode() {
        return target.toPseudoCode() + " :: " + context.toPseudoCode();
    }
} 