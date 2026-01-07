package org.tabooproject.fluxon.runtime.function.extension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExtensionIterable 测试
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ExtensionIterableTest {

    // ========== Transformation ==========

    @Test
    public void testMap() {
        assertBoth("[2, 4, 6]", "[1, 2, 3]::map(|| &it * 2)");
    }

    @Test
    public void testFlatMap() {
        assertBoth("[1, 1, 2, 2, 3, 3]", "[1, 2, 3]::flatMap(|| [&it, &it])");
    }

    @Test
    public void testAssociateBy() {
        assertBoth("{a=1, b=2}", "[1, 2]::associateBy(|| if &it == 1 then 'a' else 'b')");
    }

    @Test
    public void testAssociateWith() {
        assertBoth("{1=2, 2=4}", "[1, 2]::associateWith(|| &it * 2)");
    }

    // ========== Filtering ==========

    @Test
    public void testFilter() {
        assertBoth("[2, 4]", "[1, 2, 3, 4]::filter(|| &it % 2 == 0)");
    }

    // ========== Checking ==========

    @Test
    public void testAny() {
        assertBoth("true", "[1, 2, 3]::any(|| &it > 2)");
        assertBoth("false", "[1, 2, 3]::any(|| &it > 5)");
    }

    @Test
    public void testAll() {
        assertBoth("true", "[1, 2, 3]::all(|| &it > 0)");
        assertBoth("false", "[1, 2, 3]::all(|| &it > 2)");
    }

    @Test
    public void testNone() {
        assertBoth("true", "[1, 2, 3]::none(|| &it > 5)");
        assertBoth("false", "[1, 2, 3]::none(|| &it > 2)");
    }

    // ========== Retrieving ==========

    @Test
    public void testFind() {
        assertBoth("3", "[1, 2, 3, 4]::find(|| &it > 2)");
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("[1, 2, 3]::find(|| &it > 5)");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());
    }

    @Test
    public void testFirst() {
        assertBoth("1", "[1, 2, 3]::first()");
    }

    @Test
    public void testLast() {
        assertBoth("3", "[1, 2, 3]::last()");
    }

    // ========== Aggregation ==========

    @Test
    public void testCountOf() {
        assertBoth("2", "[1, 2, 3, 4]::countOf(|| &it > 2)");
    }

    @Test
    public void testSumOf() {
        assertBoth("20.0", "[1, 2, 3, 4]::sumOf(|| &it * 2)");
    }

    @Test
    public void testMinOf() {
        assertBoth("1", "[3, 1, 2]::minOf(|| &it)");
    }

    @Test
    public void testMaxOf() {
        assertBoth("6", "[1, 2, 3]::maxOf(|| &it * 2)");
    }

    @Test
    public void testMinBy() {
        // 返回产生最小值的元素，而非最小值本身
        // [3,1,2] 按 it*2 映射 -> [6,2,4]，最小值2对应元素1
        assertBoth("1", "[3, 1, 2]::minBy(|| &it * 2)");
    }

    @Test
    public void testMaxBy() {
        // 返回产生最大值的元素，而非最大值本身
        // [1,2,3] 按 -it 映射 -> [-1,-2,-3]，最大值-1对应元素1
        assertBoth("1", "[1, 2, 3]::maxBy(|| -&it)");
    }

    // ========== Grouping ==========

    @Test
    public void testGroupBy() {
        // Map 键顺序不保证，改用验证包含关系
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("[1, 2, 3, 4]::groupBy(|| &it % 2 == 0)");
        String str = result.getInterpretResult().toString();
        assertTrue(str.contains("true=[2, 4]") && str.contains("false=[1, 3]"));
        String str2 = result.getCompileResult().toString();
        assertTrue(str2.contains("true=[2, 4]") && str2.contains("false=[1, 3]"));
    }

    @Test
    public void testPartition() {
        // 使用 ImmutableMap.of，顺序固定
        assertBoth("{true=[2, 4], false=[1, 3]}", "[1, 2, 3, 4]::partition(|| &it % 2 == 0)");
    }

    @Test
    public void testChunked() {
        assertBoth("[[1, 2], [3, 4], [5]]", "[1, 2, 3, 4, 5]::chunked(2)");
    }

    // ========== Ordering ==========

    @Test
    public void testSorted() {
        assertBoth("[1, 2, 3]", "[3, 1, 2]::sorted()");
    }

    @Test
    public void testSortedDescending() {
        assertBoth("[3, 2, 1]", "[1, 3, 2]::sortedDescending()");
    }

    @Test
    public void testSortedBy() {
        assertBoth("[1, 2, 3]", "[3, 1, 2]::sortedBy(|| &it)");
    }

    @Test
    public void testSortedDescendingBy() {
        assertBoth("[3, 2, 1]", "[1, 3, 2]::sortedDescendingBy(|| &it)");
    }

    @Test
    public void testReversed() {
        assertBoth("[3, 2, 1]", "[1, 2, 3]::reversed()");
    }

    @Test
    public void testShuffled() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("[1, 2, 3]::shuffled()");
        assertNotNull(result.getInterpretResult());
        assertNotNull(result.getCompileResult());
    }

    // ========== Operations ==========

    @Test
    public void testTake() {
        assertBoth("[1, 2]", "[1, 2, 3, 4]::take(2)");
        assertBoth("[]", "[1, 2, 3]::take(0)");
        assertBoth("[1, 2, 3]", "[1, 2, 3]::take(10)");
    }

    @Test
    public void testDrop() {
        assertBoth("[3, 4]", "[1, 2, 3, 4]::drop(2)");
        assertBoth("[1, 2, 3]", "[1, 2, 3]::drop(0)");
        assertBoth("[]", "[1, 2, 3]::drop(10)");
    }

    @Test
    public void testTakeLast() {
        assertBoth("[3, 4]", "[1, 2, 3, 4]::takeLast(2)");
        assertBoth("[]", "[1, 2, 3]::takeLast(0)");
        assertBoth("[1, 2, 3]", "[1, 2, 3]::takeLast(10)");
    }

    @Test
    public void testDropLast() {
        assertBoth("[1, 2]", "[1, 2, 3, 4]::dropLast(2)");
        assertBoth("[1, 2, 3]", "[1, 2, 3]::dropLast(0)");
        assertBoth("[]", "[1, 2, 3]::dropLast(10)");
    }

    @Test
    public void testUnion() {
        assertBoth("[1, 2, 3, 4]", "[1, 2, 3]::union([2, 3, 4])");
    }

    @Test
    public void testIntersect() {
        assertBoth("[2, 3]", "[1, 2, 3]::intersect([2, 3, 4])");
    }

    @Test
    public void testSubtract() {
        assertBoth("[1]", "[1, 2, 3]::subtract([2, 3, 4])");
    }

    @Test
    public void testDistinct() {
        assertBoth("[1, 2, 3]", "[1, 2, 2, 3, 3, 3]::distinct()");
    }

    @Test
    public void testDistinctBy() {
        assertBoth("[1, 2]", "[1, 2, 3, 4]::distinctBy(|| &it % 2)");
    }

    // ========== Each (side effect) ==========

    @Test
    public void testEach() {
        assertBoth("6", "sum = 0; [1, 2, 3]::each(|| sum += &it); &sum");
    }

    // ========== Complex scenarios ==========

    @Test
    public void testChainedOperations() {
        // [1,2,3,4,5] -> filter(>2) -> [3,4,5] -> map(*2) -> [6,8,10] -> take(2) -> [6,8]
        assertBoth("[6, 8]", "[1, 2, 3, 4, 5]::filter(|| &it > 2)::map(|| &it * 2)::take(2)");
    }

    @Test
    public void testNestedCollections() {
        assertBoth("10.0", "[[1, 2], [3, 4]]::flatMap(|| &it)::sumOf(|| &it)");
    }

    // ========== Helper ==========

    private void assertBoth(String expected, String code) {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(code);
        assertEquals(expected, result.getInterpretResult().toString());
        assertEquals(expected, result.getCompileResult().toString());
    }
}
