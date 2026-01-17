package org.tabooproject.fluxon.parser.expression;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.MethodDefinition;

import java.util.List;

/**
 * 匿名类表达式
 * 语法: impl : Type1(args), Type2, Type3 { methods }
 *
 * @author sky
 */
public class AnonymousClassExpression extends Expression {

    private final String superClass;           // 父类全限定名（可为 null，默认 Object）
    private final List<ParseResult> superArgs; // 父类构造参数
    private final List<String> interfaces;     // 接口列表
    private final List<MethodDefinition> methods; // 方法定义

    public AnonymousClassExpression(
            @Nullable String superClass,
            @Nullable List<ParseResult> superArgs,
            @NotNull List<String> interfaces,
            @NotNull List<MethodDefinition> methods
    ) {
        super(ExpressionType.ANONYMOUS_CLASS);
        this.superClass = superClass;
        this.superArgs = superArgs;
        this.interfaces = interfaces;
        this.methods = methods;
    }

    /**
     * 获取父类全限定名
     * @return 父类名，null 表示默认继承 Object
     */
    @Nullable
    public String getSuperClass() {
        return superClass;
    }

    /**
     * 获取父类构造函数参数
     */
    @Nullable
    public List<ParseResult> getSuperArgs() {
        return superArgs;
    }

    /**
     * 获取实现的接口列表
     */
    @NotNull
    public List<String> getInterfaces() {
        return interfaces;
    }

    /**
     * 获取方法定义列表
     */
    @NotNull
    public List<MethodDefinition> getMethods() {
        return methods;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.ANONYMOUS_CLASS;
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("impl : ");
        // 父类
        if (superClass != null) {
            sb.append(superClass);
            if (superArgs != null && !superArgs.isEmpty()) {
                sb.append("(");
                for (int i = 0; i < superArgs.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(superArgs.get(i).toPseudoCode());
                }
                sb.append(")");
            }
            if (!interfaces.isEmpty()) {
                sb.append(", ");
            }
        }
        // 接口
        for (int i = 0; i < interfaces.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(interfaces.get(i));
        }
        sb.append(" {\n");
        // 方法
        for (MethodDefinition method : methods) {
            sb.append("    ").append(method.toPseudoCode()).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "AnonymousClassExpression{" +
                "superClass='" + superClass + '\'' +
                ", superArgs=" + superArgs +
                ", interfaces=" + interfaces +
                ", methods=" + methods +
                '}';
    }
}
