package org.tabooproject.fluxon.runtime.function.extension;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import java.util.LinkedHashMap;

import static org.tabooproject.fluxon.FluxonTestUtil.*;

/**
 * ExtensionMap tests for Map extension functions.
 * Tests read-only operations on immutable maps created with [key: value] syntax.
 */
class ExtensionMapTest {

    @Test
    void testGetExisting() {
        assertBothEqual("value", runSilent("[key: 'value']::get('key')"));
    }

    @Test
    void testGetMissing() {
        FluxonTestUtil.TestResult result = runSilent("[key: 'value']::get('other')");
        assertMatch(result);
        // get returns null for missing keys
    }

    @Test
    void testGetOrDefaultMissing() {
        assertBothEqual("default", runSilent("[:]::getOrDefault('missing', 'default')"));
    }

    @Test
    void testGetOrDefaultExisting() {
        assertBothEqual("found", runSilent("[key: 'found']::getOrDefault('key', 'default')"));
    }

    @Test
    void testContainsKeyTrue() {
        assertBothEqual(true, runSilent("[key: 'value']::containsKey('key')"));
    }

    @Test
    void testContainsKeyFalse() {
        assertBothEqual(false, runSilent("[key: 'value']::containsKey('other')"));
    }

    @Test
    void testContainsValueTrue() {
        assertBothEqual(true, runSilent("[key: 'value']::containsValue('value')"));
    }

    @Test
    void testContainsValueFalse() {
        assertBothEqual(false, runSilent("[key: 'value']::containsValue('other')"));
    }

    @Test
    void testSizeEmpty() {
        assertBothEqual(0, runSilent("[:]::size()"));
    }

    @Test
    void testSizeNonEmpty() {
        assertBothEqual(2, runSilent("[a: 1, b: 2]::size()"));
    }

    @Test
    void testIsEmptyTrue() {
        assertBothEqual(true, runSilent("[:]::isEmpty()"));
    }

    @Test
    void testIsEmptyFalse() {
        assertBothEqual(false, runSilent("[a: 1]::isEmpty()"));
    }

    @Test
    void testKeySetSize() {
        FluxonTestUtil.TestResult result = runSilent("[a: 1, b: 2]::keySet()::size()");
        assertMatch(result);
        assertBothEqual(2, result);
    }

    @Test
    void testValuesSize() {
        FluxonTestUtil.TestResult result = runSilent("[a: 1, b: 2]::values()::size()");
        assertMatch(result);
        assertBothEqual(2, result);
    }

    @Test
    void testEntrySetSize() {
        FluxonTestUtil.TestResult result = runSilent("[a: 1, b: 2]::entrySet()::size()");
        assertMatch(result);
        assertBothEqual(2, result);
    }

    @Test
    void testGetWithIntegerKey() {
        assertBothEqual("val", runSilent("[1: 'val']::get(1)"));
    }

    @Test
    void testMultipleKeys() {
        assertBothEqual(3, runSilent("[a: 1, b: 2, c: 3]::get('c')"));
    }

    @Test
    void testLinkedHashMapSizeWithEnv() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&map::size()",
                ctx -> {},
                env -> env.defineRootVariable("map", map)
        );
        assertBothEqual(2, result);
    }

    // Bug 1: 5+ 条目的 Map 字面量创建 LinkedHashMap，size() 在编译模式下应正常工作
    @Test
    void testLinkedHashMapLiteralSize() {
        assertBothEqual(5, runSilent("[a: 1, b: 2, c: 3, d: 4, e: 5]::size()"));
    }

    @Test
    void testLinkedHashMapLiteralSizeSix() {
        assertBothEqual(6, runSilent("[a: 1, b: 2, c: 3, d: 4, e: 5, f: 6]::size()"));
    }

    @Test
    void testLinkedHashMapLiteralGet() {
        assertBothEqual(5, runSilent("[a: 1, b: 2, c: 3, d: 4, e: 5]::get('e')"));
    }

    @Test
    void testLinkedHashMapEnvSize5() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&map::size()",
                ctx -> {},
                env -> env.defineRootVariable("map", map)
        );
        assertBothEqual(5, result);
    }
}
