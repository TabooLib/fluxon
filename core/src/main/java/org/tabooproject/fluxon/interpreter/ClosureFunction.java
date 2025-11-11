package org.tabooproject.fluxon.interpreter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.expression.LambdaExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.Symbolic;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.Collections;
import java.util.List;

/**
 * 闭包函数
 * 表示运行时的 lambda 表达式，捕获定义时的环境
 */
public class ClosureFunction implements Function, Symbolic {

    private final SymbolFunction symbolInfo;
    
    @NotNull
    private final LambdaExpression definition;
    @NotNull
    private final Interpreter interpreter;
    @NotNull
    private final Environment capturedEnvironment;

    public ClosureFunction(
            @NotNull LambdaExpression definition,
            @NotNull Interpreter interpreter,
            @NotNull Environment capturedEnvironment
    ) {
        this.symbolInfo = new SymbolFunction(null, "<lambda>", definition.getParameters().size());
        this.definition = definition;
        this.interpreter = interpreter;
        this.capturedEnvironment = capturedEnvironment;
    }

    @NotNull
    @Override
    public String getName() {
        return "<lambda>";
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
        return definition.getParameters().size();
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
    public Object call(@NotNull final FunctionContext<?> context) {
        // 使用捕获的环境作为父环境，绑定参数
        Environment functionEnv = Intrinsics.bindFunctionParameters(
                capturedEnvironment,
                definition.getParameters(),
                context.getArguments(),
                definition.getLocalVariables().size()
        );
        try {
            // 执行 lambda 体
            return interpreter.executeWithEnvironment(definition.getBody(), functionEnv);
        } catch (ReturnValue returnValue) {
            // 捕获返回值
            return returnValue.getValue();
        }
    }

    @Override
    public SymbolFunction getInfo() {
        return symbolInfo;
    }

    @NotNull
    public LambdaExpression getDefinition() {
        return definition;
    }

    @NotNull
    public Interpreter getInterpreter() {
        return interpreter;
    }

    @NotNull
    public Environment getCapturedEnvironment() {
        return capturedEnvironment;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "ClosureFunction{" +
                "parameters=" + definition.getParameters().keySet() +
                '}';
    }
}
