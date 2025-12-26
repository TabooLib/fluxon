package org.tabooproject.fluxon.runtime.function.extension;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.tabooproject.fluxon.FluxonTestUtil.*;

/**
 * ExtensionList tests for List extension functions.
 * Tests read-only operations on immutable lists created with [...] syntax.
 */
class ExtensionListTest {

    @Test
    void testGet() {
        assertBothEqual(20, runSilent("[10, 20, 30]::get(1)"));
    }

    @Test
    void testGetFirstAndLast() {
        assertBothEqual(10, runSilent("[10, 20, 30]::get(0)"));
        assertBothEqual(30, runSilent("[10, 20, 30]::get(2)"));
    }

    @Test
    void testIndexOf() {
        assertBothEqual(1, runSilent("[10, 20, 30]::indexOf(20)"));
        assertBothEqual(-1, runSilent("[10, 20, 30]::indexOf(99)"));
    }

    @Test
    void testIndexOfWithDuplicates() {
        assertBothEqual(0, runSilent("[10, 20, 10, 30]::indexOf(10)"));
    }

    @Test
    void testLastIndexOf() {
        assertBothEqual(3, runSilent("[10, 20, 30, 20]::lastIndexOf(20)"));
    }

    @Test
    void testLastIndexOfNoDuplicate() {
        assertBothEqual(1, runSilent("[10, 20, 30]::lastIndexOf(20)"));
    }

    @Test
    void testSubList() {
        FluxonTestUtil.TestResult result = runSilent("[10, 20, 30, 40]::subList(1, 3)");
        assertMatch(result);
        assertBothToStringEqual("[20, 30]", result);
    }

    @Test
    void testSubListFullRange() {
        FluxonTestUtil.TestResult result = runSilent("[10, 20, 30]::subList(0, 3)");
        assertMatch(result);
        assertBothToStringEqual("[10, 20, 30]", result);
    }

    @Test
    void testSubListSingleElement() {
        FluxonTestUtil.TestResult result = runSilent("[10, 20, 30]::subList(1, 2)");
        assertMatch(result);
        assertBothToStringEqual("[20]", result);
    }

    @Test
    void testIndexOfNotFound() {
        assertBothEqual(-1, runSilent("[1, 2, 3]::indexOf(999)"));
    }

    @Test
    void testLastIndexOfNotFound() {
        assertBothEqual(-1, runSilent("[1, 2, 3]::lastIndexOf(999)"));
    }
}
