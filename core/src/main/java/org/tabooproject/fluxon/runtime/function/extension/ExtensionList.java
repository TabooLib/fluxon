package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.FunctionContext;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ExtensionList {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        // 添加元素
        runtime.registerExtensionFunction(List.class, "add", 1, (context) -> ((List<Object>) Objects.requireNonNull(context.getTarget())).add(context.getArguments()[0]));
        // 获取列表大小
        runtime.registerExtensionFunction(List.class, "size", 0, (context) -> ((List<Object>) Objects.requireNonNull(context.getTarget())).size());
        // 获取指定索引的元素
        runtime.registerExtensionFunction(List.class, "get", 1, (context) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(context.getTarget());
            int index = ((Number) context.getArguments()[0]).intValue();
            return list.get(index);
        });
        // 设置指定索引的元素
        runtime.registerExtensionFunction(List.class, "set", 2, (context) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(context.getTarget());
            int index = ((Number) context.getArguments()[0]).intValue();
            return list.set(index, context.getArguments()[1]);
        });
        // 移除指定索引的元素
        runtime.registerExtensionFunction(List.class, "remove", 1, (context) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(context.getTarget());
            if (context.getArguments()[0] instanceof Number) {
                int index = ((Number) context.getArguments()[0]).intValue();
                return list.remove(index);
            } else {
                return list.remove(context.getArguments()[0]);
            }
        });
        // 检查是否包含某个元素
        runtime.registerExtensionFunction(List.class, "contains", 1, (context) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(context.getTarget());
            return list.contains(context.getArguments()[0]);
        });
        
        // 获取元素的索引
        runtime.registerExtensionFunction(List.class, "indexOf", 1, (context) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(context.getTarget());
            return list.indexOf(context.getArguments()[0]);
        });
        // 获取元素的最后索引
        runtime.registerExtensionFunction(List.class, "lastIndexOf", 1, (context) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(context.getTarget());
            return list.lastIndexOf(context.getArguments()[0]);
        });
        // 清空列表
        runtime.registerExtensionFunction(List.class, "clear", 0, (context) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(context.getTarget());
            list.clear();
            return null;
        });
        // 检查列表是否为空
        runtime.registerExtensionFunction(List.class, "isEmpty", 0, (context) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(context.getTarget());
            return list.isEmpty();
        });
        
        // 添加所有元素
        runtime.registerExtensionFunction(List.class, "addAll", 1, (context) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(context.getTarget());
            Collection<Object> collection = (Collection<Object>) context.getArguments()[0];
            return list.addAll(collection);
        });
        // 在指定位置插入元素
        runtime.registerExtensionFunction(List.class, "insert", 2, (context) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(context.getTarget());
            int index = ((Number) context.getArguments()[0]).intValue();
            list.add(index, context.getArguments()[1]);
            return null;
        });
        // 获取子列表
        runtime.registerExtensionFunction(List.class, "subList", 2, (context) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(context.getTarget());
            int fromIndex = ((Number) context.getArguments()[0]).intValue();
            int toIndex = ((Number) context.getArguments()[1]).intValue();
            return list.subList(fromIndex, toIndex);
        });
        // 转换为数组
        runtime.registerExtensionFunction(List.class, "toArray", 0, (context) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(context.getTarget());
            return list.toArray();
        });
    }
}
