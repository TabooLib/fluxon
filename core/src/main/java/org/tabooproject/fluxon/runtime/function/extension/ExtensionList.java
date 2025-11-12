package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ExtensionList {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(List.class)
                // 获取指定索引的元素
                .function("get", 1, (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    int index = context.getNumber(0).intValue();
                    return list.get(index);
                })
                // 设置指定索引的元素
                .function("set", 2, (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    int index = context.getNumber(0).intValue();
                    return list.set(index, context.getArgument(1));
                })
                // 在指定位置添加元素
                .function("insert", 2, (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    int index = context.getNumber(0).intValue();
                    list.add(index, context.getArgument(1));
                    return list;
                })
                // 移除指定索引的元素
                .function("removeAt", 1, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    int index = context.getNumber(0).intValue();
                    return list.remove(index);
                })
                // 获取元素的索引
                .function("indexOf", 1, (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    return list.indexOf(context.getArgument(0));
                })
                // 获取元素的最后索引
                .function("lastIndexOf", 1, (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    return list.lastIndexOf(context.getArgument(0));
                })
                // 获取子列表
                .function("subList", 2, (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    int fromIndex = context.getNumber(0).intValue();
                    int toIndex = context.getNumber(1).intValue();
                    return list.subList(fromIndex, toIndex);
                });
    }
}