package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Map;
import java.util.Objects;

public class ExtensionMapEntry {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionMapEntry.class);
    }

    @FluxonFunction(value = "key", target = Map.Entry.class)
    public static Object key(Map.Entry<?, ?> entry) {
        return Objects.requireNonNull(entry).getKey();
    }

    @FluxonFunction(value = "value", target = Map.Entry.class)
    public static Object value(Map.Entry<?, ?> entry) {
        return Objects.requireNonNull(entry).getValue();
    }
}
