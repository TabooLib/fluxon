package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

public class FunctionType {

    public static void init(FluxonRuntime runtime) {
        runtime.registerFunction("int", 1, (target, args) -> Coerce.asInteger(args[0]).orElse(0));
        runtime.registerFunction("long", 1, (target, args) -> Coerce.asLong(args[0]).orElse(0L));
        runtime.registerFunction("float", 1, (target, args) -> Coerce.asFloat(args[0]).orElse(0f));
        runtime.registerFunction("double", 1, (target, args) -> Coerce.asDouble(args[0]).orElse(0d));
        runtime.registerFunction("string", 1, (target, args) -> args[0].toString());
    }
}
