package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Objects;

public class ExtensionObject {

    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Object.class)
                .function("toString", 0, context -> context.setReturnRef(Objects.toString(context.getTarget())))
                .function("hashCode", 0, context -> context.setReturnRef(context.getTarget() != null ? context.getTarget().hashCode() : 0))
                .function("class", 0, context -> context.setReturnRef(context.getTarget() != null ? context.getTarget().getClass() : null))
                .function("isInstance", 1, context -> {
                    if (context.getTarget() == null) {
                        context.setReturnRef(false);
                        return;
                    }
                    Object arg = context.getRef(0);
                    if (!(arg instanceof Class)) {
                        context.setReturnRef(false);
                        return;
                    }
                    context.setReturnRef(((Class<?>) arg).isInstance(context.getTarget()));
                });
    }
}
