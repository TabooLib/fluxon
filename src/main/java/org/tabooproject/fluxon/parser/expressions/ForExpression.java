package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.Collections;
import java.util.List;

/**
 * For 表达式
 */
public class ForExpression implements Expression {
    private final List<String> variables;
    private final ParseResult collection;
    private final ParseResult body;

    /**
     * 创建一个 For 表达式，支持多变量解构
     *
     * @param variables 循环变量名列表
     * @param collection 集合表达式
     * @param body 循环体
     */
    public ForExpression(List<String> variables, ParseResult collection, ParseResult body) {
        this.variables = variables;
        this.collection = collection;
        this.body = body;
    }

    /**
     * 创建一个 For 表达式，单变量形式
     * 为了兼容性保留
     *
     * @param variable 循环变量名
     * @param collection 集合表达式
     * @param body 循环体
     */
    public ForExpression(String variable, ParseResult collection, ParseResult body) {
        this.variables = Collections.singletonList(variable);
        this.collection = collection;
        this.body = body;
    }

    /**
     * 获取循环变量名列表
     *
     * @return 变量名列表
     */
    public List<String> getVariables() {
        return variables;
    }

    /**
     * 获取变量名（单个变量情况）
     * 为了兼容性保留
     *
     * @return 第一个变量名
     */
    public String getVariable() {
        return variables.get(0);
    }

    public ParseResult getCollection() {
        return collection;
    }

    public ParseResult getBody() {
        return body;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.FOR;
    }

    @Override
    public String toString() {
        return "For(" + variables + ", " + collection + ", " + body + ")";
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("for ");
        
        // 处理变量部分
        if (variables.size() == 1) {
            // 单个变量
            sb.append(variables.get(0));
        } else {
            // 多个变量（解构）
            sb.append("(");
            for (int i = 0; i < variables.size(); i++) {
                sb.append(variables.get(i));
                if (i < variables.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
        }
        
        sb.append(" in ").append(collection.toPseudoCode()).append(" ");

        // 处理循环体
        if (body instanceof org.tabooproject.fluxon.parser.statements.Block) {
            sb.append(body.toPseudoCode());
        } else {
            sb.append("{ ").append(body.toPseudoCode()).append(" }");
        }

        return sb.toString();
    }
}
