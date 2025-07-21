package org.tabooproject.fluxon.interpreter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.parser.definition.Definitions;
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
    private final Definitions.FunctionDefinition definition;
    private final Environment closure;
    private final Interpreter interpreter;

    public UserFunction(Definitions.FunctionDefinition definition, Environment closure, Interpreter interpreter) {
        this.symbolInfo = new SymbolFunction(definition.getName(), definition.getParameters().size());
        this.definition = definition;
        this.closure = closure;
        this.interpreter = interpreter;
    }

    @NotNull
    @Override
    public String getName() {
        return definition.getName();
    }

    @NotNull
    @Override
    public List<Integer> getParameterCounts() {
        return Collections.singletonList(definition.getParameters().size());
    }

    @Override
    public boolean isAsync() {
        return definition.isAsync();
    }

    @Override
    public Object call(@NotNull final FunctionContext context) {
        // 使用 Operations.bindFunctionParameters 统一参数绑定逻辑
        String[] parameters = definition.getParameters().toArray(new String[0]);
        Environment functionEnv = Intrinsics.bindFunctionParameters(closure, parameters, context.getArguments());
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

    public Definitions.FunctionDefinition getDefinition() {
        return definition;
    }

    public Environment getClosure() {
        return closure;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }
}