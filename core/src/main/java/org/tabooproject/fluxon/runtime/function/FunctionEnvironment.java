package org.tabooproject.fluxon.runtime.function;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Arrays;

public class FunctionEnvironment {

    @SuppressWarnings({"DataFlowIssue"})
    public static void init(FluxonRuntime runtime) {
        // 获取环境对象
        runtime.registerFunction("env", 0, context -> context.setReturnRef(context.getEnvironment()));
        runtime.getExportRegistry().registerClass(Environment.class);
        // 变量
        runtime.registerExtensionFunction(Environment.class, "localVariables", 0, context -> {
            @Nullable Object[] localRefs = context.getTarget().getLocalRefs();
            context.setReturnRef(localRefs != null ? Arrays.asList(localRefs) : null);
        });
        runtime.registerExtensionFunction(Environment.class, "localVariableNames", 0, context -> {
            @Nullable String[] localVariableNames = context.getTarget().getLocalVariableNames();
            context.setReturnRef(localVariableNames != null ? Arrays.asList(localVariableNames) : null);
        });
    }
}
