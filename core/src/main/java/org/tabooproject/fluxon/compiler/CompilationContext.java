package org.tabooproject.fluxon.compiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 编译上下文
 * 存储编译过程中的各种信息和中间结果
 */
public class CompilationContext {

    private final String source;
    private final Map<String, Object> attributes = new HashMap<>();

    // 语法特性
    private boolean allowKetherStyleCall = FluxonFeatures.DEFAULT_ALLOW_KETHER_STYLE_CALL;
    private boolean allowInvalidReference = FluxonFeatures.DEFAULT_ALLOW_INVALID_REFERENCE;
    private boolean allowImport = FluxonFeatures.DEFAULT_ALLOW_IMPORT;

    private final List<String> packetAutoImport = FluxonFeatures.DEFAULT_PACKET_AUTO_IMPORT;
    private final List<String> packageBlacklist = FluxonFeatures.DEFAULT_PACKAGE_BLACKLIST;

    /**
     * 创建编译上下文
     */
    public CompilationContext(String source) {
        this.source = source;
    }
    
    /**
     * 获取源代码
     */
    public String getSource() {
        return source;
    }
    
    /**
     * 是否允许无括号调用（实验功能）
     */
    public boolean isAllowKetherStyleCall() {
        return allowKetherStyleCall;
    }

    /**
     * 设置是否允许无括号调用（实验功能）
     */
    public void setAllowKetherStyleCall(boolean allowKetherStyleCall) {
        this.allowKetherStyleCall = allowKetherStyleCall;
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
    public List<String> getPacketAutoImport() {
        return packetAutoImport;
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
     * @param key 属性键
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
}