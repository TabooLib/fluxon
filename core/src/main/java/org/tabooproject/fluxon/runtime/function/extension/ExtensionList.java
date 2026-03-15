package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.List;
import java.util.Objects;

public class ExtensionList {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionList.class);
    }

    // 获取指定索引的元素
    @FluxonFunction(value = "get", target = List.class)
    public static Object get(List<?> list, int index) {
        return Objects.requireNonNull(list).get(index);
    }

    // 设置指定索引的元素
    @FluxonFunction(value = "set", target = List.class)
    @SuppressWarnings("unchecked")
    public static Object set(List<?> list, int index, Object element) {
        return ((List<Object>) Objects.requireNonNull(list)).set(index, element);
    }

    // 在指定位置添加元素
    @FluxonFunction(value = "insert", target = List.class)
    @SuppressWarnings("unchecked")
    public static Object insert(List<?> list, int index, Object element) {
        List<Object> l = (List<Object>) Objects.requireNonNull(list);
        l.add(index, element);
        return l;
    }

    // 移除指定索引的元素
    @FluxonFunction(value = "removeAt", target = List.class)
    public static Object removeAt(List<?> list, int index) {
        return Objects.requireNonNull(list).remove(index);
    }

    // 获取元素的索引
    @FluxonFunction(value = "indexOf", target = List.class)
    public static int indexOf(List<?> list, Object element) {
        return Objects.requireNonNull(list).indexOf(element);
    }

    // 获取元素的最后索引
    @FluxonFunction(value = "lastIndexOf", target = List.class)
    public static int lastIndexOf(List<?> list, Object element) {
        return Objects.requireNonNull(list).lastIndexOf(element);
    }

    // 获取子列表
    @FluxonFunction(value = "subList", target = List.class)
    public static Object subList(List<?> list, int fromIndex, int toIndex) {
        return Objects.requireNonNull(list).subList(fromIndex, toIndex);
    }
}
