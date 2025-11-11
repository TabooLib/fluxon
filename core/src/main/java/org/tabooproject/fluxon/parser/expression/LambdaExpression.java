package org.tabooproject.fluxon.parser.expression;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.parser.CapturedVariable;
import org.tabooproject.fluxon.parser.ParseResult;

import java.util.*;

/**
 * Lambda 表达式
 * 表示匿名函数/闭包
 */
public class LambdaExpression implements Expression {

    private final LinkedHashMap<String, Integer> parameters;
    private final ParseResult body;
    private final Set<String> localVariables;
    private final List<CapturedVariable> capturedVariables; // 捕获的外层变量
    private final boolean isAsync;
    private final boolean isPrimarySync;

    public LambdaExpression(
            @NotNull LinkedHashMap<String, Integer> parameters,
            @NotNull ParseResult body,
            @NotNull Set<String> localVariables,
            @NotNull List<CapturedVariable> capturedVariables,
            boolean isAsync,
            boolean isPrimarySync
    ) {
        this.parameters = parameters;
        this.body = body;
        this.localVariables = localVariables;
        this.capturedVariables = Collections.unmodifiableList(capturedVariables);
        this.isAsync = isAsync;
        this.isPrimarySync = isPrimarySync;
    }

    public LinkedHashMap<String, Integer> getParameters() {
        return parameters;
    }

    public ParseResult getBody() {
        return body;
    }

    public Set<String> getLocalVariables() {
        return localVariables;
    }

    public List<CapturedVariable> getCapturedVariables() {
        return capturedVariables;
    }

    public boolean isAsync() {
        return isAsync;
    }

    public boolean isPrimarySync() {
        return isPrimarySync;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.LAMBDA;
    }

    @Override
    public String toString() {
        return "Lambda(" + parameters.keySet() + ", " + body + ")";
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        if (isAsync) {
            sb.append("async ");
        }
        if (isPrimarySync) {
            sb.append("sync ");
        }
        sb.append("lambda (");
        sb.append(String.join(", ", parameters.keySet()));
        sb.append(") -> ");
        sb.append(body.toPseudoCode());
        return sb.toString();
    }
}
