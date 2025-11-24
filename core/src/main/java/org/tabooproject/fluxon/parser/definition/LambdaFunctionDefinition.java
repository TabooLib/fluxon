package org.tabooproject.fluxon.parser.definition;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.parser.ParseResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * 代表由 lambda 生成的函数定义
 */
public class LambdaFunctionDefinition extends FunctionDefinition {

    private final String ownerClassName;

    public LambdaFunctionDefinition(
            String name,
            @NotNull LinkedHashMap<String, Integer> parameters,
            @NotNull ParseResult body,
            boolean isAsync,
            boolean isPrimarySync,
            @NotNull List<Annotation> annotations,
            @NotNull Set<String> localVariables,
            String ownerClassName
    ) {
        super(name, parameters, body, isAsync, isPrimarySync, annotations, localVariables, false);
        this.ownerClassName = ownerClassName;
    }

    public String getOwnerClassName() {
        return ownerClassName;
    }
}

