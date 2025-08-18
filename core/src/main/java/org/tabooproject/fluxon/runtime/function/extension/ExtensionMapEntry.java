package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Map;
import java.util.Objects;

public class ExtensionMapEntry {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtensionFunction(Map.Entry.class, "key", 2, (context) -> {
            Map.Entry<Object, Object> entry = Objects.requireNonNull(context.getTarget());
            return entry.getKey();
        });
        runtime.registerExtensionFunction(Map.Entry.class, "value", 2, (context) -> {
            Map.Entry<Object, Object> entry = Objects.requireNonNull(context.getTarget());
            return entry.getValue();
        });
    }
}