package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.util.*;
import java.util.stream.Collectors;

public class ExtensionCollection {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Collection.class)
                // 获取列表大小
                .function("size", 0, (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).size()))
                // 检查列表是否为空
                .function("isEmpty", 0, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(list.isEmpty());
                })
                // 检查是否包含某个元素
                .function("contains", 1, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(list.contains(context.getRef(0)));
                })
                // 转换为数组
                .function("toArray", 0, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(list.toArray());
                })
                // 添加元素
                .function("add", 1, (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).add(context.getRef(0))))
                // 移除元素
                .function("remove", 1, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(list.remove(context.getRef(0)));
                })
                // 添加所有元素
                .function("addAll", 1, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    Collection<Object> collection = (Collection<Object>) context.getRef(0);
                    if (collection == null) {
                        context.setReturnRef(false);
                        return;
                    }
                    context.setReturnRef(list.addAll(collection));
                })
                // 移除所有元素
                .function("removeAll", 1, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    Collection<Object> collection = (Collection<Object>) context.getRef(0);
                    if (collection == null) {
                        context.setReturnRef(false);
                        return;
                    }
                    context.setReturnRef(list.removeAll(collection));
                })
                // 清空列表
                .function("clear", 0, (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    list.clear();
                })
                // 转换为字符串
                .function("join", Arrays.asList(0, 1), (context) -> {
                    // 获取分隔符参数，默认值为 ", "
                    String delimiter = Coerce.asString(Objects.toString(context.getRef(0), null)).orElse(", ");
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(list.stream().map(Object::toString).collect(Collectors.joining(delimiter)));
                })
                // 随机获取元素
                .function("random", Arrays.asList(0, 1), (context) -> {
                    Collection<Object> list = Objects.requireNonNull(context.getTarget());
                    if (list.isEmpty()) {
                        return;
                    }
                    // 如果没有参数，返回一个随机元素
                    if (!(0 < context.getArgumentCount())) {
                        List<Object> tempList = new ArrayList<>(list);
                        int index = (int) (Math.random() * tempList.size());
                        context.setReturnRef(tempList.get(index));
                        return;
                    }
                    // 如果有参数，返回指定数量的不重复随机元素
                    int count = ((Number) context.getRef(0)).intValue();
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
                });
        ;
    }
}
