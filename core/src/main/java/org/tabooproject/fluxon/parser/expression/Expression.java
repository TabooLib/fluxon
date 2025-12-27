package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 表达式基础接口
 * 所有表达式类型的基础接口
 */
public abstract class Expression implements ParseResult {

    /**
     * 缓存的 evaluator 引用，在构造时初始化
     */
    private final Evaluator<ParseResult> evaluator;

    public Expression(ExpressionType type) {
        this.evaluator = type.evaluator;
    }

    /**
     * 获取表达式具体类型
     *
     * @return 表达式类型枚举值
     */
    abstract public ExpressionType getExpressionType();

    /**
     * 获取缓存的 evaluator，避免每次调用 getExpressionType().evaluator
     */
    public final Evaluator<ParseResult> getEvaluator() {
        return evaluator;
    }

    /**
     * 生成带缩进的伪代码表示
     *
     * @param indent 缩进级别
     * @return 伪代码字符串
     */
    public String toPseudoCode(int indent) {
        return "";
    }

    @Override
    public ResultType getType() {
        return ResultType.EXPRESSION;
    }
}