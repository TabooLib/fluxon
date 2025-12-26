package org.tabooproject.fluxon.runtime.function.extension;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

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
}
