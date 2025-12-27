package org.tabooproject.fluxon.runtime.collection;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * 轻量不可变 Map，支持 inline 模式（最多 4 个键值对）和数组模式。
 * <p>
 * Inline 模式下键值对直接存储在字段中，避免数组分配开销。
 */
public final class ImmutableMap extends AbstractMap<Object, Object> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final ImmutableMap EMPTY = new ImmutableMap();

    // Inline 槽位（最多支持 4 个键值对）
    private final Object k0, v0, k1, v1, k2, v2, k3, v3;
    
    // 元素数量（inline 模式使用负数表示，-1 到 -4 表示 1-4 个键值对）
    // 正数或 0 表示数组模式，值为数组长度
    private final int size;
    
    // 数组模式时的存储（inline 模式时为 null）
    private final Object[] keys;
    private final Object[] values;

    // 空 Map 构造
    private ImmutableMap() {
        this.k0 = this.v0 = this.k1 = this.v1 = this.k2 = this.v2 = this.k3 = this.v3 = null;
        this.size = 0;
        this.keys = null;
        this.values = null;
    }

    // Inline 模式构造（1-4 个键值对）
    private ImmutableMap(int inlineSize, Object k0, Object v0, Object k1, Object v1, Object k2, Object v2, Object k3, Object v3) {
        this.k0 = k0; this.v0 = v0;
        this.k1 = k1; this.v1 = v1;
        this.k2 = k2; this.v2 = v2;
        this.k3 = k3; this.v3 = v3;
        this.size = -inlineSize; // 负数表示 inline 模式
        this.keys = null;
        this.values = null;
    }

    // 数组模式构造
    private ImmutableMap(Object[] keys, Object[] values) {
        this.k0 = this.v0 = this.k1 = this.v1 = this.k2 = this.v2 = this.k3 = this.v3 = null;
        this.size = keys.length;
        this.keys = keys;
        this.values = values;
    }

    // ==================== 工厂方法 ====================

    public static ImmutableMap empty() {
        return EMPTY;
    }

    public static ImmutableMap of(Object k0, Object v0) {
        return new ImmutableMap(1, k0, v0, null, null, null, null, null, null);
    }

    public static ImmutableMap of(Object k0, Object v0, Object k1, Object v1) {
        return new ImmutableMap(2, k0, v0, k1, v1, null, null, null, null);
    }

    public static ImmutableMap of(Object k0, Object v0, Object k1, Object v1, Object k2, Object v2) {
        return new ImmutableMap(3, k0, v0, k1, v1, k2, v2, null, null);
    }

    public static ImmutableMap of(Object k0, Object v0, Object k1, Object v1, Object k2, Object v2, Object k3, Object v3) {
        return new ImmutableMap(4, k0, v0, k1, v1, k2, v2, k3, v3);
    }

    /**
     * 从并行数组创建，自动选择 inline 或数组模式
     */
    public static ImmutableMap of(Object[] keys, Object[] values) {
        switch (keys.length) {
            case 0: return EMPTY;
            case 1: return new ImmutableMap(1, keys[0], values[0], null, null, null, null, null, null);
            case 2: return new ImmutableMap(2, keys[0], values[0], keys[1], values[1], null, null, null, null);
            case 3: return new ImmutableMap(3, keys[0], values[0], keys[1], values[1], keys[2], values[2], null, null);
            case 4: return new ImmutableMap(4, keys[0], values[0], keys[1], values[1], keys[2], values[2], keys[3], values[3]);
            default: return new ImmutableMap(keys, values);
        }
    }

    // ==================== Map 接口实现 ====================

    @Override
    public Object get(Object key) {
        if (size < 0) {
            // Inline 模式
            int inlineSize = -size;
            if (inlineSize >= 1 && Objects.equals(k0, key)) return v0;
            if (inlineSize >= 2 && Objects.equals(k1, key)) return v1;
            if (inlineSize >= 3 && Objects.equals(k2, key)) return v2;
            if (inlineSize >= 4 && Objects.equals(k3, key)) return v3;
            return null;
        } else {
            // 数组模式
            if (keys == null) return null;
            for (int i = 0; i < size; i++) {
                if (Objects.equals(keys[i], key)) {
                    return values[i];
                }
            }
            return null;
        }
    }

    @Override
    public int size() {
        return size < 0 ? -size : size;
    }

    @Override
    public boolean containsKey(Object key) {
        if (size < 0) {
            // Inline 模式
            int inlineSize = -size;
            if (inlineSize >= 1 && Objects.equals(k0, key)) return true;
            if (inlineSize >= 2 && Objects.equals(k1, key)) return true;
            if (inlineSize >= 3 && Objects.equals(k2, key)) return true;
            if (inlineSize >= 4 && Objects.equals(k3, key)) return true;
            return false;
        } else {
            // 数组模式
            if (keys == null) return false;
            for (Object k : keys) {
                if (Objects.equals(k, key)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public boolean containsValue(Object value) {
        if (size < 0) {
            // Inline 模式
            int inlineSize = -size;
            if (inlineSize >= 1 && Objects.equals(v0, value)) return true;
            if (inlineSize >= 2 && Objects.equals(v1, value)) return true;
            if (inlineSize >= 3 && Objects.equals(v2, value)) return true;
            if (inlineSize >= 4 && Objects.equals(v3, value)) return true;
            return false;
        } else {
            // 数组模式
            if (values == null) return false;
            for (Object v : values) {
                if (Objects.equals(v, value)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return new EntrySet();
    }

    // ==================== 内部类 ====================

    private Object getKeyAt(int index) {
        if (size < 0) {
            switch (index) {
                case 0: return k0;
                case 1: return k1;
                case 2: return k2;
                case 3: return k3;
                default: throw new IndexOutOfBoundsException();
            }
        }
        return keys[index];
    }

    private Object getValueAt(int index) {
        if (size < 0) {
            switch (index) {
                case 0: return v0;
                case 1: return v1;
                case 2: return v2;
                case 3: return v3;
                default: throw new IndexOutOfBoundsException();
            }
        }
        return values[index];
    }

    private final class EntrySet extends AbstractSet<Entry<Object, Object>> {

        @Override
        public Iterator<Entry<Object, Object>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return ImmutableMap.this.size();
        }
    }

    private final class EntryIterator implements Iterator<Entry<Object, Object>> {

        private int index;
        private final int total = size();

        @Override
        public boolean hasNext() {
            return index < total;
        }

        @Override
        public Entry<Object, Object> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Entry<Object, Object> entry = new SimpleEntry<>(getKeyAt(index), getValueAt(index));
            index++;
            return entry;
        }
    }
}
