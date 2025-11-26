package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FunctionType {

    public static void init(FluxonRuntime runtime) {
        // 类型转换函数
        runtime.registerFunction("string", 1, (context) -> context.getString(0));
        runtime.registerFunction("int", 1, (context) -> Coerce.asInteger(context.getArgument(0)).orElse(0));
        runtime.registerFunction("intOrNull", 1, (context) -> Coerce.asInteger(context.getArgument(0)).orElse(null));
        runtime.registerFunction("long", 1, (context) -> Coerce.asLong(context.getArgument(0)).orElse(0L));
        runtime.registerFunction("longOrNull", 1, (context) -> Coerce.asLong(context.getArgument(0)).orElse(null));
        runtime.registerFunction("float", 1, (context) -> Coerce.asFloat(context.getArgument(0)).orElse(0f));
        runtime.registerFunction("floatOrNull", 1, (context) -> Coerce.asFloat(context.getArgument(0)).orElse(null));
        runtime.registerFunction("double", 1, (context) -> Coerce.asDouble(context.getArgument(0)).orElse(0d));
        runtime.registerFunction("doubleOrNull", 1, (context) -> Coerce.asDouble(context.getArgument(0)).orElse(null));

        // 集合转换为数组
        runtime.registerFunction("array", 1, (context) -> {
            Collection<?> collection = context.getArgumentByType(0, Collection.class);
            if (collection != null) {
                return collection.toArray();
            } else {
                return null;
            }
        });

        // 获取对象类型
        runtime.registerFunction("typeOf", 1, (context) -> {
            Object input = context.getArgument(0);
            if (input == null) {
                return "null";
            }
            return input.getClass().getSimpleName();
        });

        // 快速类型检查
        runtime.registerFunction("isString", 1, (context) -> context.isType(0, String.class));
        runtime.registerFunction("isNumber", 1, (context) -> context.isType(0, Number.class));
        runtime.registerFunction("isArray", 1, (context) -> context.isType(0, Object[].class));
        runtime.registerFunction("isList", 1, (context) -> context.isType(0, List.class));
        runtime.registerFunction("isMap", 1, (context) -> context.isType(0, Map.class));
    }
}
