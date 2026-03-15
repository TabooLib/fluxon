package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.util.*;

/**
 * 类型转换与类型检查函数
 *
 * @author sky
 */
public class FunctionType {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, FunctionType.class);
    }

    // 转换为字符串
    @FluxonFunction("string")
    public static String string(Object arg) {
        return arg != null ? arg.toString() : null;
    }

    // 转换为 int（失败返回 0）
    @FluxonFunction("int")
    public static int toInt(Object arg) {
        return Coerce.asInteger(arg).orElse(0);
    }

    // 转换为 int（失败返回 null）
    @FluxonFunction("intOrNull")
    public static Integer intOrNull(Object arg) {
        return Coerce.asInteger(arg).orElse(null);
    }

    // 转换为 long（失败返回 0）
    @FluxonFunction("long")
    public static long toLong(Object arg) {
        return Coerce.asLong(arg).orElse(0L);
    }

    // 转换为 long（失败返回 null）
    @FluxonFunction("longOrNull")
    public static Long longOrNull(Object arg) {
        return Coerce.asLong(arg).orElse(null);
    }

    // 转换为 float（失败返回 0）
    @FluxonFunction("float")
    public static float toFloat(Object arg) {
        return Coerce.asFloat(arg).orElse(0f);
    }

    // 转换为 float（失败返回 null）
    @FluxonFunction("floatOrNull")
    public static Float floatOrNull(Object arg) {
        return Coerce.asFloat(arg).orElse(null);
    }

    // 转换为 double（失败返回 0）
    @FluxonFunction("double")
    public static double toDouble(Object arg) {
        return Coerce.asDouble(arg).orElse(0d);
    }

    // 转换为 double（失败返回 null）
    @FluxonFunction("doubleOrNull")
    public static Double doubleOrNull(Object arg) {
        return Coerce.asDouble(arg).orElse(null);
    }

    // 集合转换为数组
    @FluxonFunction("array")
    public static Object array(Object arg) {
        if (arg instanceof Collection) {
            return ((Collection<?>) arg).toArray();
        }
        return null;
    }

    // 数组转换为不可变集合
    @FluxonFunction("list")
    public static Object list(Object arg) {
        if (arg instanceof Object[]) {
            return Arrays.asList((Object[]) arg);
        }
        return null;
    }

    // 数组转换为可变集合
    @FluxonFunction("mutableList")
    public static Object mutableList(Object arg) {
        if (arg instanceof Object[]) {
            Object[] a = (Object[]) arg;
            ArrayList<Object> list = new ArrayList<>(a.length);
            Collections.addAll(list, a);
            return list;
        }
        return null;
    }

    // 获取对象类型名称
    @FluxonFunction("typeOf")
    public static String typeOf(Object input) {
        return input != null ? input.getClass().getSimpleName() : "null";
    }

    // 快速类型检查
    @FluxonFunction("isString")
    public static boolean isString(Object arg) {
        return arg instanceof String;
    }

    @FluxonFunction("isNumber")
    public static boolean isNumber(Object arg) {
        return arg instanceof Number;
    }

    @FluxonFunction("isArray")
    public static boolean isArray(Object arg) {
        return arg instanceof Object[];
    }

    @FluxonFunction("isList")
    public static boolean isList(Object arg) {
        return arg instanceof List;
    }

    @FluxonFunction("isMap")
    public static boolean isMap(Object arg) {
        return arg instanceof Map;
    }
}
