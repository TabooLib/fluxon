package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Lambda 表达式
 */
public class LambdaExpression extends Expression {

    private final String name;
    private final LinkedHashMap<String, Integer> parameters;
    private final ParseResult body;
    private final Set<String> localVariables;

    public LambdaExpression(String name, LinkedHashMap<String, Integer> parameters, ParseResult body, Set<String> localVariables) {
        super(ExpressionType.LAMBDA);
        this.name = name;
        this.parameters = parameters;
        this.body = body;
        this.localVariables = localVariables;
    }

    public String getName() {
        return name;
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

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.LAMBDA;
    }

    @Override
    public String toPseudoCode() {
        StringBuilder builder = new StringBuilder();
        builder.append("|");
        int index = 0;
        for (String param : parameters.keySet()) {
            if (index++ > 0) {
                builder.append(", ");
            }
            builder.append(param);
        }
        builder.append("| ");
        builder.append(body.toPseudoCode());
        return builder.toString();
    }

    /**
     * 将当前表达式转换为 FunctionDefinition，供解释或字节码生成复用
     */
    public FunctionDefinition toFunctionDefinition(String ownerClassName) {
        return new LambdaFunctionDefinition(
                name,
                parameters,
                body,
                false,
                false,
                Collections.emptyList(),
                localVariables,
                ownerClassName
        );
    }
}
