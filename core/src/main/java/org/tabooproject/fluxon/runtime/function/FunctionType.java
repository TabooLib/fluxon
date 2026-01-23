package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.util.*;

public class FunctionType {

    public static void init(FluxonRuntime runtime) {
        // 类型转换函数
        runtime.registerFunction("string", 1, context -> {
            Object arg = context.getArgBoxed(0);
            context.setReturnRef(arg != null ? arg.toString() : null);
        });
        runtime.registerFunction("int", 1, context -> context.setReturnRef(Coerce.asInteger(context.getArgBoxed(0)).orElse(0)));
        runtime.registerFunction("intOrNull", 1, context -> context.setReturnRef(Coerce.asInteger(context.getArgBoxed(0)).orElse(null)));
        runtime.registerFunction("long", 1, context -> context.setReturnRef(Coerce.asLong(context.getArgBoxed(0)).orElse(0L)));
        runtime.registerFunction("longOrNull", 1, context -> context.setReturnRef(Coerce.asLong(context.getArgBoxed(0)).orElse(null)));
        runtime.registerFunction("float", 1, context -> context.setReturnRef(Coerce.asFloat(context.getArgBoxed(0)).orElse(0f)));
        runtime.registerFunction("floatOrNull", 1, context -> context.setReturnRef(Coerce.asFloat(context.getArgBoxed(0)).orElse(null)));
        runtime.registerFunction("double", 1, context -> context.setReturnRef(Coerce.asDouble(context.getArgBoxed(0)).orElse(0d)));
        runtime.registerFunction("doubleOrNull", 1, context -> context.setReturnRef(Coerce.asDouble(context.getArgBoxed(0)).orElse(null)));

        // 集合转换为数组
        runtime.registerFunction("array", 1, context -> {
            Object arg = context.getRef(0);
            if (arg instanceof Collection) {
                context.setReturnRef(((Collection<?>) arg).toArray());
            }
        });
        // 将数组转换为集合
        runtime.registerFunction("list", 1, context -> {
            Object arg = context.getRef(0);
            if (arg instanceof Object[]) {
                context.setReturnRef(Arrays.asList((Object[]) arg));
            }
        });
        // 将数组转换为可变集合
        runtime.registerFunction("mutableList", 1, context -> {
            Object arg = context.getRef(0);
            if (arg instanceof Object[]) {
                Object[] array = (Object[]) arg;
                ArrayList<Object> list = new ArrayList<>(array.length);
                Collections.addAll(list, array);
                context.setReturnRef(list);
            }
        });

        // 获取对象类型
        runtime.registerFunction("typeOf", 1, context -> {
            Object input = context.getArgBoxed(0);
            if (input == null) {
                context.setReturnRef("null");
            } else {
                context.setReturnRef(input.getClass().getSimpleName());
            }
        });

        // 快速类型检查
        runtime.registerFunction("isString", 1, context -> context.setReturnRef(context.getArgBoxed(0) instanceof String));
        runtime.registerFunction("isNumber", 1, context -> context.setReturnRef(context.getArgBoxed(0) instanceof Number));
        runtime.registerFunction("isArray", 1, context -> context.setReturnRef(context.getArgBoxed(0) instanceof Object[]));
        runtime.registerFunction("isList", 1, context -> context.setReturnRef(context.getArgBoxed(0) instanceof List));
        runtime.registerFunction("isMap", 1, context -> context.setReturnRef(context.getArgBoxed(0) instanceof Map));
    }
}
