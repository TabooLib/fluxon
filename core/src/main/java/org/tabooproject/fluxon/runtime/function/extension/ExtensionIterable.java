package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.FunctionContextPool;
import org.tabooproject.fluxon.runtime.collection.ImmutableMap;
import org.tabooproject.fluxon.runtime.stdlib.Operations;
import org.tabooproject.fluxon.util.CollectionUtils;

import java.util.*;

import static org.tabooproject.fluxon.runtime.ExtensionBuilder.*;

@SuppressWarnings("unchecked")
public class ExtensionIterable {

    public static void init(FluxonRuntime runtime) {
        initTransformation(runtime);
        initFiltering(runtime);
        initChecking(runtime);
        initRetrieving(runtime);
        initAggregation(runtime);
        initGrouping(runtime);
        initOrdering(runtime);
        initOperations(runtime);
        // 直接遍历
        runtime.registerExtension(Iterable.class)
                .function("each", 1, (context) -> {
                    forEachElement(context, null);
                    context.setReturnRef(context.getTarget());
                });
    }

    public static void initTransformation(FluxonRuntime runtime) {
        runtime.registerExtension(Iterable.class)
                // 对每个元素应用函数
                .function("map", 1, (context) -> {
                    List<Object> result = new ArrayList<>();
                    forEachElement(context, (element, callResult) -> result.add(callResult));
                    context.setReturnRef(result);
                })
                // 对每个元素应用函数并展平结果
                .function("flatMap", 1, (context) -> {
                    List<Object> result = new ArrayList<>();
                    forEachElement(context, (element, callResult) -> {
                        if (callResult instanceof Collection) {
                            result.addAll((Collection<?>) callResult);
                        } else if (callResult instanceof Iterable) {
                            for (Object item : (Iterable<?>) callResult) result.add(item);
                        } else {
                            result.add(callResult);
                        }
                    });
                    context.setReturnRef(result);
                })
                // 根据键函数创建映射，元素作为值
                .function("associateBy", 1, (context) -> {
                    Map<Object, Object> result = new HashMap<>();
                    forEachElement(context, (element, callResult) -> {
                        result.put(callResult, element);
                    });
                    context.setReturnRef(result);
                })
                // 根据值函数创建映射，元素作为键
                .function("associateWith", 1, (context) -> {
                    Map<Object, Object> result = new HashMap<>();
                    forEachElement(context, result::put);
                    context.setReturnRef(result);
                });
    }

    public static void initFiltering(FluxonRuntime runtime) {
        runtime.registerExtension(Iterable.class)
                // 过滤元素
                .function("filter", 1, (context) -> {
                    List<Object> result = new ArrayList<>();
                    forEachElement(context, (element, callResult) -> {
                        if (Operations.isTrue(callResult)) result.add(element);
                    });
                    context.setReturnRef(result);
                });
    }

    public static void initChecking(FluxonRuntime runtime) {
        runtime.registerExtension(Iterable.class)
                // 检查是否有任意元素满足条件
                .function("any", 1, (context) -> {
                    context.setReturnRef(!testElements(context, (element, callResult) -> !Operations.isTrue(callResult)));
                })
                // 检查是否所有元素都满足条件
                .function("all", 1, (context) -> {
                    context.setReturnRef(testElements(context, (element, callResult) -> Operations.isTrue(callResult)));
                })
                // 检查是否没有元素满足条件
                .function("none", 1, (context) -> {
                    context.setReturnRef(testElements(context, (element, callResult) -> !Operations.isTrue(callResult)));
                });
    }

    public static void initRetrieving(FluxonRuntime runtime) {
        runtime.registerExtension(Iterable.class)
                // 查找第一个满足条件的元素
                .function("find", 1, (context) -> {
                    Object[] found = new Object[1];
                    testElements(context, (element, callResult) -> {
                        if (Operations.isTrue(callResult)) {
                            found[0] = element;
                            return false; // 找到后中断
                        }
                        return true; // 继续查找
                    });
                    context.setReturnRef(found[0]);
                })
                // 取第一个元素
                .function("first", 0, (context) -> {
                    Iterable<Object> iterable = Objects.requireNonNull(context.getTarget());
                    if (iterable instanceof List) {
                        List<Object> list = (List<Object>) iterable;
                        if (list.isEmpty()) {
                            context.setReturnRef(null);
                            return;
                        }
                        context.setReturnRef(list.get(0));
                        return;
                    }
                    context.setReturnRef(iterable.iterator().next());
                })
                // 取最后一个元素
                .function("last", 0, (context) -> {
                    Iterable<Object> iterable = Objects.requireNonNull(context.getTarget());
                    if (iterable instanceof List) {
                        List<Object> list = (List<Object>) iterable;
                        if (list.isEmpty()) {
                            context.setReturnRef(null);
                            return;
                        }
                        context.setReturnRef(list.get(list.size() - 1));
                        return;
                    }
                    Iterator<Object> iterator = iterable.iterator();
                    Object last = null;
                    while (iterator.hasNext()) {
                        last = iterator.next();
                    }
                    context.setReturnRef(last);
                });
    }

    public static void initAggregation(FluxonRuntime runtime) {
        runtime.registerExtension(Iterable.class)
                // 统计满足条件的元素数量
                .function("countOf", 1, (context) -> {
                    int[] count = new int[1];
                    forEachElement(context, (element, callResult) -> {
                        if (Operations.isTrue(callResult)) count[0]++;
                    });
                    context.setReturnRef(count[0]);
                })
                // 对每个元素应用函数并求和
                .function("sumOf", 1, (context) -> {
                    double[] sum = new double[1];
                    forEachElement(context, (element, callResult) -> {
                        if (callResult instanceof Number) {
                            sum[0] += ((Number) callResult).doubleValue();
                        }
                    });
                    context.setReturnRef(sum[0]);
                })
                // 对每个元素应用函数并求最小值
                .function("minOf", 1, (context) -> {
                    context.setReturnRef(compareElements(context, (current, candidate) ->
                            ((Comparable<Object>) candidate).compareTo(current) < 0
                    ));
                })
                // 对每个元素应用函数并求最大值
                .function("maxOf", 1, (context) -> {
                    context.setReturnRef(compareElements(context, (current, candidate) ->
                            ((Comparable<Object>) candidate).compareTo(current) > 0
                    ));
                })
                // 根据选择器函数找到最小值对应的元素
                .function("minBy", 1, (context) -> {
                    context.setReturnRef(compareElementsBy(context, (current, candidate) ->
                            ((Comparable<Object>) candidate).compareTo(current) < 0
                    ));
                })
                // 根据选择器函数找到最大值对应的元素
                .function("maxBy", 1, (context) -> {
                    context.setReturnRef(compareElementsBy(context, (current, candidate) ->
                            ((Comparable<Object>) candidate).compareTo(current) > 0
                    ));
                });
    }

    public static void initGrouping(FluxonRuntime runtime) {
        runtime.registerExtension(Iterable.class)
                // 分组元素
                .function("groupBy", 1, (context) -> {
                    Map<Object, List<Object>> result = new HashMap<>();
                    forEachElement(context, (element, callResult) -> {
                        result.computeIfAbsent(callResult, k -> new ArrayList<>()).add(element);
                    });
                    context.setReturnRef(result);
                })
                // 根据断言将元素分为两组
                .function("partition", 1, (context) -> {
                    List<Object> matched = new ArrayList<>();
                    List<Object> unmatched = new ArrayList<>();
                    forEachElement(context, (element, callResult) -> {
                        if (Operations.isTrue(callResult)) {
                            matched.add(element);
                        } else {
                            unmatched.add(element);
                        }
                    });
                    context.setReturnRef(ImmutableMap.of(true, matched, false, unmatched));
                })
                // 将元素按指定大小分块
                .function("chunked", 1, (context) -> {
                    Iterable<Object> iterable = Objects.requireNonNull(context.getTarget());
                    int size = ((Number) context.getRef(0)).intValue();
                    if (size <= 0) throw new IllegalArgumentException("Chunk size must be positive");
                    List<List<Object>> result = new ArrayList<>();
                    List<Object> currentChunk = new ArrayList<>(size);
                    for (Object element : iterable) {
                        currentChunk.add(element);
                        if (currentChunk.size() == size) {
                            result.add(currentChunk);
                            currentChunk = new ArrayList<>(size);
                        }
                    }
                    // 添加最后一个未满的块
                    if (!currentChunk.isEmpty()) {
                        result.add(currentChunk);
                    }
                    context.setReturnRef(result);
                });
    }

    public static void initOrdering(FluxonRuntime runtime) {
        runtime.registerExtension(Iterable.class)
                // 自然顺序升序排序
                .function("sorted", 0, (context) -> {
                    context.setReturnRef(sortElements(context, false, false));
                })
                // 自然顺序降序排序
                .function("sortedDescending", 0, (context) -> {
                    context.setReturnRef(sortElements(context, true, false));
                })
                // 根据选择器函数升序排序
                .function("sortedBy", 1, (context) -> {
                    context.setReturnRef(sortElements(context, false, true));
                })
                // 根据选择器函数降序排序
                .function("sortedDescendingBy", 1, (context) -> {
                    context.setReturnRef(sortElements(context, true, true));
                })
                // 反转顺序
                .function("reversed", 0, (context) -> {
                    List<Object> list = CollectionUtils.copyList((Iterable<Object>) Objects.requireNonNull(context.getTarget()));
                    Collections.reverse(list);
                    context.setReturnRef(list);
                })
                // 随机打乱顺序
                .function("shuffled", 0, (context) -> {
                    List<Object> list = CollectionUtils.copyList((Iterable<Object>) Objects.requireNonNull(context.getTarget()));
                    Collections.shuffle(list);
                    context.setReturnRef(list);
                });
    }

    public static void initOperations(FluxonRuntime runtime) {
        runtime.registerExtension(Iterable.class)
                // 取前 n 个元素
                .function("take", 1, (context) -> {
                    Iterable<Object> list = Objects.requireNonNull(context.getTarget());
                    int n = ((Number) context.getRef(0)).intValue();
                    // 如果 n <= 0 丢弃所有元素
                    if (n <= 0) {
                        context.setReturnRef(new ArrayList<>());
                        return;
                    }
                    List<Object> result = new ArrayList<>(n);
                    int count = 0;
                    for (Object object : list) {
                        if (count >= n) {
                            break;
                        }
                        result.add(object);
                        count++;
                    }
                    context.setReturnRef(result);
                })
                // 丢弃前 n 个元素
                .function("drop", 1, (context) -> {
                    Iterable<Object> list = Objects.requireNonNull(context.getTarget());
                    int n = ((Number) context.getRef(0)).intValue();
                    // 如果 n <= 0 保留所有元素
                    if (n <= 0) {
                        context.setReturnRef(list);
                        return;
                    }
                    List<Object> result = new ArrayList<>();
                    int count = 0;
                    for (Object object : list) {
                        if (count >= n) {
                            result.add(object);
                        }
                        count++;
                    }
                    context.setReturnRef(result);
                })
                // 取后 n 个元素
                .function("takeLast", 1, (context) -> {
                    Iterable<Object> iterable = Objects.requireNonNull(context.getTarget());
                    int n = ((Number) context.getRef(0)).intValue();
                    // 如果 n <= 0 丢弃所有元素
                    if (n <= 0) {
                        context.setReturnRef(new ArrayList<>());
                        return;
                    }
                    List<Object> list = new ArrayList<>();
                    for (Object object : iterable) {
                        list.add(object);
                    }
                    int size = list.size();
                    if (n >= size) {
                        context.setReturnRef(list);
                        return;
                    }
                    context.setReturnRef(list.subList(size - n, size));
                })
                // 丢弃后 n 个元素
                .function("dropLast", 1, (context) -> {
                    Iterable<Object> iterable = Objects.requireNonNull(context.getTarget());
                    int n = ((Number) context.getRef(0)).intValue();
                    // 如果 n <= 0 保留所有元素
                    if (n <= 0) {
                        context.setReturnRef(iterable);
                        return;
                    }
                    List<Object> list = new ArrayList<>();
                    for (Object object : iterable) {
                        list.add(object);
                    }
                    int size = list.size();
                    if (n >= size) {
                        context.setReturnRef(new ArrayList<>());
                        return;
                    }
                    context.setReturnRef(list.subList(0, size - n));
                })
                // 并集：合并两个集合，去重
                .function("union", 1, (context) -> {
                    Iterable<Object> first = Objects.requireNonNull(context.getTarget());
                    Iterable<Object> second = Objects.requireNonNull((Iterable<Object>) context.getRef(0));
                    Set<Object> result = toLinkedSet(first);
                    for (Object item : second) {
                        result.add(item);
                    }
                    context.setReturnRef(result);
                })
                // 交集：返回两个集合共有的元素
                .function("intersect", 1, (context) -> {
                    context.setReturnRef(filterBySet(context, true));
                })
                // 差集：从第一个集合中移除第二个集合的元素
                .function("subtract", 1, (context) -> {
                    context.setReturnRef(filterBySet(context, false));
                })
                // 去重：移除重复元素
                .function("distinct", 0, (context) -> {
                    context.setReturnRef(toLinkedSet(Objects.requireNonNull(context.getTarget())));
                })
                // 根据选择器函数去重
                .function("distinctBy", 1, (context) -> {
                    Iterable<Object> iterable = (Iterable<Object>) Objects.requireNonNull(context.getTarget());
                    Function selector = (Function) context.getRef(0);
                    Set<Object> seenKeys = new LinkedHashSet<>();
                    List<Object> result = new ArrayList<>();
                    FunctionContextPool pool = context.getPool();
                    try (FunctionContext<?> ctx = pool.borrowCopy(context, null)) {
                        for (Object item : iterable) {
                            ctx.updateRefs(item);
                            selector.call(ctx);
                            Object key = ctx.getReturnRef();
                            if (seenKeys.add(key)) {
                                result.add(item);
                            }
                        }
                        context.setReturnRef(result);
                    }
                });
    }

    /**
     * 辅助方法：将 Iterable 转换为 LinkedHashSet（保持插入顺序）
     *
     * @param iterable 可迭代对象
     * @return LinkedHashSet
     */
    static Set<Object> toLinkedSet(Iterable<Object> iterable) {
        Set<Object> set = new LinkedHashSet<>();
        for (Object item : iterable) set.add(item);
        return set;
    }

    /**
     * 辅助方法：根据集合成员关系过滤元素（交集/差集通用）
     *
     * @param context 函数上下文
     * @param retain  true=保留存在于第二个集合的元素（交集），false=保留不存在于第二个集合的元素（差集）
     * @return 过滤后的集合
     */
    static Set<Object> filterBySet(FunctionContext<?> context, boolean retain) {
        Iterable<Object> iterable = (Iterable<Object>) Objects.requireNonNull(context.getTarget());
        Iterable<Object> second = Objects.requireNonNull((Iterable<Object>) context.getRef(0));
        Set<Object> secondSet = toLinkedSet(second);
        Set<Object> result = new LinkedHashSet<>();
        for (Object item : iterable) {
            if (secondSet.contains(item) == retain) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 辅助方法：对集合进行排序
     *
     * @param context     函数上下文
     * @param descending  是否降序
     * @param useSelector 是否使用选择器函数（参数0）
     * @return 排序后的列表
     */
    @SuppressWarnings("unchecked")
    static List<Object> sortElements(FunctionContext<?> context, boolean descending, boolean useSelector) {
        List<Object> list = CollectionUtils.copyList((Iterable<Object>) Objects.requireNonNull(context.getTarget()));
        if (!useSelector) {
            // 直接比较元素
            list.sort((a, b) -> {
                if (a instanceof Comparable && b instanceof Comparable) {
                    int cmp = ((Comparable<Object>) a).compareTo(b);
                    return descending ? -cmp : cmp;
                }
                return 0;
            });
        } else {
            // 使用选择器函数
            Function selector = (Function) context.getRef(0);
            FunctionContextPool pool = context.getPool();
            try (FunctionContext<?> ctx = pool.borrowCopy(context, null)) {
                Map<Object, Object> keyCache = new HashMap<>();
                list.sort((a, b) -> {
                    Object keyA = keyCache.computeIfAbsent(a, k -> {
                        ctx.updateRefs(k);
                        selector.call(ctx);
                        return ctx.getReturnRef();
                    });
                    Object keyB = keyCache.computeIfAbsent(b, k -> {
                        ctx.updateRefs(k);
                        selector.call(ctx);
                        return ctx.getReturnRef();
                    });
                    if (keyA instanceof Comparable && keyB instanceof Comparable) {
                        int cmp = ((Comparable<Object>) keyA).compareTo(keyB);
                        return descending ? -cmp : cmp;
                    }
                    return 0;
                });
            }
        }
        return list;
    }
}
