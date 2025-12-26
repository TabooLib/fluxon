package org.tabooproject.fluxon.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 泛型方法测试类
 *
 * @author sky
 */
public class TestGeneric<T> {

    public T value;
    private List<T> items = new ArrayList<>();

    public TestGeneric() {
    }

    public TestGeneric(T value) {
        this.value = value;
    }

    // ========== 泛型字段访问 ==========
    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public List<T> getItems() {
        return items;
    }

    /**
     * 转换值为大写（如果是String的话）
     */
    @SuppressWarnings("unchecked")
    public String transformValue() {
        if (value instanceof String) {
            return ((String) value).toUpperCase();
        }
        return String.valueOf(value);
    }

    // ========== 实例泛型方法 ==========
    
    /**
     * 返回传入的值（实例版本的identity）
     */
    public <E> E identity(E val) {
        return val;
    }

    /**
     * 将值包装为单元素列表
     */
    public <E> List<E> wrap(E val) {
        List<E> list = new ArrayList<>();
        list.add(val);
        return list;
    }

    /**
     * 创建键值对Map
     */
    public <K, V> Map<K, V> pair(K key, V val) {
        Map<K, V> map = new HashMap<>();
        map.put(key, val);
        return map;
    }

    /**
     * 合并两个值为列表
     */
    public <E> List<E> merge(E first, E second) {
        List<E> list = new ArrayList<>();
        list.add(first);
        list.add(second);
        return list;
    }

    /**
     * 有界泛型方法 - 返回数值的double值
     */
    public <N extends Number> double toDouble(N num) {
        return num.doubleValue();
    }

    // ========== 函数式接口方法 ==========
    public <R> R transform(Function<T, R> func) {
        return func.apply(value);
    }

    public boolean test(Predicate<T> predicate) {
        return predicate.test(value);
    }

    public T supply(Supplier<T> supplier) {
        return supplier.get();
    }

    // ========== 静态泛型方法 ==========
    @SuppressWarnings("unchecked")
    public static <E> List<E> createList(E... elements) {
        List<E> list = new ArrayList<>();
        for (E e : elements) {
            list.add(e);
        }
        return list;
    }

    public static <K, V> Map<K, V> createMap(K key, V value) {
        Map<K, V> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    // ========== 有界泛型 ==========
    public static <N extends Number> double sumNumbers(List<N> numbers) {
        double sum = 0;
        for (N n : numbers) {
            sum += n.doubleValue();
        }
        return sum;
    }

    public static <S extends CharSequence> int totalLength(List<S> sequences) {
        int total = 0;
        for (S s : sequences) {
            total += s.length();
        }
        return total;
    }

    // ========== 返回泛型集合 ==========
    
    /**
     * 返回字符串列表（用于测试）
     */
    public List<String> getList() {
        List<String> list = new ArrayList<>();
        list.add("item1");
        list.add("item2");
        list.add("item3");
        return list;
    }

    /**
     * 返回字符串-整数映射（用于测试）
     */
    public Map<String, Integer> getMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 1);
        map.put("key2", 2);
        return map;
    }

    /**
     * 返回自身（用于链式调用测试）
     */
    public TestGeneric<T> getSelf() {
        return this;
    }

    /**
     * varargs版本的sumNumbers（实例方法）
     */
    @SafeVarargs
    public final <N extends Number> double sumNumbers(N... numbers) {
        double sum = 0;
        for (N n : numbers) {
            sum += n.doubleValue();
        }
        return sum;
    }

    /**
     * 返回两个Comparable值中的较大者
     */
    @SuppressWarnings("unchecked")
    public <C extends Comparable<C>> C max(C a, C b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public List<String> getStringList() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        return list;
    }

    public Map<String, Integer> getStringIntMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        return map;
    }

    // ========== 泛型参数 ==========
    public String processGenericList(List<String> list) {
        return "list-size:" + list.size();
    }

    public String processGenericMap(Map<String, Integer> map) {
        return "map-size:" + map.size();
    }

    // ========== 通配符 ==========
    public int countWildcard(List<?> list) {
        return list.size();
    }

    public double sumExtends(List<? extends Number> numbers) {
        double sum = 0;
        for (Number n : numbers) {
            sum += n.doubleValue();
        }
        return sum;
    }

    public void addSuper(List<? super Integer> list, Integer value) {
        list.add(value);
    }
}
