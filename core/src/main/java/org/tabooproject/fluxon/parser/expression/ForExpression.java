package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.LinkedHashMap;

/**
 * For 表达式
 */
public class ForExpression extends Expression {
    private final LinkedHashMap<String, Integer> variables;
    private final ParseResult collection;
    private final ParseResult body;
    private final int localVariables;

    /**
     * 创建一个 For 表达式，支持多变量解构
     *
     * @param variables 循环变量名列表
     * @param collection 集合表达式
     * @param body 循环体
     */
    public ForExpression(LinkedHashMap<String, Integer> variables, ParseResult collection, ParseResult body, int localVariables) {
        super(ExpressionType.FOR);
        this.variables = variables;
        this.collection = collection;
        this.body = body;
        this.localVariables = localVariables;
    }

    /**
     * 获取循环变量名列表
     *
     * @return 变量名列表
     */
    public LinkedHashMap<String, Integer> getVariables() {
        return variables;
    }

    public ParseResult getCollection() {
        return collection;
    }

    public ParseResult getBody() {
        return body;
    }

    public int getLocalVariables() {
        return localVariables;
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

        // 变量名
        sb.append("(");
        sb.append(String.join(", ", variables.keySet()));
        sb.append(")");

        sb.append(" in ").append(collection.toPseudoCode()).append(" ");

        // 处理循环体
        if (body instanceof org.tabooproject.fluxon.parser.statement.Block) {
            sb.append(body.toPseudoCode());
        } else {
            sb.append("{ ").append(body.toPseudoCode()).append(" }");
        }

        return sb.toString();
    }
}
