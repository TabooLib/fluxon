package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.util.Objects;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class ExtensionObject {

    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Object.class)
                .function("toString", returns(Type.STRING).noParams(), context -> context.setReturnRef(Objects.toString(context.getTarget())))
                .function("hashCode", returns(Type.I).noParams(), context -> context.setReturnInt(context.getTarget() != null ? context.getTarget().hashCode() : 0))
                .function("class", returns(Type.CLASS).noParams(), context -> context.setReturnRef(context.getTarget() != null ? context.getTarget().getClass() : null))
                .function("isInstance", returns(Type.Z).params(Type.OBJECT), context -> {
                    if (context.getTarget() == null) {
                        context.setReturnBool(false);
                        return;
                    }
                    Object arg = context.getRef(0);
                    if (!(arg instanceof Class)) {
                        context.setReturnBool(false);
                        return;
                    }
                    context.setReturnBool(((Class<?>) arg).isInstance(context.getTarget()));
                });
    }
}
