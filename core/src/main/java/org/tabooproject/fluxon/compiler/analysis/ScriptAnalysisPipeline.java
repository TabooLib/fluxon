package org.tabooproject.fluxon.compiler.analysis;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 可注册的脚本分析管线；默认包含根变量与函数引用 Pass，可追加自定义 Pass。
 */
public final class ScriptAnalysisPipeline {

    private static final CopyOnWriteArrayList<ScriptAnalysisPass> REGISTERED =
            new CopyOnWriteArrayList<>(Arrays.asList(
                    new RootVariableReferencePass(),
                    new FunctionReferencePass()
            ));

    private ScriptAnalysisPipeline() {
    }

    /**
     * 注册额外分析 Pass（在默认 Pass 之后执行）。
     */
    public static void registerPass(ScriptAnalysisPass pass) {
        if (pass != null) {
            REGISTERED.add(pass);
        }
    }

    /**
     * 当前管线中的 Pass 快照（含默认与已注册）。
     */
    public static List<ScriptAnalysisPass> defaultPasses() {
        return Collections.unmodifiableList(new ArrayList<>(REGISTERED));
    }

    static ScriptAnalysis run(List<ParseResult> results, List<ScriptAnalysisPass> passes) {
        ScriptAnalysisBuilder builder = new ScriptAnalysisBuilder();
        if (results != null && passes != null) {
            for (ScriptAnalysisPass pass : passes) {
                pass.run(results, builder);
            }
        }
        return builder.build();
    }
}