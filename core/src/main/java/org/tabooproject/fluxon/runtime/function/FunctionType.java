package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

public class FunctionType {

    public static void init(FluxonRuntime runtime) {
        runtime.registerFunction("int", 1, (context) -> Coerce.asInteger(context.getArguments()[0]).orElse(0));
        runtime.registerFunction("long", 1, (context) -> Coerce.asLong(context.getArguments()[0]).orElse(0L));
        runtime.registerFunction("float", 1, (context) -> Coerce.asFloat(context.getArguments()[0]).orElse(0f));
        runtime.registerFunction("double", 1, (context) -> Coerce.asDouble(context.getArguments()[0]).orElse(0d));
        runtime.registerFunction("string", 1, (context) -> context.getArguments()[0].toString());
    }
}
