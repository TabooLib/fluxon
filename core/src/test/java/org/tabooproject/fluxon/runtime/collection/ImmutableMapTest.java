package org.tabooproject.fluxon.runtime.collection;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ImmutableMapTest {

    @Test
    void emptyMapReturnsSingleton() {
        ImmutableMap empty1 = ImmutableMap.empty();
        ImmutableMap empty2 = ImmutableMap.of(new Object[0], new Object[0]);
        assertSame(empty1, empty2, "Empty maps should be the same singleton instance");
        assertEquals(0, empty1.size());
    }

    @Test
    void ofCreatesMapWithEntries() {
        ImmutableMap map = ImmutableMap.of(
                new Object[]{"a", "b", "c"},
                new Object[]{1, 2, 3}
        );
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
    }

    @Test
    void getReturnsNullForMissingKey() {
        ImmutableMap map = ImmutableMap.of(
                new Object[]{"key1"},
                new Object[]{"value1"}
        );
        assertNull(map.get("nonexistent"));
        assertNull(map.get(null));
    }

    @Test
    void getHandlesNullKey() {
        ImmutableMap map = ImmutableMap.of(
                new Object[]{null, "key"},
                new Object[]{"nullValue", "keyValue"}
        );
        assertEquals("nullValue", map.get(null));
        assertEquals("keyValue", map.get("key"));
    }

    @Test
    void containsKeyFindsExistingKeys() {
        ImmutableMap map = ImmutableMap.of(
                new Object[]{"x", "y", "z"},
                new Object[]{10, 20, 30}
        );
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey("y"));
        assertTrue(map.containsKey("z"));
        assertFalse(map.containsKey("w"));
    }

    @Test
    void containsKeyHandlesNullKey() {
        ImmutableMap map = ImmutableMap.of(
                new Object[]{null, "a"},
                new Object[]{1, 2}
        );
        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("a"));
        assertFalse(map.containsKey("b"));
    }

    @Test
    void containsValueFindsExistingValues() {
        ImmutableMap map = ImmutableMap.of(
                new Object[]{"a", "b"},
                new Object[]{"apple", "banana"}
        );
        assertTrue(map.containsValue("apple"));
        assertTrue(map.containsValue("banana"));
        assertFalse(map.containsValue("cherry"));
    }

    @Test
    void containsValueHandlesNullValue() {
        ImmutableMap map = ImmutableMap.of(
                new Object[]{"a", "b"},
                new Object[]{null, "value"}
        );
        assertTrue(map.containsValue(null));
        assertTrue(map.containsValue("value"));
        assertFalse(map.containsValue("other"));
    }

    @Test
    void entrySetReturnsAllEntries() {
        ImmutableMap map = ImmutableMap.of(
                new Object[]{"k1", "k2"},
                new Object[]{"v1", "v2"}
        );
        Set<Map.Entry<Object, Object>> entrySet = map.entrySet();
        assertEquals(2, entrySet.size());

        boolean foundK1 = false;
        boolean foundK2 = false;
        for (Map.Entry<Object, Object> entry : entrySet) {
            if ("k1".equals(entry.getKey()) && "v1".equals(entry.getValue())) {
                foundK1 = true;
            }
            if ("k2".equals(entry.getKey()) && "v2".equals(entry.getValue())) {
                foundK2 = true;
            }
        }
        assertTrue(foundK1, "Should find k1->v1 entry");
        assertTrue(foundK2, "Should find k2->v2 entry");
    }

    @Test
    void entryIteratorHasNextReturnsFalseWhenEmpty() {
        ImmutableMap map = ImmutableMap.empty();
        Iterator<Map.Entry<Object, Object>> iterator = map.entrySet().iterator();
        assertFalse(iterator.hasNext());
    }

    @Test
    void entryIteratorThrowsNoSuchElementWhenExhausted() {
        ImmutableMap map = ImmutableMap.of(
                new Object[]{"a"},
                new Object[]{1}
        );
        Iterator<Map.Entry<Object, Object>> iterator = map.entrySet().iterator();
        assertTrue(iterator.hasNext());
        iterator.next();
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void iteratorTraversesAllEntries() {
        ImmutableMap map = ImmutableMap.of(
                new Object[]{1, 2, 3},
                new Object[]{"one", "two", "three"}
        );
        int count = 0;
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            count++;
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());
        }
        assertEquals(3, count);
    }

    @Test
    void sizeReturnsCorrectCount() {
        assertEquals(0, ImmutableMap.empty().size());
        assertEquals(1, ImmutableMap.of(new Object[]{"a"}, new Object[]{1}).size());
        assertEquals(5, ImmutableMap.of(
                new Object[]{"a", "b", "c", "d", "e"},
                new Object[]{1, 2, 3, 4, 5}
        ).size());
    }

    @Test
    void mapCanBeIteratedMultipleTimes() {
        ImmutableMap map = ImmutableMap.of(
                new Object[]{"a", "b"},
                new Object[]{1, 2}
        );

        // First iteration
        int count1 = 0;
        for (Map.Entry<Object, Object> ignored : map.entrySet()) {
            count1++;
        }

        // Second iteration
        int count2 = 0;
        for (Map.Entry<Object, Object> ignored : map.entrySet()) {
            count2++;
        }

        assertEquals(2, count1);
        assertEquals(2, count2);
    }
}
