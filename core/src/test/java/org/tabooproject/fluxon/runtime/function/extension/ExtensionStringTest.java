package org.tabooproject.fluxon.runtime.function.extension;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.FluxonTestUtil.*;

class ExtensionStringTest {

    @Test
    void testLength() {
        assertBothEqual(5, runSilent("'hello'::length()"));
        assertBothEqual(0, runSilent("''::length()"));
    }

    @Test
    void testTrim() {
        assertBothEqual("hello", runSilent("'  hello  '::trim()"));
        assertBothEqual("hello  ", runSilent("'  hello  '::ltrim()"));
        assertBothEqual("  hello", runSilent("'  hello  '::rtrim()"));
    }

    @Test
    void testSplit() {
        FluxonTestUtil.TestResult result = runSilent("'a,b,c'::split(',')");
        assertMatch(result);
        assertTrue(result.getInterpretResult() instanceof List);
        assertEquals(3, ((List<?>) result.getInterpretResult()).size());
    }

    @Test
    void testReplace() {
        assertBothEqual("hallo", runSilent("'hello'::replace('e', 'a')"));
        assertBothEqual("h-ll-", runSilent("'hello'::replaceAll('[eo]', '-')"));
    }

    @Test
    void testSubstring() {
        assertBothEqual("llo", runSilent("'hello'::substring(2)"));
        assertBothEqual("ll", runSilent("'hello'::substring(2, 4)"));
    }

    @Test
    void testIndexOf() {
        assertBothEqual(2, runSilent("'hello'::indexOf('l')"));
        assertBothEqual(3, runSilent("'hello'::indexOf('l', 3)"));
        assertBothEqual(3, runSilent("'hello'::lastIndexOf('l')"));
        assertBothEqual(2, runSilent("'hello'::lastIndexOf('l', 2)"));
    }

    @Test
    void testCase() {
        assertBothEqual("hello", runSilent("'HELLO'::lowercase()"));
        assertBothEqual("HELLO", runSilent("'hello'::uppercase()"));
    }

    @Test
    void testStartsEndsWith() {
        assertBothEqual(true, runSilent("'hello'::startsWith('he')"));
        assertBothEqual(true, runSilent("'hello'::startsWith('ll', 2)"));
        assertBothEqual(true, runSilent("'hello'::endsWith('lo')"));
    }

    @Test
    void testPadding() {
        assertBothEqual("00123", runSilent("'123'::padLeft(5, '0')"));
        assertBothEqual("123  ", runSilent("'123'::padRight(5)"));
    }

    @Test
    void testMatches() {
        assertBothEqual(true, runSilent("'hello123'::matches('.*\\\\d+')"));
        assertBothEqual(true, runSilent("'hello'::contains('ell')"));
    }

    @Test
    void testRepeat() {
        assertBothEqual("abcabcabc", runSilent("'abc'::repeat(3)"));
        assertBothEqual("", runSilent("'abc'::repeat(0)"));
    }

    @Test
    void testCharAt() {
        assertBothEqual("e", runSilent("'hello'::charAt(1)"));
        assertBothEqual(101, runSilent("'hello'::charCodeAt(1)"));
    }

    @Test
    void testToCharArray() {
        FluxonTestUtil.TestResult result = runSilent("'abc'::toCharArray()");
        assertMatch(result);
        assertTrue(result.getInterpretResult() instanceof List);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
    }

    @Test
    void testIsEmptyBlank() {
        assertBothEqual(true, runSilent("''::isEmpty()"));
        assertBothEqual(false, runSilent("'a'::isEmpty()"));
        assertBothEqual(true, runSilent("'   '::isBlank()"));
        assertBothEqual(false, runSilent("'a'::isBlank()"));
    }

    @Test
    void testReverse() {
        assertBothEqual("olleh", runSilent("'hello'::reverse()"));
    }

    @Test
    void testCapitalize() {
        assertBothEqual("Hello", runSilent("'hELLO'::capitalize()"));
        assertBothEqual("", runSilent("''::capitalize()"));
    }

    @Test
    void testFindAll() {
        FluxonTestUtil.TestResult result = runSilent("'a1b2c3'::findAll('\\\\d')");
        assertMatch(result);
        assertTrue(result.getInterpretResult() instanceof List);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(3, list.size());
    }
}
