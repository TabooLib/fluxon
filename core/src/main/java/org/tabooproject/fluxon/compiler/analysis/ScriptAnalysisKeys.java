package org.tabooproject.fluxon.compiler.analysis;

/**
 * 内置 {@link ScriptAnalysisPass} 写入的 fact 键名。
 * 自定义 Pass 应使用自有命名空间键，避免冲突。
 */
public final class ScriptAnalysisKeys {

    /** 脚本读取的根层级变量名，值为 {@code Set<String>} */
    public static final String REFERENCED_ROOT_VARIABLES = "referencedRootVariables";

    /** {@link FunctionReferencePass}：全 AST 出现过的被调函数名，值为 {@code Set<String>} */
    public static final String REFERENCED_FUNCTIONS = "referencedFunctions";

    /** {@link FunctionReferencePass}：每次调用的 {@link ResolvedCallSite} 列表 */
    public static final String RESOLVED_FUNCTION_CALLS = "resolvedFunctionCalls";

    /** 与 {@link #REFERENCED_FUNCTIONS} 同内容，兼容读取；值为 {@code Set<String>} */
    public static final String RESOLVED_FUNCTION_CALL_NAMES = "resolvedFunctionCallNames";

    private ScriptAnalysisKeys() {
    }
}