package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 错误传播表达式
 * expr? — 若 expr 为 null 或抛出异常，则从当前函数返回 null
 *
 * @author sky
 */
public class ErrorPropagationExpression extends Expression implements EnvironmentBoundaryExpression {

    private final ParseResult operand;

    public ErrorPropagationExpression(ParseResult operand) {
        super(ExpressionType.ERROR_PROPAGATION);
        this.operand = operand;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.ERROR_PROPAGATION;
    }

    public ParseResult getOperand() {
        return operand;
    }

    @Override
    public String toPseudoCode() {
        return operand.toPseudoCode() + "?";
    }
}
