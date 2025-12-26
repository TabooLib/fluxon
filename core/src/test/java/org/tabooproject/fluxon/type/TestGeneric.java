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

    // ========== 泛型方法 ==========
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

    public static <E> E identity(E value) {
        return value;
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
