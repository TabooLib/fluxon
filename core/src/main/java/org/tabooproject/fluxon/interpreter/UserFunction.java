package org.tabooproject.fluxon.interpreter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    public Object call(@NotNull final FunctionContext<?> context) {
        // 使用 Operations.bindFunctionParameters 统一参数绑定逻辑
        Environment functionEnv = Intrinsics.bindFunctionParameters(
                interpreter.getEnvironment(),
                definition.getParameters(),
                context.getArguments(),
                definition.getLocalVariables().size()
        );
        try {
            // 执行函数体
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
    public FunctionDefinition getDefinition() {
        return definition;
    }

    @NotNull
    public Interpreter getInterpreter() {
        return interpreter;
    }

    /**
     * 获取函数的注解列表
     *
     * @return 注解列表
     */
    public List<Annotation> getAnnotations() {
        return definition.getAnnotations();
    }
}