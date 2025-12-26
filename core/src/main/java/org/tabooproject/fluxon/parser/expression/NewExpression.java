package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.Arrays;

/**
 * Java 对象构造表达式
 * 语法: new fully.qualified.ClassName(args)
 */
public class NewExpression implements Expression {

    private final String className;
    private final ParseResult[] arguments;

    public NewExpression(String className, ParseResult[] arguments) {
        this.className = className;
        this.arguments = arguments;
    }

    /**
     * 获取全限定类名
     */
    public String getClassName() {
        return className;
    }

    /**
     * 获取构造函数参数
     */
    public ParseResult[] getArguments() {
        return arguments;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.NEW;
    }

    @Override
    public String toString() {
        return "New(" + className + ", " + Arrays.toString(arguments) + ")";
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("new ").append(className).append("(");
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(arguments[i].toPseudoCode());
        }
        sb.append(")");
        return sb.toString();
    }
}
