package org.tabooproject.fluxon.compiler.analysis;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 脚本静态分析结果：按 fact 键存放各 {@link ScriptAnalysisPass} 产物。
 * 通过 {@link ScriptAnalysisPipeline} 注册 Pass；{@link #analyze} 使用默认管线。
 *
 * @author sky
 */
public final class ScriptAnalysis {

    private final Map<String, Object> facts = new LinkedHashMap<>();

    ScriptAnalysis() {
    }

    /**
     * 使用默认管线分析。
     */
    public static ScriptAnalysis analyze(List<ParseResult> results) {
        return ScriptAnalysisPipeline.run(results, ScriptAnalysisPipeline.defaultPasses());
    }

    public static ScriptAnalysis analyze(ParseResult... results) {
        return analyze(java.util.Arrays.asList(results));
    }

    /**
     * 使用指定 Pass 列表分析。
     */
    public static ScriptAnalysis analyze(List<ParseResult> results, List<ScriptAnalysisPass> passes) {
        return ScriptAnalysisPipeline.run(results, passes);
    }

    /**
     * 读取 fact；不存在时返回 null。
     */
    public Object get(String key) {
        return facts.get(key);
    }

    /**
     * 读取字符串集合 fact；类型不匹配或缺失时返回空集。
     */
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key) {
        Object value = facts.get(key);
        if (value instanceof Set) {
            return Collections.unmodifiableSet((Set<String>) value);
        }
        return Collections.emptySet();
    }

    /**
     * 读取 {@link ScriptAnalysisKeys#RESOLVED_FUNCTION_CALLS}；缺失或类型不符时返回空列表。
     */
    public List<ResolvedCallSite> getResolvedCallSites() {
        Object value = facts.get(ScriptAnalysisKeys.RESOLVED_FUNCTION_CALLS);
        if (value instanceof List) {
            List<?> raw = (List<?>) value;
            List<ResolvedCallSite> sites = new ArrayList<>(raw.size());
            for (Object item : raw) {
                if (item instanceof ResolvedCallSite) {
                    sites.add((ResolvedCallSite) item);
                }
            }
            return Collections.unmodifiableList(sites);
        }
        return Collections.emptyList();
    }

    Map<String, Object> factsView() {
        return facts;
    }

    void putFact(String key, Object value) {
        facts.put(key, value);
    }

    void mergeStringIntoSet(String key, String element) {
        if (element == null || element.isEmpty()) {
            return;
        }
        Object existing = facts.get(key);
        Set<String> set;
        if (existing instanceof Set) {
            set = (Set<String>) existing;
        } else {
            set = new LinkedHashSet<>();
            facts.put(key, set);
        }
        set.add(element);
    }
}