package org.tabooproject.fluxon.runtime.function;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class FunctionEnvironment {

    @SuppressWarnings({"DataFlowIssue"})
    public static void init(FluxonRuntime runtime) {
        runtime.registerFunction("env", returns(Type.fromClass(Environment.class)).noParams(), context -> context.setReturnRef(context.getEnvironment()));
        runtime.getExportRegistry().registerClass(Environment.class);
        runtime.registerExtension(Environment.class)
                .function("localVariables", returns(Type.fromClass(List.class)).noParams(), context -> {
                    @Nullable Object[] localRefs = context.getTarget().getLocalRefs();
                    context.setReturnRef(localRefs != null ? Arrays.asList(localRefs) : null);
                })
                .function("localVariableNames", returns(Type.fromClass(List.class)).noParams(), context -> {
                    @Nullable String[] localVariableNames = context.getTarget().getLocalVariableNames();
                    context.setReturnRef(localVariableNames != null ? Arrays.asList(localVariableNames) : null);
                });
    }
}
