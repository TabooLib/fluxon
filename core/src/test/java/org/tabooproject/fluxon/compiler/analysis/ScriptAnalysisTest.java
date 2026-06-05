package org.tabooproject.fluxon.compiler.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.type.TestRuntime;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ScriptAnalysis} fact 模型与 {@link ScriptAnalysisPass} 管线。
 *
 * @author sky
 */
public class ScriptAnalysisTest {

    @BeforeEach
    void setup() {
        TestRuntime.registerTestFunctions();
    }

    @Test
    void parseWiresScriptAnalysisOnContextAndParsedScript() {
        CompilationContext ctx = new CompilationContext("&hp > 0");
        ctx.defineRootVariable("hp", double.class);
        ctx.defineRootVariable("mp", double.class);

        ParsedScript script = Fluxon.parse(ctx);

        assertNotNull(script.getScriptAnalysis());
        assertNotNull(ctx.getScriptAnalysis());
        assertEquals(script.getScriptAnalysis(), ctx.getScriptAnalysis());
    }

    @Test
    void referencedRootVariablesFact() {
        CompilationContext ctx = new CompilationContext("&hp + &mp");
        ctx.defineRootVariable("hp", double.class);
        ctx.defineRootVariable("mp", double.class);
        ctx.defineRootVariable("unused", double.class);

        ParsedScript script = Fluxon.parse(ctx);
        Set<String> roots = script.getReferencedRootVariableNames();

        assertEquals(new java.util.HashSet<>(java.util.Arrays.asList("hp", "mp")), new java.util.HashSet<>(roots));
        assertTrue(roots.containsAll(script.getScriptAnalysis().getStringSet(ScriptAnalysisKeys.REFERENCED_ROOT_VARIABLES)));
    }

    @Test
    void literalExpressionHasNoRootReferences() {
        CompilationContext ctx = new CompilationContext("1 + 2");
        ParsedScript script = Fluxon.parse(ctx);

        assertTrue(script.getReferencedRootVariableNames().isEmpty());
    }

    @Test
    void referencedFunctionsFact() {
        CompilationContext ctx = new CompilationContext("print(1)");
        ParsedScript script = Fluxon.parse(ctx);

        Set<String> functions = script.getScriptAnalysis().getStringSet(ScriptAnalysisKeys.REFERENCED_FUNCTIONS);
        assertTrue(functions.contains("print"));
        assertEquals(functions, ctx.getReferencedFunctionNames());
    }

    @Test
    void customPassWritesArbitraryFact() {
        CompilationContext ctx = new CompilationContext("&x");
        ctx.defineRootVariable("x", double.class);
        ParsedScript parsed = Fluxon.parse(ctx);

        ScriptAnalysisPass custom = (results, builder) -> {
            builder.put("test.namespace.marker", "ok");
            builder.mergeString("test.namespace.tags", "a");
            builder.mergeString("test.namespace.tags", "b");
        };
        ScriptAnalysis analysis = ScriptAnalysis.analyze(
                parsed.getResults(),
                java.util.Arrays.asList(new RootVariableReferencePass(), custom)
        );

        assertEquals("ok", analysis.get("test.namespace.marker"));
        assertEquals(new java.util.HashSet<>(java.util.Arrays.asList("a", "b")), new java.util.HashSet<>(analysis.getStringSet("test.namespace.tags")));
        assertEquals(java.util.Collections.singleton("x"), analysis.getStringSet(ScriptAnalysisKeys.REFERENCED_ROOT_VARIABLES));
    }

    @Test
    void defaultPipelineMatchesDedicatedPasses() {
        CompilationContext ctx = new CompilationContext("&a + 1");
        ctx.defineRootVariable("a", double.class);

        ParsedScript parsed = Fluxon.parse(ctx);
        ScriptAnalysis fromParse = parsed.getScriptAnalysis();
        ScriptAnalysis fromDefault = ScriptAnalysis.analyze(parsed.getResults());

        assertEquals(fromParse.getStringSet(ScriptAnalysisKeys.REFERENCED_ROOT_VARIABLES), fromDefault.getStringSet(ScriptAnalysisKeys.REFERENCED_ROOT_VARIABLES));
    }
}