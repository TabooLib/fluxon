package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Collection;
import java.util.Objects;

public class ExtensionCollection {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        // 添加元素
        runtime.registerExtensionFunction(Collection.class, "add", 1, (context) -> Objects.requireNonNull(context.getTarget()).add(context.getArguments()[0]));
        // 获取列表大小
        runtime.registerExtensionFunction(Collection.class, "size", 0, (context) -> Objects.requireNonNull(context.getTarget()).size());
        // 移除指定索引的元素
        runtime.registerExtensionFunction(Collection.class, "remove", 1, (context) -> {
            Collection<Object> list = Objects.requireNonNull(context.getTarget());
            if (context.getArguments()[0] instanceof Number) {
                int index = ((Number) context.getArguments()[0]).intValue();
                return list.remove(index);
            } else {
                return list.remove(context.getArguments()[0]);
            }
        });
        // 检查是否包含某个元素
        runtime.registerExtensionFunction(Collection.class, "contains", 1, (context) -> {
            Collection<Object> list = Objects.requireNonNull(context.getTarget());
            return list.contains(context.getArguments()[0]);
        });
        // 清空列表
        runtime.registerExtensionFunction(Collection.class, "clear", 0, (context) -> {
            Collection<Object> list = Objects.requireNonNull(context.getTarget());
            list.clear();
            return null;
        });
        // 检查列表是否为空
        runtime.registerExtensionFunction(Collection.class, "isEmpty", 0, (context) -> {
            Collection<Object> list = Objects.requireNonNull(context.getTarget());
            return list.isEmpty();
        });
        // 添加所有元素
        runtime.registerExtensionFunction(Collection.class, "addAll", 1, (context) -> {
            Collection<Object> list = Objects.requireNonNull(context.getTarget());
            Collection<Object> collection = (Collection<Object>) context.getArguments()[0];
            return list.addAll(collection);
        });
        // 转换为数组
        runtime.registerExtensionFunction(Collection.class, "toArray", 0, (context) -> {
            Collection<Object> list = Objects.requireNonNull(context.getTarget());
            return list.toArray();
        });
    }
}
