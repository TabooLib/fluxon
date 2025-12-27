package org.tabooproject.fluxon.parser.statement;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 表达式语句
 * 表示一个表达式作为语句使用
 * <p>
 * 所有 Block 里的语句，都会基于 ExprStmt 封装
 * 例如：
 * <code>
 *  if true { ... } <--- 这里的每一行都会被解析为 ExprStmt，并封装为 Block
 * </code>
 */
public class ExpressionStatement extends Statement {
    private final ParseResult expression;

    public ExpressionStatement(ParseResult expression) {
        super(StatementType.EXPRESSION_STATEMENT);
        this.expression = expression;
    }

    public ParseResult getExpression() {
        return expression;
    }

    @Override
    public StatementType getStatementType() {
        return StatementType.EXPRESSION_STATEMENT;
    }

    @Override
    public String toString() {
        return "ExpressionStatement(" + expression + ")";
    }

    @Override
    public String toPseudoCode() {
        return expression.toPseudoCode();
    }
}
