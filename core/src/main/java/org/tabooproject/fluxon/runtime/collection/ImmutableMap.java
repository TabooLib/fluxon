package org.tabooproject.fluxon.runtime.collection;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * 轻量不可变 Map，使用并行数组存储键和值，兼容 Java 8。
 */
public final class ImmutableMap extends AbstractMap<Object, Object> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final ImmutableMap EMPTY = new ImmutableMap(new Object[0], new Object[0]);

    private final Object[] keys;
    private final Object[] values;

    private ImmutableMap(Object[] keys, Object[] values) {
        this.keys = keys;
        this.values = values;
    }

    public static ImmutableMap empty() {
        return EMPTY;
    }

    public static ImmutableMap of(Object[] keys, Object[] values) {
        if (keys.length == 0) {
            return EMPTY;
        }
        return new ImmutableMap(keys, values);
    }

    @Override
    public Object get(Object key) {
        for (int i = 0; i < keys.length; i++) {
            if (Objects.equals(keys[i], key)) {
                return values[i];
            }
        }
        return null;
    }

    @Override
    public int size() {
        return keys.length;
    }

    @Override
    public boolean containsKey(Object key) {
        for (Object k : keys) {
            if (Objects.equals(k, key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Object v : values) {
            if (Objects.equals(v, value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return new EntrySet();
    }

    private final class EntrySet extends AbstractSet<Entry<Object, Object>> {

        @Override
        public Iterator<Entry<Object, Object>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return keys.length;
        }
    }

    private final class EntryIterator implements Iterator<Entry<Object, Object>> {

        private int index;

        @Override
        public boolean hasNext() {
            return index < keys.length;
        }

        @Override
        public Entry<Object, Object> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Entry<Object, Object> entry = new SimpleEntry<>(keys[index], values[index]);
            index++;
            return entry;
        }
    }
}
