package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.util.*;
import java.util.stream.Collectors;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class ExtensionCollection {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Collection.class)
                // 获取列表大小
                .function("size", returns(Type.I).noParams(), (context) -> context.setReturnInt(Objects.requireNonNull(context.getTarget()).size()))
                // 检查列表是否为空
                .function("isEmpty", returns(Type.Z).noParams(), (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(list.isEmpty());
                })
                // 检查是否包含某个元素
                .function("contains", returns(Type.Z).params(Type.OBJECT), (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(list.contains(context.getArgBoxed(0)));
                })
                // 转换为数组
                .function("toArray", returns(Type.OBJECT).noParams(), (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(list.toArray());
                })
                // 添加元素
                .function("add", returns(Type.Z).params(Type.OBJECT), (context) -> context.setReturnBool(Objects.requireNonNull(context.getTarget()).add(context.getArgBoxed(0))))
                // 移除元素
                .function("remove", returns(Type.Z).params(Type.OBJECT), (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(list.remove(context.getArgBoxed(0)));
                })
                // 添加所有元素
                .function("addAll", returns(Type.Z).params(Type.OBJECT), (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    Collection<Object> collection = (Collection<Object>) context.getRef(0);
                    if (collection == null) {
                        context.setReturnBool(false);
                        return;
                    }
                    context.setReturnBool(list.addAll(collection));
                })
                // 移除所有元素
                .function("removeAll", returns(Type.Z).params(Type.OBJECT), (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    Collection<Object> collection = (Collection<Object>) context.getRef(0);
                    if (collection == null) {
                        context.setReturnBool(false);
                        return;
                    }
                    context.setReturnBool(list.removeAll(collection));
                })
                // 清空列表
                .function("clear", returns(Type.VOID).noParams(), (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    list.clear();
                })
                // 转换为字符串 (0-1 params)
                .function("join", returns(Type.STRING).params(Type.OBJECT), (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    String delimiter = ", ";
                    if (context.getArgumentCount() >= 1) {
                        delimiter = Coerce.asString(Objects.toString(context.getArgBoxed(0), null)).orElse(", ");
                    }
                    context.setReturnRef(list.stream().map(Object::toString).collect(Collectors.joining(delimiter)));
                })
                // 随机获取元素 (0-1 params)
                .function("random", returns(Type.OBJECT).params(Type.I), (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    if (list.isEmpty()) {
                        return;
                    }
                    if (context.getArgumentCount() < 1) {
                        // 返回单个随机元素
                        List<Object> tempList = new ArrayList<>(list);
                        int index = (int) (Math.random() * tempList.size());
                        context.setReturnRef(tempList.get(index));
                    } else {
                        int count = context.getInt(0);
                        if (count <= 0) {
                            return;
                        }
                        // 如果请求数量大于等于列表大小，返回打乱后的整个列表
                        if (count >= list.size()) {
                            List<Object> shuffled = new ArrayList<>(list);
                            Collections.shuffle(shuffled);
                            context.setReturnRef(shuffled);
                            return;
                        }
                        // 否则返回指定数量的不重复随机元素
                        List<Object> result = new ArrayList<>(count);
                        List<Object> copy = new ArrayList<>(list);
                        Collections.shuffle(copy);
                        for (int i = 0; i < count; i++) {
                            result.add(copy.get(i));
                        }
                        context.setReturnRef(result);
                    }
                });
    }
}
