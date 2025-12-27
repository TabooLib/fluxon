package org.tabooproject.fluxon.parser.statement;

import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 语句接口
 * 所有语句类型的基础接口
 */
public abstract class Statement implements ParseResult {

    /**
     * 缓存的 evaluator 引用，在构造时初始化
     */
    private final Evaluator<ParseResult> evaluator;

    /**
     * 子类构造时必须传入对应的 StatementType
     */
    protected Statement(StatementType type) {
        this.evaluator = type.evaluator;
    }

    /**
     * 获取语句具体类型
     * 
     * @return 语句类型枚举值
     */
    abstract public StatementType getStatementType();

    /**
     * 获取缓存的 evaluator，避免每次调用 getStatementType().evaluator
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
        return "/* 未实现的语句 */";
    }

    @Override
    public ResultType getType() {
        return ResultType.STATEMENT;
    }
}