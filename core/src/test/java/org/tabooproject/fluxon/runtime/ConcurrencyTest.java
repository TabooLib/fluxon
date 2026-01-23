package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.RepeatedTest;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.tabooproject.fluxon.FluxonTestUtil.assertBothEqual;

/**
 * 并发安全测试：验证 Interpreter 双槽（resultRef/resultPrimitive）在多线程下的安全性
 * 同时验证解释执行和编译执行两条路径
 */
public class ConcurrencyTest {

    // ========== async 函数并发 ==========

    @RepeatedTest(50)
    void testAsyncFunctionConcurrentCalls() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "async def compute(x) = { return &x * &x + &x } " +
            "a = compute(3); b = compute(5); c = compute(7); " +
            "(await &a) + (await &b) + (await &c)"
        );
        // 3*3+3=12, 5*5+5=30, 7*7+7=56 → 98
        assertBothEqual(98, result);
    }

    @RepeatedTest(50)
    void testManyAsyncFunctionCalls() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "async def add(a, b) = { return &a + &b } " +
            "t1 = add(1, 2); t2 = add(3, 4); t3 = add(5, 6); t4 = add(7, 8); t5 = add(9, 10); " +
            "(await &t1) + (await &t2) + (await &t3) + (await &t4) + (await &t5)"
        );
        // 3 + 7 + 11 + 15 + 19 = 55
        assertBothEqual(55, result);
    }

    // ========== scope + runAsync 并发 ==========

    @RepeatedTest(50)
    void testScopeRunAsyncRace() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "result = scope { " +
            "  a = runAsync { 1 + 2 + 3 + 4 + 5 }; " +
            "  b = runAsync { 10 + 20 + 30 + 40 + 50 }; " +
            "  c = runAsync { 100 + 200 + 300 }; " +
            "  (await &a) + (await &b) + (await &c) " +
            "}; await &result"
        );
        assertBothEqual(765, result);
    }

    @RepeatedTest(50)
    void testScopeManyConcurrentTasks() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "result = scope { " +
            "  t1 = runAsync { 1 * 1 }; " +
            "  t2 = runAsync { 2 * 2 }; " +
            "  t3 = runAsync { 3 * 3 }; " +
            "  t4 = runAsync { 4 * 4 }; " +
            "  t5 = runAsync { 5 * 5 }; " +
            "  (await &t1) + (await &t2) + (await &t3) + (await &t4) + (await &t5) " +
            "}; await &result"
        );
        // 1 + 4 + 9 + 16 + 25 = 55
        assertBothEqual(55, result);
    }

    // ========== 直接 runAsync（不在 scope 内）==========

    @RepeatedTest(50)
    void testDirectRunAsyncRace() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "a = runAsync { 1 + 2 + 3 + 4 + 5 }; " +
            "b = runAsync { 10 + 20 + 30 + 40 + 50 }; " +
            "c = runAsync { 100 + 200 + 300 }; " +
            "(await &a) + (await &b) + (await &c)"
        );
        assertBothEqual(765, result);
    }

    @RepeatedTest(50)
    void testDirectRunAsyncHeavy() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "a = runAsync { 1+2+3+4+5+6+7+8+9+10 }; " +
            "b = runAsync { 11+12+13+14+15+16+17+18+19+20 }; " +
            "c = runAsync { 21+22+23+24+25+26+27+28+29+30 }; " +
            "(await &a) + (await &b) + (await &c)"
        );
        // 55 + 155 + 255 = 465
        assertBothEqual(465, result);
    }
}
