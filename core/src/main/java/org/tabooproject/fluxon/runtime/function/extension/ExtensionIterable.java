package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.collection.ImmutableMap;
import org.tabooproject.fluxon.runtime.stdlib.Operations;
import org.tabooproject.fluxon.util.CollectionUtils;

import java.util.*;

import static org.tabooproject.fluxon.runtime.ExtensionBuilder.*;
import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

@SuppressWarnings("unchecked")
public class ExtensionIterable {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionIterable.class);
        initHigherOrder(runtime);
        initQuerying(runtime);
        initOrdering(runtime);
    }

    // 高阶变换：遍历、映射、过滤、关联
    private static void initHigherOrder(FluxonRuntime runtime) {
        runtime.registerExtension(Iterable.class)
                // 直接遍历
                .function("each", returns(Type.OBJECT).params(Function.TYPE), (context) -> {
                    forEachElement(context, null);
                    context.setReturnRef(context.getTarget());
                })
                // 对每个元素应用函数
                .function("map", returns(Type.LIST).params(Function.TYPE), (context) -> {
                    List<Object> result = new ArrayList<>();
                    forEachElement(context, (element, callResult) -> result.add(callResult));
                    context.setReturnRef(result);
                })
                // 对每个元素应用函数并展平结果
                .function("flatMap", returns(Type.LIST).params(Function.TYPE), (context) -> {
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
                // 过滤元素
                .function("filter", returns(Type.LIST).params(Function.TYPE), (context) -> {
                    List<Object> result = new ArrayList<>();
                    forEachElement(context, (element, callResult) -> {
                        if (Operations.isTrue(callResult)) result.add(element);
                    });
                    context.setReturnRef(result);
                })
                // 根据键函数创建映射，元素作为值
                .function("associateBy", returns(Type.MAP).params(Function.TYPE), (context) -> {
                    Map<Object, Object> result = new HashMap<>();
                    forEachElement(context, (element, callResult) -> result.put(callResult, element));
                    context.setReturnRef(result);
                })
                // 根据值函数创建映射，元素作为键
                .function("associateWith", returns(Type.MAP).params(Function.TYPE), (context) -> {
                    Map<Object, Object> result = new HashMap<>();
                    forEachElement(context, result::put);
                    context.setReturnRef(result);
                })
                // 分组元素
                .function("groupBy", returns(Type.OBJECT).params(Function.TYPE), (context) -> {
                    Map<Object, List<Object>> result = new HashMap<>();
                    forEachElement(context, (element, callResult) -> {
                        result.computeIfAbsent(callResult, k -> new ArrayList<>()).add(element);
                    });
                    context.setReturnRef(result);
                })
                // 根据断言将元素分为两组
                .function("partition", returns(Type.OBJECT).params(Function.TYPE), (context) -> {
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
                // 根据选择器函数去重
                .function("distinctBy", returns(Type.LIST).params(Function.TYPE), (context) -> {
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

    // 查询与聚合：条件检查、查找、统计、极值
    private static void initQuerying(FluxonRuntime runtime) {
        runtime.registerExtension(Iterable.class)
                // 检查是否有任意元素满足条件
                .function("any", returns(Type.Z).params(Function.TYPE), (context) -> {
                    context.setReturnBool(!testElements(context, (element, callResult) -> !Operations.isTrue(callResult)));
                })
                // 检查是否所有元素都满足条件
                .function("all", returns(Type.Z).params(Function.TYPE), (context) -> {
                    context.setReturnBool(testElements(context, (element, callResult) -> Operations.isTrue(callResult)));
                })
                // 检查是否没有元素满足条件
                .function("none", returns(Type.Z).params(Function.TYPE), (context) -> {
                    context.setReturnBool(testElements(context, (element, callResult) -> !Operations.isTrue(callResult)));
                })
                // 查找第一个满足条件的元素
                .function("find", returns(Type.OBJECT).params(Function.TYPE), (context) -> {
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
                // 统计满足条件的元素数量
                .function("countOf", returns(Type.I).params(Function.TYPE), (context) -> {
                    int[] count = new int[1];
                    forEachElement(context, (element, callResult) -> {
                        if (Operations.isTrue(callResult)) count[0]++;
                    });
                    context.setReturnInt(count[0]);
                })
                // 对每个元素应用函数并求和
                .function("sumOf", returns(Type.D).params(Function.TYPE), (context) -> {
                    double[] sum = new double[1];
                    forEachElement(context, (element, callResult) -> {
                        if (callResult instanceof Number) {
                            sum[0] += ((Number) callResult).doubleValue();
                        }
                    });
                    context.setReturnDouble(sum[0]);
                })
                // 对每个元素应用函数并求最小值
                .function("minOf", returns(Type.OBJECT).params(Function.TYPE), (context) -> {
                    context.setReturnRef(compareElements(context, (current, candidate) ->
                            ((Comparable<Object>) candidate).compareTo(current) < 0
                    ));
                })
                // 对每个元素应用函数并求最大值
                .function("maxOf", returns(Type.OBJECT).params(Function.TYPE), (context) -> {
                    context.setReturnRef(compareElements(context, (current, candidate) ->
                            ((Comparable<Object>) candidate).compareTo(current) > 0
                    ));
                })
                // 根据选择器函数找到最小值对应的元素
                .function("minBy", returns(Type.OBJECT).params(Function.TYPE), (context) -> {
                    context.setReturnRef(compareElementsBy(context, (current, candidate) ->
                            ((Comparable<Object>) candidate).compareTo(current) < 0
                    ));
                })
                // 根据选择器函数找到最大值对应的元素
                .function("maxBy", returns(Type.OBJECT).params(Function.TYPE), (context) -> {
                    context.setReturnRef(compareElementsBy(context, (current, candidate) ->
                            ((Comparable<Object>) candidate).compareTo(current) > 0
                    ));
                });
    }

    // 排序
    private static void initOrdering(FluxonRuntime runtime) {
        runtime.registerExtension(Iterable.class)
                // 自然顺序升序排序
                .function("sorted", returns(Type.LIST).noParams(), (context) -> {
                    context.setReturnRef(sortElements(context, false, false));
                })
                // 自然顺序降序排序
                .function("sortedDescending", returns(Type.LIST).noParams(), (context) -> {
                    context.setReturnRef(sortElements(context, true, false));
                })
                // 根据选择器函数升序排序
                .function("sortedBy", returns(Type.LIST).params(Function.TYPE), (context) -> {
                    context.setReturnRef(sortElements(context, false, true));
                })
                // 根据选择器函数降序排序
                .function("sortedDescendingBy", returns(Type.LIST).params(Function.TYPE), (context) -> {
                    context.setReturnRef(sortElements(context, true, true));
                });
    }

    // 取第一个元素
    @FluxonFunction(value = "first", target = Iterable.class)
    public static Object first(Iterable<?> iterable) {
        Objects.requireNonNull(iterable);
        if (iterable instanceof List) {
            List<?> list = (List<?>) iterable;
            return list.isEmpty() ? null : list.get(0);
        }
        return iterable.iterator().next();
    }

    // 取最后一个元素
    @FluxonFunction(value = "last", target = Iterable.class)
    public static Object last(Iterable<?> iterable) {
        Objects.requireNonNull(iterable);
        if (iterable instanceof List) {
            List<?> list = (List<?>) iterable;
            return list.isEmpty() ? null : list.get(list.size() - 1);
        }
        Iterator<?> iterator = iterable.iterator();
        Object last = null;
        while (iterator.hasNext()) {
            last = iterator.next();
        }
        return last;
    }

    // 反转顺序
    @FluxonFunction(value = "reversed", target = Iterable.class)
    public static List<Object> reversed(Iterable<?> iterable) {
        List<Object> list = CollectionUtils.copyList((Iterable<Object>) Objects.requireNonNull(iterable));
        Collections.reverse(list);
        return list;
    }

    // 随机打乱顺序
    @FluxonFunction(value = "shuffled", target = Iterable.class)
    public static List<Object> shuffled(Iterable<?> iterable) {
        List<Object> list = CollectionUtils.copyList((Iterable<Object>) Objects.requireNonNull(iterable));
        Collections.shuffle(list);
        return list;
    }

    // 去重：移除重复元素
    @FluxonFunction(value = "distinct", target = Iterable.class)
    public static Object distinct(Iterable<?> iterable) {
        return toLinkedSet((Iterable<Object>) Objects.requireNonNull(iterable));
    }

    // 将元素按指定大小分块
    @FluxonFunction(value = "chunked", target = Iterable.class)
    public static Object chunked(Iterable<?> iterable, int size) {
        Objects.requireNonNull(iterable);
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
        return result;
    }

    // 取前 n 个元素
    @FluxonFunction(value = "take", target = Iterable.class)
    public static Object take(Iterable<?> iterable, int n) {
        Objects.requireNonNull(iterable);
        if (n <= 0) {
            return new ArrayList<>();
        }
        List<Object> result = new ArrayList<>(n);
        int count = 0;
        for (Object object : iterable) {
            if (count >= n) break;
            result.add(object);
            count++;
        }
        return result;
    }

    // 丢弃前 n 个元素
    @FluxonFunction(value = "drop", target = Iterable.class)
    public static Object drop(Iterable<?> iterable, int n) {
        Objects.requireNonNull(iterable);
        if (n <= 0) {
            return iterable;
        }
        List<Object> result = new ArrayList<>();
        int count = 0;
        for (Object object : iterable) {
            if (count >= n) {
                result.add(object);
            }
            count++;
        }
        return result;
    }

    // 取后 n 个元素
    @FluxonFunction(value = "takeLast", target = Iterable.class)
    public static Object takeLast(Iterable<?> iterable, int n) {
        Objects.requireNonNull(iterable);
        if (n <= 0) {
            return new ArrayList<>();
        }
        List<Object> list = new ArrayList<>();
        for (Object object : iterable) {
            list.add(object);
        }
        int size = list.size();
        if (n >= size) {
            return list;
        }
        return list.subList(size - n, size);
    }

    // 丢弃后 n 个元素
    @FluxonFunction(value = "dropLast", target = Iterable.class)
    public static Object dropLast(Iterable<?> iterable, int n) {
        Objects.requireNonNull(iterable);
        if (n <= 0) {
            return iterable;
        }
        List<Object> list = new ArrayList<>();
        for (Object object : iterable) {
            list.add(object);
        }
        int size = list.size();
        if (n >= size) {
            return new ArrayList<>();
        }
        return list.subList(0, size - n);
    }

    // 并集：合并两个集合，去重
    @FluxonFunction(value = "union", target = Iterable.class)
    public static Object union(Iterable<?> first, List<?> second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Set<Object> result = toLinkedSet((Iterable<Object>) first);
        for (Object item : second) {
            result.add(item);
        }
        return result;
    }

    // 交集：返回两个集合共有的元素
    @FluxonFunction(value = "intersect", target = Iterable.class)
    public static Object intersect(Iterable<?> first, List<?> second) {
        Objects.requireNonNull(first);
        Set<Object> secondSet = toLinkedSet((Iterable<Object>) second);
        Set<Object> result = new LinkedHashSet<>();
        for (Object item : first) {
            if (secondSet.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    // 差集：从第一个集合中移除第二个集合的元素
    @FluxonFunction(value = "subtract", target = Iterable.class)
    public static Object subtract(Iterable<?> first, List<?> second) {
        Objects.requireNonNull(first);
        Set<Object> secondSet = toLinkedSet((Iterable<Object>) second);
        Set<Object> result = new LinkedHashSet<>();
        for (Object item : first) {
            if (!secondSet.contains(item)) {
                result.add(item);
            }
        }
        return result;
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
