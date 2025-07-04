package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Objects;

public class ExtensionObject {

    public static void init(FluxonRuntime runtime) {
        runtime.registerExtensionFunction(Object.class, "toString", 0, (target, args) -> Objects.toString(target));
    }
}
