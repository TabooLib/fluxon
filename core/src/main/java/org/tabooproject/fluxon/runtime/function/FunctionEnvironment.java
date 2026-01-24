package org.tabooproject.fluxon.runtime.function;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.util.Arrays;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class FunctionEnvironment {

    @SuppressWarnings({"DataFlowIssue"})
    public static void init(FluxonRuntime runtime) {
        runtime.registerFunction("env", returns(Type.OBJECT).noParams(), context -> context.setReturnRef(context.getEnvironment()));
        runtime.getExportRegistry().registerClass(Environment.class);
        runtime.registerExtension(Environment.class)
                .function("localVariables", returns(Type.OBJECT).noParams(), context -> {
                    @Nullable Object[] localRefs = context.getTarget().getLocalRefs();
                    context.setReturnRef(localRefs != null ? Arrays.asList(localRefs) : null);
                })
                .function("localVariableNames", returns(Type.OBJECT).noParams(), context -> {
                    @Nullable String[] localVariableNames = context.getTarget().getLocalVariableNames();
                    context.setReturnRef(localVariableNames != null ? Arrays.asList(localVariableNames) : null);
                });
    }
}
