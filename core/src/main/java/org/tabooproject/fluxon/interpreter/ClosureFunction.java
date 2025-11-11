package org.tabooproject.fluxon.interpreter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.CapturedVariable;
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
 * 表示运行时的 lambda 表达式，捕获定义时的变量值
 */
public class ClosureFunction implements Function, Symbolic {

    private final SymbolFunction symbolInfo;

    @NotNull
    private final LambdaExpression definition;
    @NotNull
    private final Interpreter interpreter;
    @NotNull
    private final Object[] capturedValues; // 捕获的变量值快照
    @NotNull
    private final List<CapturedVariable> capturedVariables;

    public ClosureFunction(
            @NotNull LambdaExpression definition,
            @NotNull Interpreter interpreter,
            @NotNull Environment currentEnvironment
    ) {
        this.symbolInfo = new SymbolFunction(null, "<lambda>", definition.getParameters().size());
        this.definition = definition;
        this.interpreter = interpreter;
        this.capturedVariables = definition.getCapturedVariables();
        this.capturedValues = new Object[capturedVariables.size()];
        for (int i = 0; i < capturedVariables.size(); i++) {
            CapturedVariable captured = capturedVariables.get(i);
            capturedValues[i] = currentEnvironment.get(captured.getName(), captured.getSourceIndex());
        }
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
        // 绑定参数并准备 lambda 局部环境
        Environment functionEnv = Intrinsics.bindFunctionParameters(
                context.getEnvironment(),
                definition.getParameters(),
                context.getArguments(),
                definition.getLocalVariables().size()
        );
        // 回填捕获变量
        for (int i = 0; i < capturedVariables.size(); i++) {
            CapturedVariable captured = capturedVariables.get(i);
            functionEnv.assign(captured.getName(), capturedValues[i], captured.getLambdaIndex());
        }
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
