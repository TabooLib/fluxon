package org.tabooproject.fluxon.runtime.collection;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImmutableListTest {

    @Test
    void emptyListReturnsSingleton() {
        ImmutableList<String> empty1 = ImmutableList.empty();
        ImmutableList<String> empty2 = ImmutableList.of(new Object[0]);
        assertSame(empty1, empty2, "Empty lists should be the same singleton instance");
        assertEquals(0, empty1.size());
    }

    @Test
    void ofCreatesListWithElements() {
        ImmutableList<Integer> list = ImmutableList.of(new Object[]{1, 2, 3});
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    void getReturnsCorrectElement() {
        ImmutableList<String> list = ImmutableList.of(new Object[]{"a", "b", "c"});
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    void getThrowsIndexOutOfBoundsForInvalidIndex() {
        ImmutableList<String> list = ImmutableList.of(new Object[]{"a"});
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> list.get(5));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    void toArrayReturnsNewArray() {
        Object[] original = new Object[]{"x", "y"};
        ImmutableList<String> list = ImmutableList.of(original);
        Object[] result = list.toArray();

        assertArrayEquals(original, result);
        assertNotSame(original, result, "toArray should return a copy, not the original");

        // Modifying result should not affect list
        result[0] = "modified";
        assertEquals("x", list.get(0));
    }

    @Test
    void toArrayWithTypedArraySmallerThanSize() {
        ImmutableList<String> list = ImmutableList.of(new Object[]{"a", "b", "c"});
        String[] result = list.toArray(new String[0]);
        assertArrayEquals(new String[]{"a", "b", "c"}, result);
    }

    @Test
    void toArrayWithTypedArrayLargerThanSize() {
        ImmutableList<String> list = ImmutableList.of(new Object[]{"a", "b"});
        String[] input = new String[5];
        Arrays.fill(input, "placeholder");
        String[] result = list.toArray(input);

        assertSame(input, result, "Should reuse provided array when large enough");
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertNull(result[2], "Element after last should be null");
        assertEquals("placeholder", result[3]);
        assertEquals("placeholder", result[4]);
    }

    @Test
    void toArrayWithTypedArrayExactSize() {
        ImmutableList<String> list = ImmutableList.of(new Object[]{"a", "b"});
        String[] input = new String[2];
        String[] result = list.toArray(input);

        assertSame(input, result);
        assertArrayEquals(new String[]{"a", "b"}, result);
    }

    @Test
    void containsFindsExistingElement() {
        ImmutableList<String> list = ImmutableList.of(new Object[]{"apple", "banana", "cherry"});
        assertTrue(list.contains("banana"));
        assertTrue(list.contains("apple"));
        assertTrue(list.contains("cherry"));
    }

    @Test
    void containsReturnsFalseForMissingElement() {
        ImmutableList<String> list = ImmutableList.of(new Object[]{"apple", "banana"});
        assertFalse(list.contains("orange"));
        assertFalse(list.contains(null));
    }

    @Test
    void containsHandlesNullElements() {
        ImmutableList<String> list = ImmutableList.of(new Object[]{"a", null, "b"});
        assertTrue(list.contains(null));
        assertTrue(list.contains("a"));
    }

    @Test
    void subListReturnsSameInstanceForFullRange() {
        ImmutableList<String> list = ImmutableList.of(new Object[]{"a", "b", "c"});
        List<String> subList = list.subList(0, 3);
        assertSame(list, subList, "Full range subList should return same instance");
    }

    @Test
    void subListReturnsNewImmutableListForPartialRange() {
        ImmutableList<String> list = ImmutableList.of(new Object[]{"a", "b", "c", "d"});
        List<String> subList = list.subList(1, 3);

        assertNotSame(list, subList);
        assertEquals(2, subList.size());
        assertEquals("b", subList.get(0));
        assertEquals("c", subList.get(1));
    }

    @Test
    void subListFromStartToMiddle() {
        ImmutableList<Integer> list = ImmutableList.of(new Object[]{1, 2, 3, 4, 5});
        List<Integer> subList = list.subList(0, 2);

        assertEquals(2, subList.size());
        assertEquals(1, subList.get(0));
        assertEquals(2, subList.get(1));
    }

    @Test
    void subListFromMiddleToEnd() {
        ImmutableList<Integer> list = ImmutableList.of(new Object[]{1, 2, 3, 4, 5});
        List<Integer> subList = list.subList(3, 5);

        assertEquals(2, subList.size());
        assertEquals(4, subList.get(0));
        assertEquals(5, subList.get(1));
    }

    @Test
    void emptySubList() {
        ImmutableList<String> list = ImmutableList.of(new Object[]{"a", "b", "c"});
        List<String> subList = list.subList(1, 1);
        assertEquals(0, subList.size());
    }

    @Test
    void iteratorWorks() {
        ImmutableList<String> list = ImmutableList.of(new Object[]{"a", "b", "c"});
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s);
        }
        assertEquals("abc", sb.toString());
    }
}
