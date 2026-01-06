package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 上下文调用表达式
 * 表示形如 "text" :: replace("a", "b") 或 "text" :: { replace("a", "b"); length } 的表达式
 * 支持安全上下文调用操作符 ?:: 用于 null 短路
 */
public class ContextCallExpression extends Expression {

    private final ParseResult target;
    private final ParseResult context;
    private final boolean safe;

    /**
     * 构造函数
     *
     * @param target  目标表达式（:: 左侧的值）
     * @param context 上下文表达式（:: 右侧的表达式或块）
     */
    public ContextCallExpression(ParseResult target, ParseResult context) {
        this(target, context, false);
    }

    /**
     * 构造函数
     *
     * @param target  目标表达式（:: 或 ?:: 左侧的值）
     * @param context 上下文表达式（:: 或 ?:: 右侧的表达式或块）
     * @param safe    是否为安全调用（?::）
     */
    public ContextCallExpression(ParseResult target, ParseResult context, boolean safe) {
        super(ExpressionType.CONTEXT_CALL);
        this.target = target;
        this.context = context;
        this.safe = safe;
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
     * 是否为安全调用（?::）
     * 当 target 为 null 时，安全调用会返回 null 而不是执行上下文表达式
     */
    public boolean isSafe() {
        return safe;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.CONTEXT_CALL;
    }

    @Override
    public String toString() {
        String op = safe ? " ?:: " : " :: ";
        return "ContextCall{" +
                "target=" + target +
                op +
                "context=" + context +
                '}';
    }

    @Override
    public String toPseudoCode() {
        String op = safe ? " ?:: " : " :: ";
        return target.toPseudoCode() + op + context.toPseudoCode();
    }
}