package org.tabooproject.fluxon.runtime.collection;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CopyOnWriteMapTest {

    @Test
    void wrapDoesNotCopyImmediately() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);

        assertFalse(cow.isDetached(), "Should not be detached after wrap");
        assertEquals(2, cow.size());
        assertEquals(1, cow.get("a"));
        assertEquals(2, cow.get("b"));
    }

    @Test
    void readOperationsDoNotTriggerCopy() {
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 10);

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);

        // 执行各种读操作
        cow.size();
        cow.isEmpty();
        cow.containsKey("x");
        cow.containsValue(10);
        cow.get("x");
        cow.getOrDefault("y", 99);
        cow.keySet();
        cow.values();
        cow.entrySet();

        assertFalse(cow.isDetached(), "Read operations should not trigger copy");
    }

    @Test
    void putTriggersCopy() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);
        cow.put("b", 2);

        assertTrue(cow.isDetached(), "Put should trigger copy");
        assertEquals(2, cow.size());
        assertEquals(1, cow.get("a"));
        assertEquals(2, cow.get("b"));

        // 原始 Map 不应被修改
        assertEquals(1, source.size());
        assertFalse(source.containsKey("b"));
    }

    @Test
    void removeTriggersCopy() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);
        cow.remove("a");

        assertTrue(cow.isDetached(), "Remove should trigger copy");
        assertEquals(1, cow.size());
        assertNull(cow.get("a"));
        assertEquals(2, cow.get("b"));

        // 原始 Map 不应被修改
        assertEquals(2, source.size());
        assertTrue(source.containsKey("a"));
    }

    @Test
    void clearTriggersCopy() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);
        cow.clear();

        assertTrue(cow.isDetached(), "Clear should trigger copy");
        assertTrue(cow.isEmpty());

        // 原始 Map 不应被修改
        assertEquals(1, source.size());
    }

    @Test
    void clearOnEmptyMapDoesNotTriggerCopy() {
        Map<String, Integer> source = new HashMap<>();

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);
        cow.clear();

        assertFalse(cow.isDetached(), "Clear on empty map should not trigger copy");
    }

    @Test
    void putAllWithEmptyMapDoesNotTriggerCopy() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);
        cow.putAll(new HashMap<>());

        assertFalse(cow.isDetached(), "PutAll with empty map should not trigger copy");
    }

    @Test
    void putIfAbsentOnlyTriggersWhenNeeded() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);

        // 已存在的 key 不触发复制
        Integer result1 = cow.putIfAbsent("a", 100);
        assertEquals(1, result1);
        assertFalse(cow.isDetached(), "putIfAbsent on existing key should not trigger copy");

        // 不存在的 key 触发复制
        Integer result2 = cow.putIfAbsent("b", 2);
        assertNull(result2);
        assertTrue(cow.isDetached(), "putIfAbsent on new key should trigger copy");
        assertEquals(2, cow.get("b"));
    }

    @Test
    void replaceOnlyTriggersWhenNeeded() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);

        // 不存在的 key 不触发复制
        Integer result1 = cow.replace("nonexistent", 100);
        assertNull(result1);
        assertFalse(cow.isDetached(), "replace on non-existing key should not trigger copy");

        // 存在的 key 触发复制
        Integer result2 = cow.replace("a", 10);
        assertEquals(1, result2);
        assertTrue(cow.isDetached(), "replace on existing key should trigger copy");
        assertEquals(10, cow.get("a"));
    }

    @Test
    void removeWithValueOnlyTriggersWhenMatched() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);

        // 值不匹配不触发复制
        boolean result1 = cow.remove("a", 999);
        assertFalse(result1);
        assertFalse(cow.isDetached(), "remove with non-matching value should not trigger copy");

        // 值匹配触发复制
        boolean result2 = cow.remove("a", 1);
        assertTrue(result2);
        assertTrue(cow.isDetached(), "remove with matching value should trigger copy");
        assertNull(cow.get("a"));
    }

    @Test
    void detachMethodForcesImmediateCopy() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);
        assertFalse(cow.isDetached());

        cow.detach();
        assertTrue(cow.isDetached());

        // 后续修改原始 Map 不影响 cow
        source.put("b", 2);
        assertFalse(cow.containsKey("b"));
    }

    @Test
    void emptyConstructorCreatesDetachedMap() {
        CopyOnWriteMap<String, Integer> cow = new CopyOnWriteMap<>();

        assertTrue(cow.isDetached(), "Empty constructor should create detached map");
        assertTrue(cow.isEmpty());
    }

    @Test
    void capacityConstructorCreatesDetachedMap() {
        CopyOnWriteMap<String, Integer> cow = new CopyOnWriteMap<>(100);

        assertTrue(cow.isDetached(), "Capacity constructor should create detached map");
        assertTrue(cow.isEmpty());
    }

    @Test
    void staticEmptyFactoryCreatesEmptyMap() {
        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.empty();

        assertTrue(cow.isDetached());
        assertTrue(cow.isEmpty());
    }

    @Test
    void keySetIsUnmodifiableBeforeDetach() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);

        assertThrows(UnsupportedOperationException.class, () -> cow.keySet().remove("a"));
        assertFalse(cow.isDetached(), "Failed modification should not trigger detach");
    }

    @Test
    void keySetIsModifiableAfterDetach() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);
        cow.detach();

        cow.keySet().remove("a");
        assertEquals(1, cow.size());
        assertFalse(cow.containsKey("a"));
    }

    @Test
    void multipleWritesOnlyDetachOnce() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);

        cow.put("b", 2);
        assertTrue(cow.isDetached());

        // 后续写入不会再次复制（已经 detached）
        cow.put("c", 3);
        cow.put("d", 4);
        cow.remove("a");

        assertEquals(3, cow.size());
        assertFalse(cow.containsKey("a"));
        assertTrue(cow.containsKey("b"));
        assertTrue(cow.containsKey("c"));
        assertTrue(cow.containsKey("d"));
    }

    @Test
    void iterationWorksBeforeAndAfterDetach() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        CopyOnWriteMap<String, Integer> cow = CopyOnWriteMap.wrap(source);

        // 迭代前 detach
        int countBefore = 0;
        for (Map.Entry<String, Integer> entry : cow.entrySet()) {
            countBefore++;
        }
        assertEquals(2, countBefore);

        cow.put("c", 3);

        // 迭代后 detach
        int countAfter = 0;
        for (Map.Entry<String, Integer> entry : cow.entrySet()) {
            countAfter++;
        }
        assertEquals(3, countAfter);
    }
}
