package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.util.Map;
import java.util.Objects;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class ExtensionMapEntry {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Map.Entry.class)
                .function("key", returns(Type.OBJECT).noParams(), context -> {
                    Map.Entry<Object, Object> entry = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(entry.getKey());
                })
                .function("value", returns(Type.OBJECT).noParams(), context -> {
                    Map.Entry<Object, Object> entry = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(entry.getValue());
                });
    }
}
