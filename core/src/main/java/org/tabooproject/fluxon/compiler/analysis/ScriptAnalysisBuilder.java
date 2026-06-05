package org.tabooproject.fluxon.compiler.analysis;

/**
 * {@link ScriptAnalysisPass} 写入 fact 的构建器。
 */
public final class ScriptAnalysisBuilder {

    private final ScriptAnalysis analysis = new ScriptAnalysis();

    /**
     * 写入任意 fact（覆盖同键旧值）。
     */
    public void put(String key, Object value) {
        analysis.putFact(key, value);
    }

    /**
     * 向字符串集合 fact 合并一个元素（无则创建 {@link java.util.LinkedHashSet}）。
     */
    public void mergeString(String setKey, String element) {
        analysis.mergeStringIntoSet(setKey, element);
    }

    ScriptAnalysis build() {
        return analysis;
    }
}