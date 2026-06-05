package org.tabooproject.fluxon.compiler.analysis;

/**
 * 内置 {@link ScriptAnalysisPass} 写入的 fact 键名。
 * 自定义 Pass 应使用自有命名空间键，避免冲突。
 */
public final class ScriptAnalysisKeys {

    /** 脚本读取的根层级变量名，值为 {@code Set<String>} */
    public static final String REFERENCED_ROOT_VARIABLES = "referencedRootVariables";

    /** 脚本中出现的函数调用名，值为 {@code Set<String>} */
    public static final String REFERENCED_FUNCTIONS = "referencedFunctions";

    private ScriptAnalysisKeys() {
    }
}