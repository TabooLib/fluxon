package org.tabooproject.fluxon.parser;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Records the source excerpt attached to a parsed node.
 * Using an identity map avoids accidental key collisions between
 * structurally equal nodes created at different points in the tree.
 */
public final class SourceTrace {

    /**
     * Weakly keys parsed nodes so entries vanish once the AST is eligible for GC.
     * This avoids unbounded growth across many independent parses while still
     * keeping a single lookup table that both the interpreter and bytecode
     * generator can consult.
     */
    private static final Map<ParseResult, SourceExcerpt> SOURCE_MAP = Collections.synchronizedMap(new WeakHashMap<>());

    private SourceTrace() {
    }

    public static void attach(ParseResult node, SourceExcerpt excerpt) {
        if (node == null || excerpt == null) {
            return;
        }
        SOURCE_MAP.put(node, excerpt);
    }

    public static void copy(ParseResult target, ParseResult source) {
        if (target == null || source == null) {
            return;
        }
        SourceExcerpt excerpt = SOURCE_MAP.get(source);
        if (excerpt != null) {
            SOURCE_MAP.put(target, excerpt);
        }
    }

    public static SourceExcerpt get(ParseResult node) {
        if (node == null) {
            return null;
        }
        return SOURCE_MAP.get(node);
    }

    public static int size() {
        return SOURCE_MAP.size();
    }
}
