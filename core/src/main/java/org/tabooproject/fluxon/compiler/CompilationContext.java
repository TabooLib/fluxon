package org.tabooproject.fluxon.compiler;

import java.util.HashMap;
import java.util.Map;

/**
 * 编译上下文
 * 存储编译过程中的各种信息和中间结果
 */
public class CompilationContext {

    // 全局是否允许无括号调用
    public static boolean DEFAULT_ALLOW_KETHER_STYLE_CALL = false;
    // 全局是否允许无效引用
    public static boolean DEFAULT_ALLOW_INVALID_REFERENCE = false;

    private final String source;
    private String sourceName;
    private boolean allowKetherStyleCall = DEFAULT_ALLOW_KETHER_STYLE_CALL;
    private boolean allowInvalidReference = DEFAULT_ALLOW_INVALID_REFERENCE;
    private final Map<String, Object> attributes = new HashMap<>();
    
    /**
     * 创建编译上下文
     * 
     * @param source 源代码
     */
    public CompilationContext(String source) {
        this.source = source;
        this.sourceName = "unknown"; // 默认源文件名
    }
    
    /**
     * 获取源代码
     * 
     * @return 源代码
     */
    public String getSource() {
        return source;
    }
    
    /**
     * 获取源文件名
     * 
     * @return 源文件名
     */
    public String getSourceName() {
        return sourceName;
    }
    
    /**
     * 设置源文件名
     * 
     * @param sourceName 源文件名
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
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
}