package org.tabooproject.fluxon.compiler.analysis;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.parser.ParsedScript;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FunctionReferencePass} 与 compile 路径上的 {@link ScriptAnalysis}。
 */
public class FunctionReferencePassTest {

    @Test
    void compileWiresScriptAnalysis() {
        CompilationContext ctx = new CompilationContext("print(\"ok\")");
        CompileResult result = Fluxon.compile(
                org.tabooproject.fluxon.runtime.FluxonRuntime.getInstance().newEnvironment(),
                ctx,
                "CompileAnalysisSmokeTest"
        );
        assertNotNull(result.getScriptAnalysis());
        assertNotNull(result.getCompilationContext());
    }

    @Test
    void topLevelCallInReferencedAndCallSites() {
        CompilationContext ctx = new CompilationContext("print(\"x\")");
        ParsedScript parsed = Fluxon.parse(ctx);
        ScriptAnalysis analysis = parsed.getScriptAnalysis();
        assertTrue(analysis.getStringSet(ScriptAnalysisKeys.REFERENCED_FUNCTIONS).contains("print"));
        List<ResolvedCallSite> sites = analysis.getResolvedCallSites();
        assertTrue(sites.stream().anyMatch(s -> "print".equals(s.getFunctionName()) && s.getEnclosingFunction() == null));
    }

    @Test
    void resolvedCallLiteralArgs() {
        String source = ""
                + "def mock_spawn(spawnerId, mobId) = { }\n"
                + "@except\n"
                + "async def stage_boss(ctx) = {\n"
                + "    mock_spawn(\"spawner_a\", \"mob.alpha\")\n"
                + "}\n";
        ScriptAnalysis analysis = parseFlowSnippet(source);
        List<ResolvedCallSite> sites = analysis.getResolvedCallSites();
        assertTrue(sites.stream().anyMatch(site ->
                "mock_spawn".equals(site.getFunctionName())
                        && "stage_boss".equals(site.getEnclosingFunction())
                        && site.getArgumentCount() >= 2
                        && "spawner_a".equals(site.getResolvedStringArg(0))
                        && "mob.alpha".equals(site.getResolvedStringArg(1))
        ));
    }

    @Test
    void localConstantPropagationForReferenceArg() {
        String source = ""
                + "def mock_spawn(spawnerId, mobId) = { }\n"
                + "@except\n"
                + "async def spawn_mob(ctx) = {\n"
                + "    mob_1 = \"mob.beta\"\n"
                + "    mock_spawn(\"spawner_b\", &mob_1)\n"
                + "}\n";
        ScriptAnalysis analysis = parseFlowSnippet(source);
        List<ResolvedCallSite> sites = analysis.getResolvedCallSites();
        ResolvedCallSite spawn = sites.stream()
                .filter(site -> "mock_spawn".equals(site.getFunctionName()))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
        assertEquals("spawn_mob", spawn.getEnclosingFunction());
        assertEquals("spawner_b", spawn.getResolvedStringArg(0));
        assertEquals("mob.beta", spawn.getResolvedStringArg(1));
    }

    private static ScriptAnalysis parseFlowSnippet(String source) {
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowInvalidReference(true);
        ParsedScript parsed = Fluxon.parse(
                ctx,
                org.tabooproject.fluxon.runtime.FluxonRuntime.getInstance().newEnvironment()
        );
        return parsed.getScriptAnalysis();
    }

    @Test
    void resolvedCallNumericLiteralArg() {
        String source = ""
                + "def mock_delay(seconds) = { }\n"
                + "@except\n"
                + "async def wait_stage(ctx) = {\n"
                + "    mock_delay(3)\n"
                + "}\n";
        ScriptAnalysis analysis = parseFlowSnippet(source);
        ResolvedCallSite site = analysis.getResolvedCallSites().stream()
                .filter(s -> "mock_delay".equals(s.getFunctionName()))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
        assertEquals(3, site.getResolvedArg(0));
    }

    @Test
    void fullyDynamicArgsStillRecordedAsCallSite() {
        String source = ""
                + "def mock_fn(a, b) = { }\n"
                + "@except\n"
                + "async def run(ctx) = {\n"
                + "    mock_fn(&ctx, &ctx)\n"
                + "}\n";
        ScriptAnalysis analysis = parseFlowSnippet(source);
        ResolvedCallSite site = analysis.getResolvedCallSites().stream()
                .filter(s -> "mock_fn".equals(s.getFunctionName()))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
        assertEquals("run", site.getEnclosingFunction());
        assertNull(site.getResolvedArg(0));
        assertNull(site.getResolvedArg(1));
        assertTrue(analysis.getStringSet(ScriptAnalysisKeys.REFERENCED_FUNCTIONS).contains("mock_fn"));
    }
}