package org.tabooproject.fluxon.runtime.collection;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

/**
 * 简单的不可变列表实现，支持 inline 模式（最多 4 个元素）和数组模式。
 * <p>
 * Inline 模式下元素直接存储在字段中，避免数组分配开销。
 */
@SuppressWarnings("unchecked")
public final class ImmutableList<E> extends AbstractList<E> implements RandomAccess, Serializable {

    private static final long serialVersionUID = 1L;

    private static final ImmutableList<?> EMPTY = new ImmutableList<>();

    // Inline 槽位（最多支持 4 个元素）
    private final Object e0, e1, e2, e3;
    
    // 元素数量（inline 模式使用负数表示，-1 到 -4 表示 1-4 个元素）
    // 正数或 0 表示数组模式，值为数组长度
    private final int size;
    
    // 数组模式时的存储（inline 模式时为 null）
    private final Object[] elements;

    // 空列表构造
    private ImmutableList() {
        this.e0 = this.e1 = this.e2 = this.e3 = null;
        this.size = 0;
        this.elements = null;
    }

    // Inline 模式构造（1-4 个元素）
    private ImmutableList(int inlineSize, Object e0, Object e1, Object e2, Object e3) {
        this.e0 = e0; this.e1 = e1; this.e2 = e2; this.e3 = e3;
        this.size = -inlineSize; // 负数表示 inline 模式
        this.elements = null;
    }

    // 数组模式构造
    private ImmutableList(Object[] elements) {
        this.e0 = this.e1 = this.e2 = this.e3 = null;
        this.size = elements.length;
        this.elements = elements;
    }

    // ==================== 工厂方法 ====================

    public static <E> ImmutableList<E> empty() {
        return (ImmutableList<E>) EMPTY;
    }

    public static <E> ImmutableList<E> of(E e0) {
        return new ImmutableList<>(1, e0, null, null, null);
    }

    public static <E> ImmutableList<E> of(E e0, E e1) {
        return new ImmutableList<>(2, e0, e1, null, null);
    }

    public static <E> ImmutableList<E> of(E e0, E e1, E e2) {
        return new ImmutableList<>(3, e0, e1, e2, null);
    }

    public static <E> ImmutableList<E> of(E e0, E e1, E e2, E e3) {
        return new ImmutableList<>(4, e0, e1, e2, e3);
    }

    /**
     * 从数组创建，自动选择 inline 或数组模式
     */
    public static <E> ImmutableList<E> of(Object[] elements) {
        switch (elements.length) {
            case 0: return (ImmutableList<E>) EMPTY;
            case 1: return new ImmutableList<>(1, elements[0], null, null, null);
            case 2: return new ImmutableList<>(2, elements[0], elements[1], null, null);
            case 3: return new ImmutableList<>(3, elements[0], elements[1], elements[2], null);
            case 4: return new ImmutableList<>(4, elements[0], elements[1], elements[2], elements[3]);
            default: return new ImmutableList<>(elements);
        }
    }

    // ==================== List 接口实现 ====================

    @Override
    public E get(int index) {
        if (size < 0) {
            // Inline 模式
            int inlineSize = -size;
            if (index < 0 || index >= inlineSize) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + inlineSize);
            }
            switch (index) {
                case 0: return (E) e0;
                case 1: return (E) e1;
                case 2: return (E) e2;
                case 3: return (E) e3;
                default: throw new IndexOutOfBoundsException("Index: " + index);
            }
        } else {
            // 数组模式
            if (elements == null || index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
            }
            return (E) elements[index];
        }
    }

    @Override
    public int size() {
        return size < 0 ? -size : size;
    }

    @Override
    public Object @NotNull [] toArray() {
        int actualSize = size();
        Object[] result = new Object[actualSize];
        if (size < 0) {
            // Inline 模式
            for (int i = 0; i < actualSize; i++) {
                result[i] = get(i);
            }
        } else if (elements != null) {
            System.arraycopy(elements, 0, result, 0, actualSize);
        }
        return result;
    }

    @Override
    public <T> T @NotNull [] toArray(T[] a) {
        int actualSize = size();
        if (a.length < actualSize) {
            a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), actualSize);
        }
        if (size < 0) {
            // Inline 模式
            for (int i = 0; i < actualSize; i++) {
                a[i] = (T) get(i);
            }
        } else if (elements != null) {
            System.arraycopy(elements, 0, a, 0, actualSize);
        }
        if (a.length > actualSize) {
            a[actualSize] = null;
        }
        return a;
    }

    @Override
    public boolean contains(Object o) {
        int actualSize = size();
        for (int i = 0; i < actualSize; i++) {
            if (Objects.equals(o, get(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull List<E> subList(int fromIndex, int toIndex) {
        int actualSize = size();
        if (fromIndex < 0 || toIndex > actualSize || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex + ", toIndex: " + toIndex + ", size: " + actualSize);
        }
        if (fromIndex == 0 && toIndex == actualSize) {
            return this;
        }
        int subSize = toIndex - fromIndex;
        Object[] sub = new Object[subSize];
        for (int i = 0; i < subSize; i++) {
            sub[i] = get(fromIndex + i);
        }
        return ImmutableList.of(sub);
    }
}
