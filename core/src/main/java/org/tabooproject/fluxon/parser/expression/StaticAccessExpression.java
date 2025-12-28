package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.Arrays;

/**
 * 静态成员访问表达式
 * 语法: static fully.qualified.ClassName.member 或 static fully.qualified.ClassName.method(args)
 */
public class StaticAccessExpression extends Expression {

    private final String className;
    private final String memberName;
    private final ParseResult[] arguments;
    private final boolean isMethodCall;

    public StaticAccessExpression(String className, String memberName, ParseResult[] arguments, boolean isMethodCall) {
        super(ExpressionType.STATIC_ACCESS);
        this.className = className;
        this.memberName = memberName;
        this.arguments = arguments;
        this.isMethodCall = isMethodCall;
    }

    /**
     * 获取全限定类名
     */
    public String getClassName() {
        return className;
    }

    /**
     * 获取成员名（方法名或字段名）
     */
    public String getMemberName() {
        return memberName;
    }

    /**
     * 获取方法参数（如果是方法调用）
     */
    public ParseResult[] getArguments() {
        return arguments;
    }

    /**
     * 是否为方法调用
     */
    public boolean isMethodCall() {
        return isMethodCall;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.STATIC_ACCESS;
    }

    @Override
    public String toString() {
        if (isMethodCall) {
            return "StaticAccess(" + className + "." + memberName + ", " + Arrays.toString(arguments) + ")";
        } else {
            return "StaticAccess(" + className + "." + memberName + ")";
        }
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("static ").append(className).append(".").append(memberName);
        if (isMethodCall) {
            sb.append("(");
            for (int i = 0; i < arguments.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(arguments[i].toPseudoCode());
            }
            sb.append(")");
        }
        return sb.toString();
    }
}
