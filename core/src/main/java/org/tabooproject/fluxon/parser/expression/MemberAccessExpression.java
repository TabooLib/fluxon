package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 成员访问表达式
 * 表示形如 obj.field 或 obj.method(args) 的反射访问表达式
 * 支持安全访问操作符 ?. 用于 null 短路
 */
public class MemberAccessExpression extends Expression {

    private final ParseResult target;
    private final String memberName;
    private final ParseResult[] args;
    private final boolean isMethodCall;
    private final boolean safe;

    /**
     * 构造字段访问表达式
     *
     * @param target     目标对象表达式
     * @param memberName 成员名称（字段名或方法名）
     */
    public MemberAccessExpression(ParseResult target, String memberName) {
        this(target, memberName, false);
    }

    /**
     * 构造字段访问表达式
     *
     * @param target     目标对象表达式
     * @param memberName 成员名称（字段名或方法名）
     * @param safe       是否为安全访问（?.）
     */
    public MemberAccessExpression(ParseResult target, String memberName, boolean safe) {
        super(ExpressionType.MEMBER_ACCESS);
        this.target = target;
        this.memberName = memberName;
        this.args = new ParseResult[0];
        this.isMethodCall = false;
        this.safe = safe;
    }

    /**
     * 构造方法调用表达式
     *
     * @param target     目标对象表达式
     * @param memberName 方法名
     * @param args       方法参数
     */
    public MemberAccessExpression(ParseResult target, String memberName, ParseResult[] args) {
        this(target, memberName, args, false);
    }

    /**
     * 构造方法调用表达式
     *
     * @param target     目标对象表达式
     * @param memberName 方法名
     * @param args       方法参数
     * @param safe       是否为安全访问（?.）
     */
    public MemberAccessExpression(ParseResult target, String memberName, ParseResult[] args, boolean safe) {
        super(ExpressionType.MEMBER_ACCESS);
        this.target = target;
        this.memberName = memberName;
        this.args = args;
        this.isMethodCall = true;
        this.safe = safe;
    }

    /**
     * 获取目标对象表达式
     */
    public ParseResult getTarget() {
        return target;
    }

    /**
     * 获取成员名称
     */
    public String getMemberName() {
        return memberName;
    }

    /**
     * 获取方法参数（如果是方法调用）
     */
    public ParseResult[] getArgs() {
        return args;
    }

    /**
     * 是否为方法调用
     */
    public boolean isMethodCall() {
        return isMethodCall;
    }

    /**
     * 是否为安全访问（?.）
     * 当 target 为 null 时，安全访问会返回 null 而不是抛出 NullPointerException
     */
    public boolean isSafe() {
        return safe;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.MEMBER_ACCESS;
    }

    @Override
    public String toString() {
        String op = safe ? "?." : ".";
        if (isMethodCall) {
            StringBuilder sb = new StringBuilder();
            sb.append("MemberAccess(").append(target).append(op).append(memberName).append("(");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(args[i]);
            }
            sb.append("))");
            return sb.toString();
        } else {
            return "MemberAccess(" + target + op + memberName + ")";
        }
    }

    @Override
    public String toPseudoCode() {
        String op = safe ? "?." : ".";
        if (isMethodCall) {
            StringBuilder sb = new StringBuilder();
            sb.append(target.toPseudoCode()).append(op).append(memberName).append("(");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(args[i].toPseudoCode());
            }
            sb.append(")");
            return sb.toString();
        } else {
            return target.toPseudoCode() + op + memberName;
        }
    }
}
