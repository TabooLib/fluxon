package org.tabooproject.fluxon.compiler;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.parser.CommandRegistry;

import java.util.HashMap;
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

    private final List<String> packageAutoImport = FluxonFeatures.DEFAULT_PACKAGE_AUTO_IMPORT;
    private final List<String> packageBlacklist = FluxonFeatures.DEFAULT_PACKAGE_BLACKLIST;

    // Command 注册表
    private CommandRegistry commandRegistry = CommandRegistry.primary();

    /**
     * 创建编译上下文
     */
    public CompilationContext(String source) {
        this.source = source;
        this.fileName = "main";
    }

    public CompilationContext(String source, String fileName) {
        this.source = source;
        this.fileName = fileName;
    }

    /**
     * 获取源代码
     */
    public String getSource() {
        return source;
    }

    /**
     * 获取文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 是否允许无效引用
     */
    public boolean isAllowInvalidReference() {
        return allowInvalidReference;
    }

    /**
     * 设置是否允许无效引用
     */
    public void setAllowInvalidReference(boolean allowInvalidReference) {
        this.allowInvalidReference = allowInvalidReference;
    }

    /**
     * 是否允许导入
     */
    public boolean isAllowImport() {
        return allowImport;
    }

    /**
     * 设置是否允许导入
     */
    public void setAllowImport(boolean allowImport) {
        this.allowImport = allowImport;
    }

    /**
     * 获取自动导入的包
     */
    public List<String> getPackageAutoImport() {
        return packageAutoImport;
    }

    /**
     * 获取包黑名单
     */
    public List<String> getPackageBlacklist() {
        return packageBlacklist;
    }

    /**
     * 设置属性
     *
     * @param key   属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取属性
     *
     * @param key 属性键
     * @param <T> 属性类型
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * 获取所有属性
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * 获取 Command 注册表
     * 默认返回 {@link CommandRegistry#primary()}，除非通过 {@link #setCommandRegistry(CommandRegistry)} 设置了自定义注册表。
     */
    @NotNull
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    /**
     * 设置自定义 Command 注册表
     * 用于需要隔离 command 集合的场景，如沙箱环境或多租户系统。
     */
    public void setCommandRegistry(@NotNull CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }
}