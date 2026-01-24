package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.util.Map;
import java.util.Objects;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class ExtensionMap {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Map.class)
                // 添加键值对
                .function("put", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.put(context.getArgBoxed(0), context.getArgBoxed(1)));
                })
                // 获取指定键的值
                .function("get", returns(Type.OBJECT).params(Type.OBJECT), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.get(context.getArgBoxed(0)));
                })
                // 获取指定键的值，如果不存在则返回默认值
                .function("getOrDefault", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.getOrDefault(context.getArgBoxed(0), context.getArgBoxed(1)));
                })
                // 移除指定键的键值对
                .function("remove", returns(Type.OBJECT).params(Type.OBJECT), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.remove(context.getArgBoxed(0)));
                })
                // 检查是否包含指定键
                .function("containsKey", returns(Type.Z).params(Type.OBJECT), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(map.containsKey(context.getArgBoxed(0)));
                })
                // 检查是否包含指定值
                .function("containsValue", returns(Type.Z).params(Type.OBJECT), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(map.containsValue(context.getArgBoxed(0)));
                })
                // 获取 Map 的大小
                .function("size", returns(Type.I).noParams(), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnInt(map.size());
                })
                // 检查 Map 是否为空
                .function("isEmpty", returns(Type.Z).noParams(), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(map.isEmpty());
                })
                // 清空 Map
                .function("clear", returns(Type.VOID).noParams(), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    map.clear();
                })
                // 获取所有键
                .function("keySet", returns(Type.OBJECT).noParams(), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.keySet());
                })
                // 获取所有值
                .function("values", returns(Type.OBJECT).noParams(), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.values());
                })
                // 获取所有键值对
                .function("entrySet", returns(Type.OBJECT).noParams(), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.entrySet());
                })
                // 添加所有键值对
                .function("putAll", returns(Type.VOID).params(Type.OBJECT), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    Map<Object, Object> otherMap = (Map<Object, Object>) context.getRef(0);
                    if (otherMap != null) {
                        map.putAll(otherMap);
                    }
                })
                // 如果键不存在则添加
                .function("putIfAbsent", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.putIfAbsent(context.getArgBoxed(0), context.getArgBoxed(1)));
                })
                // 替换指定键的值
                .function("replace", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(map.replace(context.getArgBoxed(0), context.getArgBoxed(1)));
                })
                // 替换指定键的值（仅当旧值匹配时）
                .function("replaceIfMatch", returns(Type.Z).params(Type.OBJECT, Type.OBJECT, Type.OBJECT), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(map.replace(context.getArgBoxed(0), context.getArgBoxed(1), context.getArgBoxed(2)));
                })
                // 移除指定键值对（仅当键值匹配时）
                .function("removeIfMatch", returns(Type.Z).params(Type.OBJECT, Type.OBJECT), (context) -> {
                    Map<Object, Object> map = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(map.remove(context.getArgBoxed(0), context.getArgBoxed(1)));
                });
    }
}
