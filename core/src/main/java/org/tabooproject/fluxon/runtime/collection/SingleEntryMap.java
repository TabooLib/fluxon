package org.tabooproject.fluxon.runtime.collection;

import java.util.*;

/**
 * 单条目 Map 实现，用于优化单变量场景
 * 避免 LinkedHashMap 的开销，同时保持 Map 接口兼容性
 */
public final class SingleEntryMap<K, V> extends AbstractMap<K, V> {

    private final K key;
    private final V value;
    private Set<Entry<K, V>> entrySet;

    public SingleEntryMap(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * 获取唯一的键
     */
    public K getKey() {
        return key;
    }

    /**
     * 获取唯一的值
     */
    public V getValue() {
        return value;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object k) {
        return Objects.equals(key, k);
    }

    @Override
    public boolean containsValue(Object v) {
        return Objects.equals(value, v);
    }

    @Override
    public V get(Object k) {
        return Objects.equals(key, k) ? value : null;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = Collections.singleton(new SimpleImmutableEntry<>(key, value));
        }
        return entrySet;
    }

    @Override
    public Set<K> keySet() {
        return Collections.singleton(key);
    }

    @Override
    public Collection<V> values() {
        return Collections.singletonList(value);
    }
}
