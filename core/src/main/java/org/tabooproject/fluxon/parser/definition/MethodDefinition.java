package org.tabooproject.fluxon.parser.definition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.ParseResult;

import java.util.List;
import java.util.Set;

/**
 * 匿名类方法定义
 * 用于 impl 表达式中的方法声明
 *
 * @author sky
 */
public class MethodDefinition implements Definition {

    private final String name;
    private final List<String> parameterNames;
    private final String returnType;
    private final ParseResult body;
    private final Set<String> localVariables;

    public MethodDefinition(
            @NotNull String name,
            @NotNull List<String> parameterNames,
            @Nullable String returnType,
            @NotNull ParseResult body,
            @NotNull Set<String> localVariables
    ) {
        this.name = name;
        this.parameterNames = parameterNames;
        this.returnType = returnType;
        this.body = body;
        this.localVariables = localVariables;
    }

    public String getName() {
        return name;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    @Nullable
    public String getReturnType() {
        return returnType;
    }

    public ParseResult getBody() {
        return body;
    }

    public Set<String> getLocalVariables() {
        return localVariables;
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("override ").append(name).append("(");
        sb.append(String.join(", ", parameterNames));
        sb.append(")");
        if (returnType != null) {
            sb.append(": ").append(returnType);
        }
        sb.append(" = ").append(body.toPseudoCode());
        return sb.toString();
    }

    @Override
    public String toString() {
        return "MethodDefinition{" +
                "name='" + name + '\'' +
                ", parameterNames=" + parameterNames +
                ", returnType='" + returnType + '\'' +
                ", body=" + body +
                ", localVariables=" + localVariables +
                '}';
    }
}
