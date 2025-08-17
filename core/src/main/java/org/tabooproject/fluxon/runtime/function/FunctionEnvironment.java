package org.tabooproject.fluxon.runtime.function;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.FunctionContext;

import java.util.Arrays;

public class FunctionEnvironment {

    @SuppressWarnings({"DataFlowIssue"})
    public static void init(FluxonRuntime runtime) {
        // 获取环境对象
        runtime.registerFunction("env", 0, FunctionContext::getEnvironment);

        // 基本属性
        runtime.registerExtensionFunction(Environment.class, "id", 0, (context) -> context.getTarget().getId());
        runtime.registerExtensionFunction(Environment.class, "level", 0, (context) -> context.getTarget().getLevel());
        runtime.registerExtensionFunction(Environment.class, "parent", 0, (context) -> context.getTarget().getParent());
        runtime.registerExtensionFunction(Environment.class, "root", 0, (context) -> context.getTarget().getRoot());

        // 函数
        runtime.registerExtensionFunction(Environment.class, "functions", 0, (context) -> context.getTarget().getRootFunctions());
        runtime.registerExtensionFunction(Environment.class, "extensionFunctions", 0, (context) -> context.getTarget().getRootExtensionFunctions());

        // 变量
        runtime.registerExtensionFunction(Environment.class, "variables", 0, (context) -> context.getTarget().getRootVariables());
        runtime.registerExtensionFunction(Environment.class, "localVariables", 0, context -> {
            @Nullable Object[] localVariables = context.getTarget().getLocalVariables();
            return localVariables != null ? Arrays.asList(localVariables) : null;
        });
        runtime.registerExtensionFunction(Environment.class, "localVariableNames", 0, (context) -> {
            @Nullable String[] localVariableNames = context.getTarget().getLocalVariableNames();
            return localVariableNames != null ? Arrays.asList(localVariableNames) : null;
        });
    }
}
