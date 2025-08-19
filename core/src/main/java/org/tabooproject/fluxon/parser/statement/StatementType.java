package org.tabooproject.fluxon.parser.statement;

import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.stmt.*;
import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 语句类型枚举
 * 用于区分不同类型的语句
 */
public enum StatementType {

    // 表达式语句
    EXPRESSION_STATEMENT(new ExprStmtEvaluator()),
    
    // 代码块
    BLOCK(new BlockEvaluator()),

    // 跳出语句
    BREAK(new BreakEvaluator()),

    // 继续语句
    CONTINUE(new ContinueEvaluator()),
    
    // 返回语句
    RETURN(new ReturnEvaluator());

    public final Evaluator<ParseResult> evaluator;

    @SuppressWarnings("unchecked")
    StatementType(Evaluator<?> evaluator) {
        this.evaluator = (Evaluator<ParseResult>) evaluator;
    }
} 