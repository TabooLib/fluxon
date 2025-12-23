package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.LinkedHashMap;

/**
 * 解构赋值表达式
 * 语法: (var1, var2, ...) = expression
 * 将右侧表达式的值解构并赋值给左侧的多个变量
 */
public class DestructuringAssignExpression implements Expression {

    // 变量名到局部变量位置的映射
    private final LinkedHashMap<String, Integer> variables;
    // 右侧表达式（被解构的值）
    private final ParseResult value;

    /**
     * 创建解构赋值表达式
     *
     * @param variables 变量名到局部变量位置的映射
     * @param value     右侧表达式
     */
    public DestructuringAssignExpression(LinkedHashMap<String, Integer> variables, ParseResult value) {
        this.variables = variables;
        this.value = value;
    }

    /**
     * 获取变量名到位置的映射
     */
    public LinkedHashMap<String, Integer> getVariables() {
        return variables;
    }

    /**
     * 获取右侧表达式
     */
    public ParseResult getValue() {
        return value;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.DESTRUCTURING_ASSIGNMENT;
    }

    @Override
    public String toString() {
        return "DestructuringAssign(" + variables.keySet() + " = " + value + ")";
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(String.join(", ", variables.keySet()));
        sb.append(") = ");
        sb.append(value.toPseudoCode());
        return sb.toString();
    }
}
