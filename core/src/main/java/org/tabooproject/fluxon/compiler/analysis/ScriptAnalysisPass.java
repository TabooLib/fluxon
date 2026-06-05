package org.tabooproject.fluxon.compiler.analysis;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.List;

/**
 * 脚本 AST 静态分析步骤；通过 {@link ScriptAnalysisBuilder} 写入任意 fact 键。
 */
public interface ScriptAnalysisPass {

    void run(List<ParseResult> results, ScriptAnalysisBuilder builder);
}