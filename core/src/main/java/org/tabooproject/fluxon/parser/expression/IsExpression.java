package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;

/**
 * Is 类型检查表达式
 * <p>
 * 用于运行时类型检查，类似 Java 的 instanceof。
 * 右侧的类型在解析期就被解析并缓存为 Class<?> 对象。
 */
public class IsExpression extends Expression {
    private final ParseResult left;
    private final Token operator;
    private final String typeLiteral;
    private final Class<?> targetClass;

    public IsExpression(ParseResult left, Token operator, String typeLiteral, Class<?> targetClass) {
        super(ExpressionType.IS_CHECK);
        this.left = left;
        this.operator = operator;
        this.typeLiteral = typeLiteral;
        this.targetClass = targetClass;
    }

    public ParseResult getLeft() {
        return left;
    }

    public Token getOperator() {
        return operator;
    }

    public String getTypeLiteral() {
        return typeLiteral;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.IS_CHECK;
    }

    @Override
    public String toString() {
        return "Is(" + left + " is " + typeLiteral + ")";
    }

    @Override
    public String toPseudoCode() {
        return left.toPseudoCode() + " is " + typeLiteral;
    }
}
