package org.tabooproject.fluxon.runtime.function.extension;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.FluxonTestUtil.TestResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.FluxonTestUtil.*;

/**
 * ExtensionString 边界测试
 * 测试空字符串、边界索引、特殊字符等情况
 *
 * @author sky
 */
public class ExtensionStringBoundaryTest {

    @Test
    void testLengthEmpty() {
        assertBothEqual(0, runSilent("''::length()"));
    }

    @Test
    void testLengthSingleChar() {
        assertBothEqual(1, runSilent("'a'::length()"));
    }

    @Test
    void testLengthWhitespace() {
        assertBothEqual(3, runSilent("'   '::length()"));
        assertBothEqual(1, runSilent("' '::length()"));
    }

    @Test
    void testLengthUnicode() {
        // Unicode 字符 (中文)
        assertBothEqual(3, runSilent("'你好啊'::length()"));
        // Emoji (占用两个 char)
        TestResult result = runSilent("'😀'::length()");
        assertEquals(2, result.getInterpretResult()); // Java String 中 emoji 占 2 个 char
    }

    @Test
    void testTrimEmpty() {
        assertBothEqual("", runSilent("''::trim()"));
        assertBothEqual("", runSilent("''::ltrim()"));
        assertBothEqual("", runSilent("''::rtrim()"));
    }

    @Test
    void testTrimOnlyWhitespace() {
        assertBothEqual("", runSilent("'   '::trim()"));
        assertBothEqual("", runSilent("'   '::ltrim()"));
        assertBothEqual("", runSilent("'   '::rtrim()"));
    }

    @Test
    void testTrimNoWhitespace() {
        assertBothEqual("hello", runSilent("'hello'::trim()"));
        assertBothEqual("hello", runSilent("'hello'::ltrim()"));
        assertBothEqual("hello", runSilent("'hello'::rtrim()"));
    }

    @Test
    void testTrimMixed() {
        assertBothEqual("hello", runSilent("'  hello  '::trim()"));
        assertBothEqual("hello  ", runSilent("'  hello  '::ltrim()"));
        assertBothEqual("  hello", runSilent("'  hello  '::rtrim()"));
    }

    @Test
    void testTrimTabs() {
        assertBothEqual("hello", runSilent("'\t\thello\t\t'::trim()"));
    }

    @Test
    void testTrimNewlines() {
        assertBothEqual("hello", runSilent("'\n\nhello\n\n'::trim()"));
    }

    @Test
    void testSplitEmpty() {
        TestResult result = runSilent("''::split(',')");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(1, list.size());
        assertEquals("", list.get(0));
    }

    @Test
    void testSplitNoDelimiter() {
        TestResult result = runSilent("'hello'::split(',')");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(1, list.size());
        assertEquals("hello", list.get(0));
    }

    @Test
    void testSplitConsecutiveDelimiters() {
        TestResult result = runSilent("'a,,b'::split(',')");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("", list.get(1));
        assertEquals("b", list.get(2));
    }

    @Test
    void testSplitTrailingDelimiter() {
        TestResult result = runSilent("'a,b,'::split(',')");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        // Java split 默认移除尾部空串
        assertEquals(2, list.size());
    }

    @Test
    void testSplitRegexDelimiter() {
        TestResult result = runSilent("'a1b2c'::split('\\\\d')");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(3, list.size());
    }

    @Test
    void testReplaceEmpty() {
        // Java replace("", "x") 会在每个字符间插入 x
        assertBothEqual("xhxexlxlxox", runSilent("'hello'::replace('', 'x')"));
    }

    @Test
    void testReplaceNotFound() {
        assertBothEqual("hello", runSilent("'hello'::replace('x', 'y')"));
    }

    @Test
    void testReplaceMultipleOccurrences() {
        // Java replace 替换所有出现
        assertBothEqual("hella warld", runSilent("'hello world'::replace('o', 'a')"));
    }

    @Test
    void testReplaceWithEmpty() {
        assertBothEqual("hell wrld", runSilent("'hello world'::replace('o', '')"));
    }

    @Test
    void testReplaceAllEmpty() {
        // Java replaceAll("", "x") 同样会在每个位置插入 x
        assertBothEqual("xhxexlxlxox", runSilent("'hello'::replaceAll('', 'x')"));
    }

    @Test
    void testReplaceAllRegex() {
        assertBothEqual("h-ll- w-rld", runSilent("'hello world'::replaceAll('[eo]', '-')"));
    }

    @Test
    void testReplaceAllDigits() {
        assertBothEqual("a_b_c_", runSilent("'a1b2c3'::replaceAll('\\\\d', '_')"));
    }

    @Test
    void testSubstringFromZero() {
        assertBothEqual("hello", runSilent("'hello'::substring(0)"));
    }

    @Test
    void testSubstringFromEnd() {
        assertBothEqual("", runSilent("'hello'::substring(5)"));
    }

    @Test
    void testSubstringFullRange() {
        assertBothEqual("hello", runSilent("'hello'::substring(0, 5)"));
    }

    @Test
    void testSubstringEmptyRange() {
        assertBothEqual("", runSilent("'hello'::substring(2, 2)"));
    }

    @Test
    void testSubstringOverflow() {
        // 实现中有 Math.min(end, str.length())
        assertBothEqual("llo", runSilent("'hello'::substring(2, 100)"));
    }

    @Test
    void testSubstringEmpty() {
        assertBothEqual("", runSilent("''::substring(0)"));
        assertBothEqual("", runSilent("''::substring(0, 0)"));
    }

    @Test
    void testSubstringSingleChar() {
        assertBothEqual("e", runSilent("'hello'::substring(1, 2)"));
    }

    @Test
    void testIndexOfNotFound() {
        assertBothEqual(-1, runSilent("'hello'::indexOf('x')"));
    }

    @Test
    void testIndexOfEmpty() {
        assertBothEqual(0, runSilent("'hello'::indexOf('')"));
    }

    @Test
    void testIndexOfAtStart() {
        assertBothEqual(0, runSilent("'hello'::indexOf('h')"));
    }

    @Test
    void testIndexOfAtEnd() {
        assertBothEqual(4, runSilent("'hello'::indexOf('o')"));
    }

    @Test
    void testIndexOfWithOffset() {
        assertBothEqual(3, runSilent("'hello'::indexOf('l', 3)"));
        assertBothEqual(-1, runSilent("'hello'::indexOf('l', 4)"));
    }

    @Test
    void testIndexOfWithOffsetPastEnd() {
        assertBothEqual(-1, runSilent("'hello'::indexOf('o', 10)"));
    }

    @Test
    void testLastIndexOfNotFound() {
        assertBothEqual(-1, runSilent("'hello'::lastIndexOf('x')"));
    }

    @Test
    void testLastIndexOfEmpty() {
        assertBothEqual(5, runSilent("'hello'::lastIndexOf('')"));
    }

    @Test
    void testLastIndexOfMultiple() {
        assertBothEqual(3, runSilent("'hello'::lastIndexOf('l')"));
    }

    @Test
    void testLastIndexOfWithOffset() {
        assertBothEqual(2, runSilent("'hello'::lastIndexOf('l', 2)"));
    }

    @Test
    void testIndexOfEmptyString() {
        assertBothEqual(-1, runSilent("''::indexOf('x')"));
        assertBothEqual(0, runSilent("''::indexOf('')"));
    }

    @Test
    void testLowercaseEmpty() {
        assertBothEqual("", runSilent("''::lowercase()"));
    }

    @Test
    void testUppercaseEmpty() {
        assertBothEqual("", runSilent("''::uppercase()"));
    }

    @Test
    void testLowercaseAlreadyLower() {
        assertBothEqual("hello", runSilent("'hello'::lowercase()"));
    }

    @Test
    void testUppercaseAlreadyUpper() {
        assertBothEqual("HELLO", runSilent("'HELLO'::uppercase()"));
    }

    @Test
    void testCaseMixed() {
        assertBothEqual("hello world", runSilent("'HeLLo WoRLd'::lowercase()"));
        assertBothEqual("HELLO WORLD", runSilent("'HeLLo WoRLd'::uppercase()"));
    }

    @Test
    void testCaseNumbers() {
        assertBothEqual("abc123", runSilent("'ABC123'::lowercase()"));
        assertBothEqual("ABC123", runSilent("'abc123'::uppercase()"));
    }

    @Test
    void testStartsWithEmpty() {
        assertBothEqual(true, runSilent("'hello'::startsWith('')"));
        assertBothEqual(true, runSilent("''::startsWith('')"));
    }

    @Test
    void testEndsWithEmpty() {
        assertBothEqual(true, runSilent("'hello'::endsWith('')"));
        assertBothEqual(true, runSilent("''::endsWith('')"));
    }

    @Test
    void testStartsWithFull() {
        assertBothEqual(true, runSilent("'hello'::startsWith('hello')"));
    }

    @Test
    void testEndsWithFull() {
        assertBothEqual(true, runSilent("'hello'::endsWith('hello')"));
    }

    @Test
    void testStartsWithLonger() {
        assertBothEqual(false, runSilent("'hi'::startsWith('hello')"));
    }

    @Test
    void testEndsWithLonger() {
        assertBothEqual(false, runSilent("'hi'::endsWith('hello')"));
    }

    @Test
    void testStartsWithOffset() {
        assertBothEqual(true, runSilent("'hello'::startsWith('llo', 2)"));
        assertBothEqual(false, runSilent("'hello'::startsWith('llo', 1)"));
    }

    @Test
    void testStartsWithOffsetPastEnd() {
        assertBothEqual(false, runSilent("'hello'::startsWith('o', 10)"));
    }

    @Test
    void testPadLeftNoChange() {
        assertBothEqual("hello", runSilent("'hello'::padLeft(3)"));
        assertBothEqual("hello", runSilent("'hello'::padLeft(5)"));
    }

    @Test
    void testPadRightNoChange() {
        assertBothEqual("hello", runSilent("'hello'::padRight(3)"));
        assertBothEqual("hello", runSilent("'hello'::padRight(5)"));
    }

    @Test
    void testPadLeftEmpty() {
        assertBothEqual("     ", runSilent("''::padLeft(5)"));
    }

    @Test
    void testPadRightEmpty() {
        assertBothEqual("     ", runSilent("''::padRight(5)"));
    }

    @Test
    void testPadLeftWithChar() {
        assertBothEqual("00123", runSilent("'123'::padLeft(5, '0')"));
        assertBothEqual("--abc", runSilent("'abc'::padLeft(5, '-')"));
    }

    @Test
    void testPadRightWithChar() {
        assertBothEqual("123**", runSilent("'123'::padRight(5, '*')"));
    }

    @Test
    void testPadWithEmptyChar() {
        // 空字符串默认用空格
        assertBothEqual("  123", runSilent("'123'::padLeft(5, '')"));
    }

    @Test
    void testPadZeroLength() {
        assertBothEqual("hello", runSilent("'hello'::padLeft(0)"));
        assertBothEqual("hello", runSilent("'hello'::padRight(0)"));
    }

    @Test
    void testMatchesEmpty() {
        assertBothEqual(true, runSilent("''::matches('')"));
        assertBothEqual(false, runSilent("'hello'::matches('')"));
    }

    @Test
    void testMatchesFull() {
        assertBothEqual(true, runSilent("'hello'::matches('hello')"));
        assertBothEqual(true, runSilent("'hello'::matches('.*')"));
    }

    @Test
    void testMatchesPartial() {
        // matches 必须匹配整个字符串
        assertBothEqual(false, runSilent("'hello'::matches('ell')"));
        assertBothEqual(true, runSilent("'hello'::matches('.*ell.*')"));
    }

    @Test
    void testContainsEmpty() {
        assertBothEqual(true, runSilent("'hello'::contains('')"));
        assertBothEqual(true, runSilent("''::contains('')"));
    }

    @Test
    void testContainsNotFound() {
        assertBothEqual(false, runSilent("'hello'::contains('xyz')"));
    }

    @Test
    void testContainsPartial() {
        assertBothEqual(true, runSilent("'hello'::contains('ell')"));
    }

    @Test
    void testContainsFull() {
        assertBothEqual(true, runSilent("'hello'::contains('hello')"));
    }

    @Test
    void testContainsLonger() {
        assertBothEqual(false, runSilent("'hi'::contains('hello')"));
    }

    @Test
    void testRepeatZero() {
        assertBothEqual("", runSilent("'hello'::repeat(0)"));
    }

    @Test
    void testRepeatNegative() {
        assertBothEqual("", runSilent("'hello'::repeat(-1)"));
    }

    @Test
    void testRepeatOne() {
        assertBothEqual("hello", runSilent("'hello'::repeat(1)"));
    }

    @Test
    void testRepeatEmpty() {
        assertBothEqual("", runSilent("''::repeat(5)"));
    }

    @Test
    void testRepeatSingleChar() {
        assertBothEqual("aaaaa", runSilent("'a'::repeat(5)"));
    }

    @Test
    void testCharAtFirst() {
        assertBothEqual("h", runSilent("'hello'::charAt(0)"));
    }

    @Test
    void testCharAtLast() {
        assertBothEqual("o", runSilent("'hello'::charAt(4)"));
    }

    @Test
    void testCharAtNegativeThrows() {
        runExpectingError("'hello'::charAt(-1)", "String index out of range");
    }

    @Test
    void testCharAtOverflowThrows() {
        runExpectingError("'hello'::charAt(5)", "String index out of range");
        runExpectingError("'hello'::charAt(100)", "String index out of range");
    }

    @Test
    void testCharCodeAtFirst() {
        assertBothEqual(104, runSilent("'hello'::charCodeAt(0)")); // 'h' = 104
    }

    @Test
    void testCharCodeAtNegativeThrows() {
        runExpectingError("'hello'::charCodeAt(-1)", "String index out of range");
    }

    @Test
    void testCharCodeAtOverflowThrows() {
        runExpectingError("'hello'::charCodeAt(5)", "String index out of range");
    }

    @Test
    void testToCharArrayEmpty() {
        TestResult result = runSilent("''::toCharArray()");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(0, list.size());
    }

    @Test
    void testToCharArraySingleChar() {
        TestResult result = runSilent("'a'::toCharArray()");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(1, list.size());
        assertEquals("a", list.get(0));
    }

    @Test
    void testToCharArrayMultiple() {
        TestResult result = runSilent("'abc'::toCharArray()");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    void testIsEmptyEmpty() {
        assertBothEqual(true, runSilent("''::isEmpty()"));
    }

    @Test
    void testIsEmptyWhitespace() {
        assertBothEqual(false, runSilent("' '::isEmpty()"));
        assertBothEqual(false, runSilent("'   '::isEmpty()"));
    }

    @Test
    void testIsEmptyNonEmpty() {
        assertBothEqual(false, runSilent("'a'::isEmpty()"));
    }

    @Test
    void testIsBlankEmpty() {
        assertBothEqual(true, runSilent("''::isBlank()"));
    }

    @Test
    void testIsBlankWhitespace() {
        assertBothEqual(true, runSilent("' '::isBlank()"));
        assertBothEqual(true, runSilent("'   '::isBlank()"));
        assertBothEqual(true, runSilent("'\t\n'::isBlank()"));
    }

    @Test
    void testIsBlankNonBlank() {
        assertBothEqual(false, runSilent("'a'::isBlank()"));
        assertBothEqual(false, runSilent("' a '::isBlank()"));
    }

    @Test
    void testReverseEmpty() {
        assertBothEqual("", runSilent("''::reverse()"));
    }

    @Test
    void testReverseSingleChar() {
        assertBothEqual("a", runSilent("'a'::reverse()"));
    }

    @Test
    void testReversePalindrome() {
        assertBothEqual("aba", runSilent("'aba'::reverse()"));
    }

    @Test
    void testReverseNormal() {
        assertBothEqual("olleh", runSilent("'hello'::reverse()"));
        assertBothEqual("cba", runSilent("'abc'::reverse()"));
    }

    @Test
    void testReverseWithSpaces() {
        assertBothEqual("dlrow olleh", runSilent("'hello world'::reverse()"));
    }

    @Test
    void testCapitalizeEmpty() {
        assertBothEqual("", runSilent("''::capitalize()"));
    }

    @Test
    void testCapitalizeSingleChar() {
        assertBothEqual("A", runSilent("'a'::capitalize()"));
        assertBothEqual("A", runSilent("'A'::capitalize()"));
    }

    @Test
    void testCapitalizeAlreadyCapitalized() {
        assertBothEqual("Hello", runSilent("'Hello'::capitalize()"));
    }

    @Test
    void testCapitalizeLowercase() {
        assertBothEqual("Hello", runSilent("'hello'::capitalize()"));
    }

    @Test
    void testCapitalizeMixed() {
        assertBothEqual("Hello", runSilent("'hELLO'::capitalize()"));
        assertBothEqual("Hello world", runSilent("'HELLO WORLD'::capitalize()"));
    }

    @Test
    void testCapitalizeNumber() {
        assertBothEqual("123abc", runSilent("'123ABC'::capitalize()"));
    }

    @Test
    void testFindAllEmpty() {
        TestResult result = runSilent("'hello'::findAll('')");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(0, list.size());
    }

    @Test
    void testFindAllNotFound() {
        TestResult result = runSilent("'hello'::findAll('x')");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(0, list.size());
    }

    @Test
    void testFindAllSingleMatch() {
        TestResult result = runSilent("'hello'::findAll('ell')");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(1, list.size());
        assertEquals("ell", list.get(0));
    }

    @Test
    void testFindAllMultipleMatches() {
        TestResult result = runSilent("'abcabc'::findAll('abc')");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(2, list.size());
    }

    @Test
    void testFindAllRegex() {
        TestResult result = runSilent("'a1b2c3d4'::findAll('\\\\d')");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(4, list.size());
    }

    @Test
    void testFindAllOverlapping() {
        // 正则不会匹配重叠
        TestResult result = runSilent("'aaaa'::findAll('aa')");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(2, list.size()); // aa|aa, 不是 aa|aa|aa
    }

    @Test
    void testFindAllEmptyString() {
        TestResult result = runSilent("''::findAll('a')");
        assertMatch(result);
        List<?> list = (List<?>) result.getInterpretResult();
        assertEquals(0, list.size());
    }

    @Test
    void testChainedCalls() {
        assertBothEqual("HELLO", runSilent("'  hello  '::trim()::uppercase()"));
        assertBothEqual(5, runSilent("'  hello  '::trim()::length()"));
        assertBothEqual("olleh", runSilent("'hello'::uppercase()::lowercase()::reverse()"));
    }

    @Test
    void testChainedWithSubstring() {
        assertBothEqual("ELL", runSilent("'hello'::substring(1, 4)::uppercase()"));
    }

    @Test
    void testUnicodeBasic() {
        assertBothEqual("你好", runSilent("'你好世界'::substring(0, 2)"));
        assertBothEqual("世界", runSilent("'你好世界'::substring(2)"));
    }

    @Test
    void testUnicodeIndexOf() {
        assertBothEqual(2, runSilent("'你好世界'::indexOf('世')"));
    }

    @Test
    void testUnicodeReverse() {
        assertBothEqual("界世好你", runSilent("'你好世界'::reverse()"));
    }

    @Test
    void testUnicodeContains() {
        assertBothEqual(true, runSilent("'你好世界'::contains('世界')"));
        assertBothEqual(false, runSilent("'你好世界'::contains('地球')"));
    }
}
