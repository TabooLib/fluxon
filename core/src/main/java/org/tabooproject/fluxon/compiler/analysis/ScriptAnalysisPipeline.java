package org.tabooproject.fluxon.compiler.analysis;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 默认 Pass 顺序：根变量读取名 → 函数调用引用（被调名 + 调用点实参静态值，见 {@link FunctionReferencePass}）。
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