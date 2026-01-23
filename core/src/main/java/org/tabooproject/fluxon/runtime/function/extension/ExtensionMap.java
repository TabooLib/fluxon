package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Map;
import java.util.Objects;

public class ExtensionMap {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Map.class)
                // 添加键值对
                .function("put", 2, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.put(context.getRef(0), context.getRef(1)));
                })
                // 获取指定键的值
                .function("get", 1, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.get(context.getRef(0)));
                })
                // 获取指定键的值，如果不存在则返回默认值
                .function("getOrDefault", 2, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.getOrDefault(context.getRef(0), context.getRef(1)));
                })
                // 移除指定键的键值对
                .function("remove", 1, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.remove(context.getRef(0)));
                })
                // 检查是否包含指定键
                .function("containsKey", 1, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.containsKey(context.getRef(0)));
                })
                // 检查是否包含指定值
                .function("containsValue", 1, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.containsValue(context.getRef(0)));
                })
                // 获取 Map 的大小
                .function("size", 0, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.size());
                })
                // 检查 Map 是否为空
                .function("isEmpty", 0, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.isEmpty());
                })
                // 清空 Map
                .function("clear", 0, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    map.clear();
                })
                // 获取所有键
                .function("keySet", 0, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.keySet());
                })
                // 获取所有值
                .function("values", 0, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.values());
                })
                // 获取所有键值对
                .function("entrySet", 0, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.entrySet());
                })
                // 添加所有键值对
                .function("putAll", 1, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    Map<Object, Object> otherMap = (Map<Object, Object>) context.getRef(0);
                    if (otherMap != null) {
                        map.putAll(otherMap);
                    }
                })
                // 如果键不存在则添加
                .function("putIfAbsent", 2, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.putIfAbsent(context.getRef(0), context.getRef(1)));
                })
                // 替换指定键的值
                .function("replace", 2, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.replace(context.getRef(0), context.getRef(1)));
                })
                // 替换指定键的值（仅当旧值匹配时）
                .function("replaceIfMatch", 3, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.replace(context.getRef(0), context.getRef(1), context.getRef(2)));
                })
                // 移除指定键值对（仅当键值匹配时）
                .function("removeIfMatch", 2, (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.remove(context.getRef(0), context.getRef(1)));
                });
    }
}
