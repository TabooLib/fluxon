package org.tabooproject.fluxon.interpreter;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.parser.definition.Definitions;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.Collections;
import java.util.List;

/**
 * 用户自定义的函数
 * 解释执行环境中的函数定义
 */
public class UserFunction implements Function {
    
    private final Definitions.FunctionDefinition definition;
    private final Environment closure;
    private final Interpreter interpreter;

    public UserFunction(Definitions.FunctionDefinition definition, Environment closure, Interpreter interpreter) {
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
    public Object call(Object[] args) {
        // 使用 Operations.bindFunctionParameters 统一参数绑定逻辑
        String[] parameters = definition.getParameters().toArray(new String[0]);
        Environment functionEnv = Intrinsics.bindFunctionParameters(closure, parameters, args);
        try {
            // 执行函数体
            return interpreter.executeWithEnvironment(definition.getBody(), functionEnv);
        } catch (ReturnValue returnValue) {
            // 捕获返回值
            return returnValue.getValue();
        }
    }

    /**
     * 获取函数定义
     * @return 函数定义
     */
    public Definitions.FunctionDefinition getDefinition() {
        return definition;
    }
}