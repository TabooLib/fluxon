package org.tabooproject.fluxon.interpreter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.Symbolic;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.Collections;
import java.util.List;

/**
 * 用户自定义的函数
 * 解释执行环境中的函数定义
 */
public class UserFunction implements Function, Symbolic {

    private final SymbolFunction symbolInfo;

    @NotNull
    private final FunctionDefinition definition;
    @NotNull
    private final Interpreter interpreter;

    public UserFunction(@NotNull FunctionDefinition definition, @NotNull Interpreter interpreter) {
        this.symbolInfo = new SymbolFunction(null, definition.getName(), definition.getParameters().size());
        this.definition = definition;
        this.interpreter = interpreter;
    }

    @NotNull
    @Override
    public String getName() {
        return definition.getName();
    }

    @Nullable
    @Override
    public String getNamespace() {
        return null;
    }

    @NotNull
    @Override
    public List<Integer> getParameterCounts() {
        return Collections.singletonList(definition.getParameters().size());
    }

    @Override
    public int getMaxParameterCount() {
        return Collections.max(getParameterCounts());
    }

    @Override
    public boolean isAsync() {
        return definition.isAsync();
    }

    @Override
    public boolean isPrimarySync() {
        return definition.isPrimarySync();
    }

    @Override
    public void call(@NotNull final FunctionContext<?> context) {
        Environment functionEnv = Intrinsics.bindFunctionParameters(
                interpreter.getEnvironment(),
                definition.getParameters(),
                context,
                definition.getLocalVariables().size()
        );
        if (definition.getBody().getType() != null && definition.getBody().getType() != ParseResult.ResultType.STATEMENT) {
            interpreter.consumeCostStep();
        }
        try {
            Object result = interpreter.executeWithEnvironment(definition.getBody(), functionEnv);
            context.setReturnRef(result);
        } catch (ReturnValue returnValue) {
            context.setReturnRef(returnValue.getValue());
        }
    }

    @Override
    public SymbolFunction getInfo() {
        return symbolInfo;
    }

    @NotNull
    public FunctionDefinition getDefinition() {
        return definition;
    }

    @NotNull
    public Interpreter getInterpreter() {
        return interpreter;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return definition.getAnnotations();
    }

    @Override
    public String toString() {
        return "UserFunction{" +
                "definition=" + definition +
                '}';
    }
}
