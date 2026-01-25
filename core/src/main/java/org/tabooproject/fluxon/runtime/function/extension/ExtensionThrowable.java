package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class ExtensionThrowable {

    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Throwable.class)
                .function("message", returns(Type.STRING).noParams(), context -> context.setReturnRef(context.getTarget().getMessage()))
                .function("localizedMessage", returns(Type.STRING).noParams(), context -> context.setReturnRef(context.getTarget().getLocalizedMessage()))
                .function("cause", returns(Type.OBJECT).noParams(), context -> context.setReturnRef(context.getTarget().getCause()))
                .function("printStackTrace", returns(Type.VOID).noParams(), context -> context.getTarget().printStackTrace());
    }
}
