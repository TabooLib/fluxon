package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;

/**
 * 赋值表达式
 */
public class AssignExpression extends Expression {

    private final ParseResult target;  // 赋值目标（Identifier 或 IndexAccessExpression）
    private final Token operator;
    private final ParseResult value;
    private final int position;

    public AssignExpression(ParseResult target, Token operator, ParseResult value, int position) {
        super(ExpressionType.ASSIGNMENT);
        this.target = target;
        this.operator = operator;
        this.value = value;
        this.position = position;
    }

    /**
     * 获取赋值目标
     */
    public ParseResult getTarget() {
        return target;
    }

    /**
     * 获取变量名（兼容性方法，仅当 target 是 Identifier 时有效）
     */
    public String getName() {
        if (target instanceof Identifier) {
            return ((Identifier) target).getValue();
        }
        return null;
    }

    public Token getOperator() {
        return operator;
    }

    public ParseResult getValue() {
        return value;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.ASSIGNMENT;
    }

    @Override
    public String toString() {
        return "Assignment(" + target + " " + operator.getLexeme() + " " + value + ", position: " + position + ")";
    }

    @Override
    public String toPseudoCode() {
        return target.toPseudoCode() + " " + operator.getLexeme() + " " + value.toPseudoCode();
    }
}
