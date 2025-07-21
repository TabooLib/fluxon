package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.FunctionContext;

import java.util.Map;
import java.util.Objects;

public class ExtensionMap {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        // 添加键值对
        runtime.registerExtensionFunction(Map.class, "put", 2, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.put(context.getArguments()[0], context.getArguments()[1]);
        });
        // 获取指定键的值
        runtime.registerExtensionFunction(Map.class, "get", 1, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.get(context.getArguments()[0]);
        });
        // 获取指定键的值，如果不存在则返回默认值
        runtime.registerExtensionFunction(Map.class, "getOrDefault", 2, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.getOrDefault(context.getArguments()[0], context.getArguments()[1]);
        });
        // 移除指定键的键值对
        runtime.registerExtensionFunction(Map.class, "remove", 1, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.remove(context.getArguments()[0]);
        });
        // 检查是否包含指定键
        runtime.registerExtensionFunction(Map.class, "containsKey", 1, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.containsKey(context.getArguments()[0]);
        });
        // 检查是否包含指定值
        runtime.registerExtensionFunction(Map.class, "containsValue", 1, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.containsValue(context.getArguments()[0]);
        });
        // 获取 Map 的大小
        runtime.registerExtensionFunction(Map.class, "size", 0, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.size();
        });
        // 检查 Map 是否为空
        runtime.registerExtensionFunction(Map.class, "isEmpty", 0, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.isEmpty();
        });
        // 清空 Map
        runtime.registerExtensionFunction(Map.class, "clear", 0, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            map.clear();
            return null;
        });
        // 获取所有键
        runtime.registerExtensionFunction(Map.class, "keySet", 0, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.keySet();
        });
        // 获取所有值
        runtime.registerExtensionFunction(Map.class, "values", 0, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.values();
        });
        // 获取所有键值对
        runtime.registerExtensionFunction(Map.class, "entrySet", 0, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.entrySet();
        });
        // 添加所有键值对
        runtime.registerExtensionFunction(Map.class, "putAll", 1, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            Map<Object, Object> otherMap = (Map<Object, Object>) context.getArguments()[0];
            map.putAll(otherMap);
            return null;
        });
        // 如果键不存在则添加
        runtime.registerExtensionFunction(Map.class, "putIfAbsent", 2, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.putIfAbsent(context.getArguments()[0], context.getArguments()[1]);
        });
        // 替换指定键的值
        runtime.registerExtensionFunction(Map.class, "replace", 2, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.replace(context.getArguments()[0], context.getArguments()[1]);
        });
        // 替换指定键的值（仅当旧值匹配时）
        runtime.registerExtensionFunction(Map.class, "replaceIfMatch", 3, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.replace(context.getArguments()[0], context.getArguments()[1], context.getArguments()[2]);
        });
        // 移除指定键值对（仅当键值匹配时）
        runtime.registerExtensionFunction(Map.class, "removeIfMatch", 2, (context) -> {
            Map<Object, Object> map = (Map<Object, Object>) Objects.requireNonNull(context.getTarget());
            return map.remove(context.getArguments()[0], context.getArguments()[1]);
        });
    }
} 