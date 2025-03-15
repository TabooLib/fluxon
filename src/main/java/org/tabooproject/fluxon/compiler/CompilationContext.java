package org.tabooproject.fluxon.compiler;

import java.util.HashMap;
import java.util.Map;

/**
 * 编译上下文
 * 存储编译过程中的各种信息和中间结果
 */
public class CompilationContext {
    private final String source;
    private String sourceName;
    private final boolean strictMode;
    private final Map<String, Object> attributes = new HashMap<>();
    
    /**
     * 创建编译上下文
     * 
     * @param source 源代码
     */
    public CompilationContext(String source) {
        this.source = source;
        this.sourceName = "unknown"; // 默认源文件名
        // 检查是否启用严格模式
        this.strictMode = source.startsWith("#!strict");
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
     * 检查是否启用严格模式
     * 
     * @return 是否启用严格模式
     */
    public boolean isStrictMode() {
        return strictMode;
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