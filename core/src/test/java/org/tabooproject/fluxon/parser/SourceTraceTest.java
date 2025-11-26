package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SourceTraceTest {

    private Map<ParseResult, SourceExcerpt> sourceMap;

    @BeforeEach
    void setUp() throws Exception {
        sourceMap = getSourceMap();
        synchronized (sourceMap) {
            sourceMap.clear();
        }
    }

    @Test
    void keepsEntryWhileNodeAlive() {
        DummyNode node = new DummyNode();
        SourceExcerpt excerpt = SourceExcerpt.from("main", "x", 1, 1);
        SourceTrace.attach(node, excerpt);
        synchronized (sourceMap) {
            assertEquals(1, sourceMap.size());
            assertSame(excerpt, sourceMap.get(node));
        }
    }

    @Test
    void clearsEntryAfterGc() throws Exception {
        DummyNode node = new DummyNode();
        SourceExcerpt excerpt = SourceExcerpt.from("main", "x", 1, 1);
        SourceTrace.attach(node, excerpt);
        WeakReference<DummyNode> ref = new WeakReference<>(node);
        // Drop strong refs
        node = null;
        excerpt = null;
        forceGcSweep();
        synchronized (sourceMap) {
            assertEquals(0, sourceMap.size(), "weak entries should be cleared after GC");
        }
        assertSame(null, ref.get(), "node reference should be cleared");
    }

    private void forceGcSweep() throws InterruptedException {
        // Try a few rounds to give the GC and WeakHashMap cleanup a chance to run
        for (int i = 0; i < 20; i++) {
            System.gc();
            // size() triggers WeakHashMap expunge when synchronized
            synchronized (sourceMap) {
                sourceMap.size();
            }
            if (sourceMap.isEmpty()) {
                return;
            }
            Thread.sleep(50);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<ParseResult, SourceExcerpt> getSourceMap() throws Exception {
        Field field = SourceTrace.class.getDeclaredField("SOURCE_MAP");
        field.setAccessible(true);
        Object value = field.get(null);
        assertNotNull(value);
        return (Map<ParseResult, SourceExcerpt>) value;
    }

    private static final class DummyNode implements ParseResult {
        @Override
        public ResultType getType() {
            return ResultType.EXPRESSION;
        }

        @Override
        public String toPseudoCode() {
            return "dummy";
        }
    }
}
