package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Collection;
import java.util.Objects;

public class ExtensionCollection {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Collection.class)
                // 获取列表大小
                .function("size", 0, (context) -> Objects.requireNonNull(context.getTarget()).size())
                // 检查列表是否为空
                .function("isEmpty", 0, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    return list.isEmpty();
                })
                // 检查是否包含某个元素
                .function("contains", 1, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    return list.contains(context.getArguments()[0]);
                })
                // 转换为数组
                .function("toArray", 0, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    return list.toArray();
                })
                // 添加元素
                .function("add", 1, (context) -> Objects.requireNonNull(context.getTarget()).add(context.getArguments()[0]))
                // 移除元素
                .function("remove", 1, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    return list.remove(context.getArguments()[0]);
                })
                // 添加所有元素
                .function("addAll", 1, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    Collection<Object> collection = (Collection<Object>) context.getArguments()[0];
                    return list.addAll(collection);
                })
                // 移除所有元素
                .function("removeAll", 1, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    Collection<Object> collection = (Collection<Object>) context.getArguments()[0];
                    return list.removeAll(collection);
                })
                // 清空列表
                .function("clear", 0, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    list.clear();
                    return null;
                });
    }
}
