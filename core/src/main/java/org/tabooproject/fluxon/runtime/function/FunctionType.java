package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.util.Collection;

public class FunctionType {

    public static void init(FluxonRuntime runtime) {
        runtime.registerFunction("int", 1, (context) -> Coerce.asInteger(context.getArgument(0)).orElse(0));
        runtime.registerFunction("intOrNull", 1, (context) -> Coerce.asInteger(context.getArgument(0)).orElse(null));

        runtime.registerFunction("long", 1, (context) -> Coerce.asLong(context.getArgument(0)).orElse(0L));
        runtime.registerFunction("longOrNull", 1, (context) -> Coerce.asLong(context.getArgument(0)).orElse(null));

        runtime.registerFunction("float", 1, (context) -> Coerce.asFloat(context.getArgument(0)).orElse(0f));
        runtime.registerFunction("floatOrNull", 1, (context) -> Coerce.asFloat(context.getArgument(0)).orElse(null));

        runtime.registerFunction("double", 1, (context) -> Coerce.asDouble(context.getArgument(0)).orElse(0d));
        runtime.registerFunction("doubleOrNull", 1, (context) -> Coerce.asDouble(context.getArgument(0)).orElse(null));

        runtime.registerFunction("string", 1, (context) -> context.getString(0));
        runtime.registerFunction("array", 1, (context) -> {
            Collection<?> collection = context.getArgumentByType(0, Collection.class);
            if (collection != null) {
                return collection.toArray();
            } else {
                return null;
            }
        });
        runtime.registerFunction("typeOf", 1, (context) -> {
            Object input = context.getArgument(0);
            if (input == null) {
                return "null";
            }
            return input.getClass().getSimpleName();
        });
    }
}
