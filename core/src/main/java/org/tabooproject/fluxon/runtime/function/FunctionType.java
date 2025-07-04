package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;
import org.tabooproject.fluxon.runtime.stdlib.Operations;

public class FunctionType {

    public static void init(FluxonRuntime runtime) {
        runtime.registerFunction("int", 1, args -> Coerce.asInteger(args[0]).orElse(0));
        runtime.registerFunction("long", 1, args -> Coerce.asLong(args[0]).orElse(0L));
        runtime.registerFunction("float", 1, args -> Coerce.asFloat(args[0]).orElse(0f));
        runtime.registerFunction("double", 1, args -> Coerce.asDouble(args[0]).orElse(0d));
        runtime.registerFunction("string", 1, args -> args[0].toString());
    }
}
