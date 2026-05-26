package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonOperator;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.*;
import java.util.stream.Collectors;

public class ExtensionCollection {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionCollection.class);
    }

    // 获取列表大小
    @FluxonFunction(value = "size", target = Collection.class)
    public static int size(Collection<?> collection) {
        return Objects.requireNonNull(collection).size();
    }

    // 检查列表是否为空
    @FluxonFunction(value = "isEmpty", target = Collection.class)
    public static boolean isEmpty(Collection<?> collection) {
        return Objects.requireNonNull(collection).isEmpty();
    }

    // 检查是否包含某个元素
    @FluxonFunction(value = "contains", target = Collection.class)
    public static boolean contains(Collection<?> collection, Object element) {
        return Objects.requireNonNull(collection).contains(element);
    }

    // 转换为数组
    @FluxonFunction(value = "toArray", target = Collection.class)
    public static Object toArray(Collection<?> collection) {
        return Objects.requireNonNull(collection).toArray();
    }

    // 添加元素
    @FluxonFunction(value = "add", target = Collection.class)
    @SuppressWarnings("unchecked")
    public static boolean add(Collection<?> collection, Object element) {
        return ((Collection<Object>) Objects.requireNonNull(collection)).add(element);
    }

    // Collection += value 必须返回原集合，不能沿用 add() 的 boolean 返回值。
    @FluxonOperator(value = TokenType.PLUS_ASSIGN, target = Collection.class, returnsTarget = true)
    @SuppressWarnings("unchecked")
    public static Collection<?> plusAssign(Collection<?> collection, Object element) {
        Collection<Object> col = (Collection<Object>) Objects.requireNonNull(collection);
        if (element instanceof Collection) {
            col.addAll((Collection<?>) element);
        } else {
            col.add(element);
        }
        return col;
    }

    // 移除元素
    @FluxonFunction(value = "remove", target = Collection.class)
    public static boolean remove(Collection<?> collection, Object element) {
        return Objects.requireNonNull(collection).remove(element);
    }

    // Collection -= value 与内置减法保持一致，集合右值按批量移除处理。
    @FluxonOperator(value = TokenType.MINUS_ASSIGN, target = Collection.class, returnsTarget = true)
    @SuppressWarnings("unchecked")
    public static Collection<?> minusAssign(Collection<?> collection, Object element) {
        Collection<Object> col = (Collection<Object>) Objects.requireNonNull(collection);
        if (element instanceof Collection) {
            col.removeAll((Collection<?>) element);
        } else {
            col.remove(element);
        }
        return col;
    }

    // 添加所有元素
    @FluxonFunction(value = "addAll", target = Collection.class)
    @SuppressWarnings("unchecked")
    public static boolean addAll(Collection<?> collection, Object elements) {
        Collection<Object> col = (Collection<Object>) Objects.requireNonNull(collection);
        Collection<Object> other = (Collection<Object>) elements;
        if (other == null) {
            return false;
        }
        return col.addAll(other);
    }

    // 移除所有元素
    @FluxonFunction(value = "removeAll", target = Collection.class)
    @SuppressWarnings("unchecked")
    public static boolean removeAll(Collection<?> collection, Object elements) {
        Collection<Object> col = (Collection<Object>) Objects.requireNonNull(collection);
        Collection<Object> other = (Collection<Object>) elements;
        if (other == null) {
            return false;
        }
        return col.removeAll(other);
    }

    // 清空列表
    @FluxonFunction(value = "clear", target = Collection.class)
    public static void clear(Collection<?> collection) {
        Objects.requireNonNull(collection).clear();
    }

    // 转换为字符串（无分隔符）
    @FluxonFunction(value = "join", target = Collection.class)
    public static Object join0(Collection<?> collection) {
        return Objects.requireNonNull(collection).stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    // 转换为字符串（自定义分隔符）
    @FluxonFunction(value = "join", target = Collection.class)
    public static Object join1(Collection<?> collection, String delimiter) {
        if (delimiter == null) delimiter = ", ";
        return Objects.requireNonNull(collection).stream().map(Object::toString).collect(Collectors.joining(delimiter));
    }

    // 随机获取元素
    @FluxonFunction(value = "random", target = Collection.class)
    public static Object random0(Collection<?> collection) {
        Collection<?> col = Objects.requireNonNull(collection);
        if (col.isEmpty()) {
            return null;
        }
        List<?> tempList = new ArrayList<>(col);
        int index = (int) (Math.random() * tempList.size());
        return tempList.get(index);
    }

    // 随机获取多个元素
    @FluxonFunction(value = "random", target = Collection.class)
    public static Object random1(Collection<?> collection, int count) {
        Collection<?> col = Objects.requireNonNull(collection);
        if (col.isEmpty()) {
            return null;
        }
        if (count <= 0) {
            return null;
        }
        if (count >= col.size()) {
            List<Object> shuffled = new ArrayList<>((Collection<?>) col);
            Collections.shuffle(shuffled);
            return shuffled;
        }
        List<Object> copy = new ArrayList<>((Collection<?>) col);
        Collections.shuffle(copy);
        List<Object> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(copy.get(i));
        }
        return result;
    }
}
