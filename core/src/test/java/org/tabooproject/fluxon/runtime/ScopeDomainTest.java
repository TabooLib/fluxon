package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 结构化并发 Scope Domain 测试
 */
public class ScopeDomainTest {

    @Test
    void testBasicScope() throws Exception {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "scope { 42 }",
            ctx -> {},
            env -> {}
        );
        
        // scope 返回 CompletableFuture
        Object value = result.getInterpretResult();
        assertTrue(value instanceof CompletableFuture);
        assertEquals(42, ((CompletableFuture<?>) value).get(5, TimeUnit.SECONDS));
    }

    @Test
    void testScopeWithAsync() throws Exception {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "result = scope { task1 = runAsync { 1 }; task2 = runAsync { 2 }; (await &task1) + (await &task2) }; await &result",
            ctx -> {},
            env -> {}
        );

        assertEquals(3, result.getInterpretResult());
    }

    @Test
    void testAsyncReturnsCompletableFuture() throws Exception {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "task = runAsync { 42 }; await &task",
            ctx -> {},
            env -> {}
        );
        
        assertEquals(42, result.getInterpretResult());
    }

    @Test
    void testSyncDomain() throws Exception {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "task = runSync { 100 }; await &task",
            ctx -> {},
            env -> {}
        );
        
        assertEquals(100, result.getInterpretResult());
    }

    @Test
    void testScopeWithAsyncAndSync() throws Exception {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "result = scope { " +
            "  asyncTask = runAsync { 10 }; " +
            "  syncTask = runSync { 20 }; " +
            "  (await &asyncTask) + (await &syncTask) " +
            "}; await &result",
            ctx -> {},
            env -> {}
        );
        
        assertEquals(30, result.getInterpretResult());
    }
}
