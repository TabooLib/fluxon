package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Map;
import java.util.Objects;

public class ExtensionMap {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionMap.class);
    }

    // 添加键值对
    @FluxonFunction(value = "put", target = Map.class)
    @SuppressWarnings("unchecked")
    public static Object put(Map<?, ?> map, Object key, Object value) {
        return ((Map<Object, Object>) Objects.requireNonNull(map)).put(key, value);
    }

    // 获取指定键的值
    @FluxonFunction(value = "get", target = Map.class)
    public static Object get(Map<?, ?> map, Object key) {
        return Objects.requireNonNull(map).get(key);
    }

    // 获取指定键的值，如果不存在则返回默认值
    @FluxonFunction(value = "getOrDefault", target = Map.class)
    @SuppressWarnings("unchecked")
    public static Object getOrDefault(Map<?, ?> map, Object key, Object defaultValue) {
        return ((Map<Object, Object>) Objects.requireNonNull(map)).getOrDefault(key, defaultValue);
    }

    // 移除指定键的键值对
    @FluxonFunction(value = "remove", target = Map.class)
    public static Object remove(Map<?, ?> map, Object key) {
        return Objects.requireNonNull(map).remove(key);
    }

    // 检查是否包含指定键
    @FluxonFunction(value = "containsKey", target = Map.class)
    public static boolean containsKey(Map<?, ?> map, Object key) {
        return Objects.requireNonNull(map).containsKey(key);
    }

    // 检查是否包含指定值
    @FluxonFunction(value = "containsValue", target = Map.class)
    public static boolean containsValue(Map<?, ?> map, Object value) {
        return Objects.requireNonNull(map).containsValue(value);
    }

    // 获取 Map 的大小
    @FluxonFunction(value = "size", target = Map.class)
    public static int size(Map<?, ?> map) {
        return Objects.requireNonNull(map).size();
    }

    // 检查 Map 是否为空
    @FluxonFunction(value = "isEmpty", target = Map.class)
    public static boolean isEmpty(Map<?, ?> map) {
        return Objects.requireNonNull(map).isEmpty();
    }

    // 清空 Map
    @FluxonFunction(value = "clear", target = Map.class)
    public static void clear(Map<?, ?> map) {
        Objects.requireNonNull(map).clear();
    }

    // 获取所有键
    @FluxonFunction(value = "keySet", target = Map.class)
    public static Object keySet(Map<?, ?> map) {
        return Objects.requireNonNull(map).keySet();
    }

    // 获取所有值
    @FluxonFunction(value = "values", target = Map.class)
    public static Object values(Map<?, ?> map) {
        return Objects.requireNonNull(map).values();
    }

    // 获取所有键值对
    @FluxonFunction(value = "entrySet", target = Map.class)
    public static Object entrySet(Map<?, ?> map) {
        return Objects.requireNonNull(map).entrySet();
    }

    // 如果键不存在则添加
    @FluxonFunction(value = "putIfAbsent", target = Map.class)
    @SuppressWarnings("unchecked")
    public static Object putIfAbsent(Map<?, ?> map, Object key, Object value) {
        return ((Map<Object, Object>) Objects.requireNonNull(map)).putIfAbsent(key, value);
    }

    // 替换指定键的值
    @FluxonFunction(value = "replace", target = Map.class)
    @SuppressWarnings("unchecked")
    public static Object replace(Map<?, ?> map, Object key, Object value) {
        return ((Map<Object, Object>) Objects.requireNonNull(map)).replace(key, value);
    }

    // 替换指定键的值（仅当旧值匹配时）
    @FluxonFunction(value = "replaceIfMatch", target = Map.class)
    @SuppressWarnings("unchecked")
    public static boolean replaceIfMatch(Map<?, ?> map, Object key, Object oldValue, Object newValue) {
        return ((Map<Object, Object>) Objects.requireNonNull(map)).replace(key, oldValue, newValue);
    }

    // 移除指定键值对（仅当键值匹配时）
    @FluxonFunction(value = "removeIfMatch", target = Map.class)
    public static boolean removeIfMatch(Map<?, ?> map, Object key, Object value) {
        return Objects.requireNonNull(map).remove(key, value);
    }

    // 合并另一个 Map 的所有键值对
    @FluxonFunction(value = "putAll", target = Map.class)
    @SuppressWarnings("unchecked")
    public static void putAll(Map<?, ?> map, Map<?, ?> otherMap) {
        if (otherMap != null) {
            ((Map<Object, Object>) Objects.requireNonNull(map)).putAll(otherMap);
        }
    }
}
