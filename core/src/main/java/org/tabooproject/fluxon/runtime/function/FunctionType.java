package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.util.*;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class FunctionType {

    public static void init(FluxonRuntime runtime) {
        // 类型转换函数
        runtime.registerFunction("string", returns(Type.STRING).params(Type.OBJECT), context -> {
            Object arg = context.getArgBoxed(0);
            context.setReturnRef(arg != null ? arg.toString() : null);
        });
        runtime.registerFunction("int", returns(Type.I).params(Type.OBJECT), context -> context.setReturnInt(Coerce.asInteger(context.getArgBoxed(0)).orElse(0)));
        runtime.registerFunction("intOrNull", returns(Type.INT).params(Type.OBJECT), context -> context.setReturnRef(Coerce.asInteger(context.getArgBoxed(0)).orElse(null)));
        runtime.registerFunction("long", returns(Type.J).params(Type.OBJECT), context -> context.setReturnLong(Coerce.asLong(context.getArgBoxed(0)).orElse(0L)));
        runtime.registerFunction("longOrNull", returns(Type.LONG).params(Type.OBJECT), context -> context.setReturnRef(Coerce.asLong(context.getArgBoxed(0)).orElse(null)));
        runtime.registerFunction("float", returns(Type.F).params(Type.OBJECT), context -> context.setReturnFloat(Coerce.asFloat(context.getArgBoxed(0)).orElse(0f)));
        runtime.registerFunction("floatOrNull", returns(Type.FLOAT).params(Type.OBJECT), context -> context.setReturnRef(Coerce.asFloat(context.getArgBoxed(0)).orElse(null)));
        runtime.registerFunction("double", returns(Type.D).params(Type.OBJECT), context -> context.setReturnDouble(Coerce.asDouble(context.getArgBoxed(0)).orElse(0d)));
        runtime.registerFunction("doubleOrNull", returns(Type.DOUBLE).params(Type.OBJECT), context -> context.setReturnRef(Coerce.asDouble(context.getArgBoxed(0)).orElse(null)));

        // 集合转换为数组
        runtime.registerFunction("array", returns(Type.OBJECT).params(Type.OBJECT), context -> {
            Object arg = context.getRef(0);
            if (arg instanceof Collection) {
                context.setReturnRef(((Collection<?>) arg).toArray());
            }
        });
        // 将数组转换为集合
        runtime.registerFunction("list", returns(Type.LIST).params(Type.OBJECT), context -> {
            Object arg = context.getRef(0);
            if (arg instanceof Object[]) {
                context.setReturnRef(Arrays.asList((Object[]) arg));
            }
        });
        // 将数组转换为可变集合
        runtime.registerFunction("mutableList", returns(Type.LIST).params(Type.OBJECT), context -> {
            Object arg = context.getRef(0);
            if (arg instanceof Object[]) {
                Object[] array = (Object[]) arg;
                ArrayList<Object> list = new ArrayList<>(array.length);
                Collections.addAll(list, array);
                context.setReturnRef(list);
            }
        });

        // 获取对象类型
        runtime.registerFunction("typeOf", returns(Type.STRING).params(Type.OBJECT), context -> {
            Object input = context.getArgBoxed(0);
            if (input == null) {
                context.setReturnRef("null");
            } else {
                context.setReturnRef(input.getClass().getSimpleName());
            }
        });

        // 快速类型检查
        runtime.registerFunction("isString", returns(Type.Z).params(Type.OBJECT), context -> context.setReturnBool(context.getArgBoxed(0) instanceof String));
        runtime.registerFunction("isNumber", returns(Type.Z).params(Type.OBJECT), context -> context.setReturnBool(context.getArgBoxed(0) instanceof Number));
        runtime.registerFunction("isArray", returns(Type.Z).params(Type.OBJECT), context -> context.setReturnBool(context.getArgBoxed(0) instanceof Object[]));
        runtime.registerFunction("isList", returns(Type.Z).params(Type.OBJECT), context -> context.setReturnBool(context.getArgBoxed(0) instanceof List));
        runtime.registerFunction("isMap", returns(Type.Z).params(Type.OBJECT), context -> context.setReturnBool(context.getArgBoxed(0) instanceof Map));
    }
}
