package org.tabooproject.fluxon.semantic;

import org.tabooproject.fluxon.ast.SourceLocation;

/**
 * 语义错误
 * 表示语义分析过程中发现的错误
 */
public class SemanticError {
    private final String message;
    private final SourceLocation location;
    
    /**
     * 创建语义错误
     * 
     * @param message 错误消息
     * @param location 源代码位置
     */
    public SemanticError(String message, SourceLocation location) {
        this.message = message;
        this.location = location;
    }
    
    /**
     * 获取错误消息
     * 
     * @return 错误消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取源代码位置
     * 
     * @return 源代码位置
     */
    public SourceLocation getLocation() {
        return location;
    }
    
    @Override
    public String toString() {
        return String.format("[%d:%d-%d:%d] %s",
                location.getStartLine(), location.getStartColumn(),
                location.getEndLine(), location.getEndColumn(),
                message);
    }
}