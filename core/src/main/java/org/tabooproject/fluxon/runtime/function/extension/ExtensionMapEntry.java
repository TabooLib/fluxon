package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Map;
import java.util.Objects;

public class ExtensionMapEntry {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Map.Entry.class)
                .function("key", 0, (context) -> {
                    Map.Entry<Object, Object> entry = Objects.requireNonNull(context.getTarget());
                    return entry.getKey();
                })
                .function("value", 0, (context) -> {
                    Map.Entry<Object, Object> entry = Objects.requireNonNull(context.getTarget());
                    return entry.getValue();
                });
    }
}