package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ExtensionList {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        // 获取指定索引的元素
        runtime.registerExtensionFunction(List.class, "get", 1, (context) -> {
            List<Object> list = Objects.requireNonNull(context.getTarget());
            int index = ((Number) context.getArguments()[0]).intValue();
            return list.get(index);
        });
        // 设置指定索引的元素
        runtime.registerExtensionFunction(List.class, "set", 2, (context) -> {
            List<Object> list = Objects.requireNonNull(context.getTarget());
            int index = ((Number) context.getArguments()[0]).intValue();
            return list.set(index, context.getArguments()[1]);
        });
        // 获取元素的索引
        runtime.registerExtensionFunction(List.class, "indexOf", 1, (context) -> {
            List<Object> list = Objects.requireNonNull(context.getTarget());
            return list.indexOf(context.getArguments()[0]);
        });
        // 获取元素的最后索引
        runtime.registerExtensionFunction(List.class, "lastIndexOf", 1, (context) -> {
            List<Object> list = Objects.requireNonNull(context.getTarget());
            return list.lastIndexOf(context.getArguments()[0]);
        });
        // 在指定位置插入元素
        runtime.registerExtensionFunction(List.class, "insert", 2, (context) -> {
            List<Object> list = Objects.requireNonNull(context.getTarget());
            int index = ((Number) context.getArguments()[0]).intValue();
            list.add(index, context.getArguments()[1]);
            return null;
        });
        // 获取子列表
        runtime.registerExtensionFunction(List.class, "subList", 2, (context) -> {
            List<Object> list = Objects.requireNonNull(context.getTarget());
            int fromIndex = ((Number) context.getArguments()[0]).intValue();
            int toIndex = ((Number) context.getArguments()[1]).intValue();
            return list.subList(fromIndex, toIndex);
        });
    }
}
