package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.List;
import java.util.Objects;

public class ExtensionList {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(List.class)
                // 获取指定索引的元素
                .function("get", 1, (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    int index = context.getAsInt(0);
                    context.setReturnRef(list.get(index));
                })
                // 设置指定索引的元素
                .function("set", 2, (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    int index = context.getAsInt(0);
                    context.setReturnRef(list.set(index, context.getRef(1)));
                })
                // 在指定位置添加元素
                .function("insert", 2, (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    int index = context.getAsInt(0);
                    list.add(index, context.getRef(1));
                    context.setReturnRef(list);
                })
                // 移除指定索引的元素
                .function("removeAt", 1, (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    int index = context.getAsInt(0);
                    context.setReturnRef(list.remove(index));
                })
                // 获取元素的索引
                .function("indexOf", 1, (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(list.indexOf(context.getArgBoxed(0)));
                })
                // 获取元素的最后索引
                .function("lastIndexOf", 1, (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(list.lastIndexOf(context.getArgBoxed(0)));
                })
                // 获取子列表
                .function("subList", 2, (context) -> {
                    List<Object> list = Objects.requireNonNull(context.getTarget());
                    int fromIndex = context.getAsInt(0);
                    int toIndex = context.getAsInt(1);
                    context.setReturnRef(list.subList(fromIndex, toIndex));
                });
    }
}
