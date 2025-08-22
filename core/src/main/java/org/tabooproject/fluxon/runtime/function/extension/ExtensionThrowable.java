package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

public class ExtensionThrowable {

    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Throwable.class)
                .function("message", 0, (context) -> context.getTarget().getMessage())
                .function("localizedMessage", 0, (context) -> context.getTarget().getLocalizedMessage())
                .function("cause", 0, (context) -> context.getTarget().getCause())
                .function("printStackTrace", 0, (context) -> {
                    context.getTarget().printStackTrace();
                    return null;
                });
    }
}
