package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

public class ExtensionThrowable {

    public static void init(FluxonRuntime runtime) {
        runtime.registerExtensionFunction(Throwable.class, "message", 0, (context) -> context.getTarget().getMessage());
        runtime.registerExtensionFunction(Throwable.class, "localizedMessage", 0, (context) -> context.getTarget().getLocalizedMessage());
        runtime.registerExtensionFunction(Throwable.class, "cause", 0, (context) -> context.getTarget().getCause());
        runtime.registerExtensionFunction(Throwable.class, "printStackTrace", 0, (context) -> {
            context.getTarget().printStackTrace();
            return null;
        });
    }
}
