package org.tabooproject.fluxon.runtime.collection;

import org.tabooproject.fluxon.runtime.Type;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * 惰性整数范围，不分配元素数组。
 * 支持正向和反向范围。
 *
 * @author sky
 */
public class IntRange extends AbstractList<Integer> implements RandomAccess {

    public static final Type TYPE = new Type(IntRange.class);

    private final int start;
    private final int end; // inclusive
    private final int step; // +1 or -1
    private final int size;

    public IntRange(int start, int end) {
        this.start = start;
        this.end = end;
        this.step = start <= end ? 1 : -1;
        this.size = Math.abs(end - start) + 1;
    }

    @Override
    public Integer get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return start + index * step;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Number)) return false;
        int value = ((Number) o).intValue();
        if (step > 0) {
            return value >= start && value <= end;
        } else {
            return value <= start && value >= end;
        }
    }

    @Override
    public int indexOf(Object o) {
        if (!(o instanceof Number)) return -1;
        int value = ((Number) o).intValue();
        int index = (value - start) * step; // normalize to positive index
        if (index < 0 || index >= size) return -1;
        return index;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private int current = start;
            private int remaining = size;

            @Override
            public boolean hasNext() {
                return remaining > 0;
            }

            @Override
            public Integer next() {
                if (remaining <= 0) throw new NoSuchElementException();
                int value = current;
                current += step;
                remaining--;
                return value;
            }
        };
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getStep() {
        return step;
    }

    @Override
    public String toString() {
        return start + ".." + end;
    }
}
