package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.*;

public class ExtensionIterable {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Iterable.class)
                // 取第一个元素
                .function("first", 0, (context) -> {
                    Iterable<Object> iterable = Objects.requireNonNull(context.getTarget());
                    if (iterable instanceof List) {
                        List<Object> list = (List<Object>) iterable;
                        if (list.isEmpty()) return null;
                        return list.get(0);
                    }
                    return iterable.iterator().next();
                })
                // 取最后一个元素
                .function("last", 0, (context) -> {
                    Iterable<Object> iterable = Objects.requireNonNull(context.getTarget());
                    if (iterable instanceof List) {
                        List<Object> list = (List<Object>) iterable;
                        if (list.isEmpty()) return null;
                        return list.get(list.size() - 1);
                    }
                    Iterator<Object> iterator = iterable.iterator();
                    Object last = null;
                    while (iterator.hasNext()) {
                        last = iterator.next();
                    }
                    return last;
                })
                // 取前 n 个元素
                .function("take", 1, (context) -> {
                    Iterable<Object> list = Objects.requireNonNull(context.getTarget());
                    int n = context.getNumber(0).intValue();
                    // 如果 n <= 0 丢弃所有元素
                    if (n <= 0) return new ArrayList<>();
                    List<Object> result = new ArrayList<>(n);
                    int count = 0;
                    for (Object object : list) {
                        if (count >= n) {
                            break;
                        }
                        result.add(object);
                        count++;
                    }
                    return result;
                })
                // 丢弃前 n 个元素
                .function("drop", 1, (context) -> {
                    Iterable<Object> list = Objects.requireNonNull(context.getTarget());
                    int n = context.getNumber(0).intValue();
                    // 如果 n <= 0 保留所有元素
                    if (n <= 0) return list;
                    List<Object> result = new ArrayList<>();
                    int count = 0;
                    for (Object object : list) {
                        if (count >= n) {
                            result.add(object);
                        }
                        count++;
                    }
                    return result;
                })
                // 取后 n 个元素
                .function("takeLast", 1, (context) -> {
                    Iterable<Object> iterable = Objects.requireNonNull(context.getTarget());
                    int n = context.getNumber(0).intValue();
                    // 如果 n <= 0 丢弃所有元素
                    if (n <= 0) return new ArrayList<>();
                    List<Object> list = new ArrayList<>();
                    for (Object object : iterable) {
                        list.add(object);
                    }
                    int size = list.size();
                    if (n >= size) return list;
                    return list.subList(size - n, size);
                })
                // 丢弃后 n 个元素
                .function("dropLast", 1, (context) -> {
                    Iterable<Object> iterable = Objects.requireNonNull(context.getTarget());
                    int n = context.getNumber(0).intValue();
                    // 如果 n <= 0 保留所有元素
                    if (n <= 0) return iterable;
                    List<Object> list = new ArrayList<>();
                    for (Object object : iterable) {
                        list.add(object);
                    }
                    int size = list.size();
                    if (n >= size) return new ArrayList<>();
                    return list.subList(0, size - n);
                });
    }
}