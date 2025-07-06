package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ExtensionList {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        // 添加元素
        runtime.registerExtensionFunction(List.class, "add", 1, (target, args) -> ((List<Object>) Objects.requireNonNull(target)).add(args[0]));
        // 获取列表大小
        runtime.registerExtensionFunction(List.class, "size", 0, (target, args) -> ((List<Object>) Objects.requireNonNull(target)).size());
        // 获取指定索引的元素
        runtime.registerExtensionFunction(List.class, "get", 1, (target, args) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(target);
            int index = ((Number) args[0]).intValue();
            return list.get(index);
        });
        // 设置指定索引的元素
        runtime.registerExtensionFunction(List.class, "set", 2, (target, args) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(target);
            int index = ((Number) args[0]).intValue();
            return list.set(index, args[1]);
        });
        // 移除指定索引的元素
        runtime.registerExtensionFunction(List.class, "remove", 1, (target, args) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(target);
            if (args[0] instanceof Number) {
                int index = ((Number) args[0]).intValue();
                return list.remove(index);
            } else {
                return list.remove(args[0]);
            }
        });
        // 检查是否包含某个元素
        runtime.registerExtensionFunction(List.class, "contains", 1, (target, args) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(target);
            return list.contains(args[0]);
        });
        
        // 获取元素的索引
        runtime.registerExtensionFunction(List.class, "indexOf", 1, (target, args) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(target);
            return list.indexOf(args[0]);
        });
        // 获取元素的最后索引
        runtime.registerExtensionFunction(List.class, "lastIndexOf", 1, (target, args) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(target);
            return list.lastIndexOf(args[0]);
        });
        // 清空列表
        runtime.registerExtensionFunction(List.class, "clear", 0, (target, args) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(target);
            list.clear();
            return null;
        });
        // 检查列表是否为空
        runtime.registerExtensionFunction(List.class, "isEmpty", 0, (target, args) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(target);
            return list.isEmpty();
        });
        
        // 添加所有元素
        runtime.registerExtensionFunction(List.class, "addAll", 1, (target, args) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(target);
            Collection<Object> collection = (Collection<Object>) args[0];
            return list.addAll(collection);
        });
        // 在指定位置插入元素
        runtime.registerExtensionFunction(List.class, "insert", 2, (target, args) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(target);
            int index = ((Number) args[0]).intValue();
            list.add(index, args[1]);
            return null;
        });
        // 获取子列表
        runtime.registerExtensionFunction(List.class, "subList", 2, (target, args) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(target);
            int fromIndex = ((Number) args[0]).intValue();
            int toIndex = ((Number) args[1]).intValue();
            return list.subList(fromIndex, toIndex);
        });
        // 转换为数组
        runtime.registerExtensionFunction(List.class, "toArray", 0, (target, args) -> {
            List<Object> list = (List<Object>) Objects.requireNonNull(target);
            return list.toArray();
        });
    }
}
