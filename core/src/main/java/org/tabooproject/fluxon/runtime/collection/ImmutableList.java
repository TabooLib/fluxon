package org.tabooproject.fluxon.runtime.collection;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

/**
 * 简单的不可变列表实现，基于数组存储，兼容 Java 8。
 */
@SuppressWarnings("unchecked")
public final class ImmutableList<E> extends AbstractList<E> implements RandomAccess, Serializable {

    private static final long serialVersionUID = 1L;

    private static final ImmutableList<?> EMPTY = new ImmutableList<>(new Object[0]);

    private final Object[] elements;

    private ImmutableList(Object[] elements) {
        this.elements = elements;
    }

    @SuppressWarnings("unchecked")
    public static <E> ImmutableList<E> of(Object[] elements) {
        if (elements.length == 0) {
            return (ImmutableList<E>) EMPTY;
        }
        return new ImmutableList<>(elements);
    }

    @SuppressWarnings("unchecked")
    public static <E> ImmutableList<E> empty() {
        return (ImmutableList<E>) EMPTY;
    }

    @Override
    public E get(int index) {
        return (E) elements[index];
    }

    @Override
    public int size() {
        return elements.length;
    }

    @Override
    public Object @NotNull [] toArray() {
        return Arrays.copyOf(elements, elements.length);
    }

    @Override
    public <T> T @NotNull [] toArray(T[] a) {
        int size = elements.length;
        if (a.length < size) {
            // noinspection unchecked
            return (T[]) Arrays.copyOf(elements, size, a.getClass());
        }
        System.arraycopy(elements, 0, a, 0, size);
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    @Override
    public boolean contains(Object o) {
        for (Object element : elements) {
            if (Objects.equals(o, element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull List<E> subList(int fromIndex, int toIndex) {
        if (fromIndex == 0 && toIndex == elements.length) {
            return this;
        }
        return ImmutableList.of(Arrays.copyOfRange(elements, fromIndex, toIndex));
    }
}
