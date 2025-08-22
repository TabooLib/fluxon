package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.util.Collection;

public class FunctionType {

    public static void init(FluxonRuntime runtime) {
        runtime.registerFunction("int", 1, (context) -> Coerce.asInteger(context.getArguments()[0]).orElse(0));
        runtime.registerFunction("long", 1, (context) -> Coerce.asLong(context.getArguments()[0]).orElse(0L));
        runtime.registerFunction("float", 1, (context) -> Coerce.asFloat(context.getArguments()[0]).orElse(0f));
        runtime.registerFunction("double", 1, (context) -> Coerce.asDouble(context.getArguments()[0]).orElse(0d));
        runtime.registerFunction("string", 1, (context) -> context.getArguments()[0].toString());
        runtime.registerFunction("array", 1, (context) -> {
            Object input = context.getArguments()[0];
            if (input instanceof Collection) {
                Collection<?> collection = (Collection<?>) input;
                return collection.toArray();
            } else {
                throw new IllegalArgumentException("Input must be a Collection, but got: " + (input == null ? "null" : input.getClass().getSimpleName()));
            }
        });
    }
}
