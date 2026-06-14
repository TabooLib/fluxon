package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.collection.ImmutableMap;
import org.tabooproject.fluxon.runtime.stdlib.Operations;
import org.tabooproject.fluxon.util.CollectionUtils;

import java.util.*;

import static org.tabooproject.fluxon.runtime.ExtensionBuilder.*;

@SuppressWarnings("unchecked")
public class ExtensionIterable {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionIterable.class);
    }

    // 直接遍历并对每个元素应用 Fluxon 函数
    @FluxonFunction(value = "each", target = Iterable.class)
    public static Object each(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        forEachElement(context, null);
        return context.getTarget();
    }

    // 对每个元素应用 Fluxon 函数并收集返回值
    @FluxonFunction(value = "map", target = Iterable.class)
    public static List<Object> map(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        List<Object> result = new ArrayList<>();
        forEachElement(context, (element, callResult) -> result.add(callResult));
        return result;
    }

    // 对每个元素应用函数并将结果展平为列表
    @FluxonFunction(value = "flatMap", target = Iterable.class)
    public static List<Object> flatMap(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        List<Object> result = new ArrayList<>();
        forEachElement(context, (element, callResult) -> {
            if (callResult instanceof Collection) {
                result.addAll((Collection<?>) callResult);
            } else if (callResult instanceof Iterable) {
                for (Object item : (Iterable<?>) callResult) {
                    result.add(item);
                }
            } else {
                result.add(callResult);
            }
        });
        return result;
    }

    // 保留谓词为真的元素
    @FluxonFunction(value = "filter", target = Iterable.class)
    public static List<Object> filter(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        List<Object> result = new ArrayList<>();
        forEachElement(context, (element, callResult) -> {
            if (Operations.isTrue(callResult)) {
                result.add(element);
            }
        });
        return result;
    }

    // 以选择器返回值为键、元素为值构建映射
    @FluxonFunction(value = "associateBy", target = Iterable.class)
    public static Map<Object, Object> associateBy(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        Map<Object, Object> result = new HashMap<>();
        forEachElement(context, (element, callResult) -> result.put(callResult, element));
        return result;
    }

    // 以元素为键、选择器返回值为值构建映射
    @FluxonFunction(value = "associateWith", target = Iterable.class)
    public static Map<Object, Object> associateWith(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        Map<Object, Object> result = new HashMap<>();
        forEachElement(context, result::put);
        return result;
    }

    // 按选择器返回值分组
    @FluxonFunction(value = "groupBy", target = Iterable.class)
    public static Map<Object, List<Object>> groupBy(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        Map<Object, List<Object>> result = new HashMap<>();
        forEachElement(context, (element, callResult) -> {
            result.computeIfAbsent(callResult, k -> new ArrayList<>()).add(element);
        });
        return result;
    }

    // 按谓词将元素分为 true / false 两组
    @FluxonFunction(value = "partition", target = Iterable.class)
    public static Object partition(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        List<Object> matched = new ArrayList<>();
        List<Object> unmatched = new ArrayList<>();
        forEachElement(context, (element, callResult) -> {
            if (Operations.isTrue(callResult)) {
                matched.add(element);
            } else {
                unmatched.add(element);
            }
        });
        return ImmutableMap.of(true, matched, false, unmatched);
    }

    // 按选择器返回值去重，保留首次出现的元素
    @FluxonFunction(value = "distinctBy", target = Iterable.class)
    public static List<Object> distinctBy(Iterable<?> iterable, FunctionContext<?> context, Function selector) {
        Iterable<Object> target = (Iterable<Object>) Objects.requireNonNull(context.getTarget());
        Set<Object> seenKeys = new LinkedHashSet<>();
        List<Object> result = new ArrayList<>();
        FunctionContextPool pool = context.getPool();
        try (FunctionContext<?> ctx = pool.borrowCopy(context, null)) {
            for (Object item : target) {
                ctx.updateRefs(item);
                selector.call(ctx);
                Object key = ctx.getReturnRef();
                if (seenKeys.add(key)) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    // 是否存在任意元素使谓词为真
    @FluxonFunction(value = "any", target = Iterable.class)
    public static boolean any(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        return !testElements(context, (element, callResult) -> !Operations.isTrue(callResult));
    }

    // 是否所有元素均使谓词为真
    @FluxonFunction(value = "all", target = Iterable.class)
    public static boolean all(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        return testElements(context, (element, callResult) -> Operations.isTrue(callResult));
    }

    // 是否没有元素使谓词为真
    @FluxonFunction(value = "none", target = Iterable.class)
    public static boolean none(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        return testElements(context, (element, callResult) -> !Operations.isTrue(callResult));
    }

    // 返回第一个使谓词为真的元素
    @FluxonFunction(value = "find", target = Iterable.class)
    public static Object find(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        Object[] found = new Object[1];
        testElements(context, (element, callResult) -> {
            if (Operations.isTrue(callResult)) {
                found[0] = element;
                return false;
            }
            return true;
        });
        return found[0];
    }

    // 统计使谓词为真的元素个数
    @FluxonFunction(value = "countOf", target = Iterable.class)
    public static int countOf(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        int[] count = new int[1];
        forEachElement(context, (element, callResult) -> {
            if (Operations.isTrue(callResult)) {
                count[0]++;
            }
        });
        return count[0];
    }

    // 对每个元素应用函数并将数值结果求和
    @FluxonFunction(value = "sumOf", target = Iterable.class)
    public static double sumOf(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        double[] sum = new double[1];
        forEachElement(context, (element, callResult) -> {
            if (callResult instanceof Number) {
                sum[0] += ((Number) callResult).doubleValue();
            }
        });
        return sum[0];
    }

    // 对每个元素应用函数后取可比最小值
    @FluxonFunction(value = "minOf", target = Iterable.class)
    public static Object minOf(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        return compareElements(context, (current, candidate) ->
                ((Comparable<Object>) candidate).compareTo(current) < 0
        );
    }

    // 对每个元素应用函数后取可比最大值
    @FluxonFunction(value = "maxOf", target = Iterable.class)
    public static Object maxOf(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        return compareElements(context, (current, candidate) ->
                ((Comparable<Object>) candidate).compareTo(current) > 0
        );
    }

    // 按选择器返回值取最小值对应的原始元素
    @FluxonFunction(value = "minBy", target = Iterable.class)
    public static Object minBy(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        return compareElementsBy(context, (current, candidate) ->
                ((Comparable<Object>) candidate).compareTo(current) < 0
        );
    }

    // 按选择器返回值取最大值对应的原始元素
    @FluxonFunction(value = "maxBy", target = Iterable.class)
    public static Object maxBy(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        return compareElementsBy(context, (current, candidate) ->
                ((Comparable<Object>) candidate).compareTo(current) > 0
        );
    }

    // 按元素自然顺序升序排序
    @FluxonFunction(value = "sorted", target = Iterable.class)
    public static List<Object> sorted(Iterable<?> iterable, FunctionContext<?> context) {
        return sortElements(context, false, false);
    }

    // 按元素自然顺序降序排序
    @FluxonFunction(value = "sortedDescending", target = Iterable.class)
    public static List<Object> sortedDescending(Iterable<?> iterable, FunctionContext<?> context) {
        return sortElements(context, true, false);
    }

    // 按选择器返回值升序排序
    @FluxonFunction(value = "sortedBy", target = Iterable.class)
    public static List<Object> sortedBy(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        return sortElements(context, false, true);
    }

    // 按选择器返回值降序排序
    @FluxonFunction(value = "sortedDescendingBy", target = Iterable.class)
    public static List<Object> sortedDescendingBy(Iterable<?> iterable, FunctionContext<?> context, Function closure) {
        return sortElements(context, true, true);
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
