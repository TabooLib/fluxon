package org.tabooproject.fluxon.compiler;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.runtime.Type;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 编译上下文
 * 存储编译过程中的各种信息和中间结果
 */
public class CompilationContext {

    private final String source;
    private final String fileName;
    private final Map<String, Object> attributes = new HashMap<>();

    // 语法特性
    private boolean allowInvalidReference = FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE;
    private boolean allowImport = FluxonFeatures.DEFAULT_ALLOW_IMPORT;
    private boolean allowReflectionAccess = FluxonFeatures.DEFAULT_ALLOW_REFLECTION_ACCESS;
    private boolean allowJavaConstruction = FluxonFeatures.DEFAULT_ALLOW_JAVA_CONSTRUCTION;

    private final List<String> packageAutoImport = FluxonFeatures.DEFAULT_PACKAGE_AUTO_IMPORT;
    private final List<String> packageBlacklist = FluxonFeatures.DEFAULT_PACKAGE_BLACKLIST;

    // 注册表
    private CommandRegistry commandRegistry = CommandRegistry.primary();
    private DomainRegistry domainRegistry = DomainRegistry.primary();
    private SyntaxMacroRegistry syntaxMacroRegistry = SyntaxMacroRegistry.primary();
    private OperatorRegistry operatorRegistry = OperatorRegistry.primary();
    private StatementMacroRegistry statementMacroRegistry = StatementMacroRegistry.primary();
    private PostfixOperatorRegistry postfixOperatorRegistry = PostfixOperatorRegistry.primary();

    // root 变量类型
    private final Map<String, Type> rootVariableTypes = new LinkedHashMap<>();
    // 强制所有根层级变量使用 localVariables 存储
    private boolean forceLocalVariables = false;

    public CompilationContext(String source) {
        this.source = source;
        this.fileName = "main";
    }

    public CompilationContext(String source, String fileName) {
        this.source = source;
        this.fileName = fileName;
    }

    public String getSource() {
        return source;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isAllowInvalidReference() {
        return allowInvalidReference;
    }

    public void setAllowInvalidReference(boolean allowInvalidReference) {
        this.allowInvalidReference = allowInvalidReference;
    }

    public boolean isAllowImport() {
        return allowImport;
    }

    public void setAllowImport(boolean allowImport) {
        this.allowImport = allowImport;
    }

    public List<String> getPackageAutoImport() {
        return packageAutoImport;
    }

    public List<String> getPackageBlacklist() {
        return packageBlacklist;
    }

    public boolean isAllowReflectionAccess() {
        return allowReflectionAccess;
    }

    public void setAllowReflectionAccess(boolean allowReflectionAccess) {
        this.allowReflectionAccess = allowReflectionAccess;
    }

    public boolean isAllowJavaConstruction() {
        return allowJavaConstruction;
    }

    public void setAllowJavaConstruction(boolean allowJavaConstruction) {
        this.allowJavaConstruction = allowJavaConstruction;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @NotNull
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    public void setCommandRegistry(@NotNull CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    @NotNull
    public DomainRegistry getDomainRegistry() {
        return domainRegistry;
    }

    public void setDomainRegistry(@NotNull DomainRegistry domainRegistry) {
        this.domainRegistry = domainRegistry;
    }

    @NotNull
    public SyntaxMacroRegistry getSyntaxMacroRegistry() {
        return syntaxMacroRegistry;
    }

    public void setSyntaxMacroRegistry(@NotNull SyntaxMacroRegistry syntaxMacroRegistry) {
        this.syntaxMacroRegistry = syntaxMacroRegistry;
    }

    @NotNull
    public OperatorRegistry getOperatorRegistry() {
        return operatorRegistry;
    }

    public void setOperatorRegistry(@NotNull OperatorRegistry operatorRegistry) {
        this.operatorRegistry = operatorRegistry;
    }

    @NotNull
    public StatementMacroRegistry getStatementMacroRegistry() {
        return statementMacroRegistry;
    }

    public void setStatementMacroRegistry(@NotNull StatementMacroRegistry statementMacroRegistry) {
        this.statementMacroRegistry = statementMacroRegistry;
    }

    @NotNull
    public PostfixOperatorRegistry getPostfixOperatorRegistry() {
        return postfixOperatorRegistry;
    }

    public void setPostfixOperatorRegistry(@NotNull PostfixOperatorRegistry postfixOperatorRegistry) {
        this.postfixOperatorRegistry = postfixOperatorRegistry;
    }

    /**
     * 定义单个 root 变量类型
     *
     * @param name 变量名
     * @param type 类型
     * @return this
     */
    public CompilationContext defineRootVariable(String name, Class<?> type) {
        rootVariableTypes.put(name, Type.fromClass(type));
        return this;
    }

    /**
     * 批量定义 root 变量类型
     *
     * @param variables 变量映射
     * @return this
     */
    public CompilationContext defineRootVariables(Map<String, Class<?>> variables) {
        for (Map.Entry<String, Class<?>> entry : variables.entrySet()) {
            rootVariableTypes.put(entry.getKey(), Type.fromClass(entry.getValue()));
        }
        return this;
    }

    /**
     * 获取 root 变量类型
     */
    public Map<String, Type> getRootVariableTypes() {
        return rootVariableTypes;
    }

    /**
     * 是否强制所有根层级变量使用 localVariables 存储
     */
    public boolean isForceLocalVariables() {
        return forceLocalVariables;
    }

    /**
     * 设置是否强制所有根层级变量使用 localVariables 存储
     *
     * @param forceLocalVariables true 表示强制使用临时变量
     */
    public void setForceLocalVariables(boolean forceLocalVariables) {
        this.forceLocalVariables = forceLocalVariables;
    }
}
