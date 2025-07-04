package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ExtensionList {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtensionFunction(List.class, "add", 1, (target, args) -> ((List<Object>) Objects.requireNonNull(target)).add(args[0]));
    }
}
