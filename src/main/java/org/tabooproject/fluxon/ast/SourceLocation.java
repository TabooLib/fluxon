package org.tabooproject.fluxon.ast;

/**
 * 源代码位置信息
 * 表示 AST 节点在源代码中的位置
 */
public class SourceLocation {
    private final int startLine;
    private final int startColumn;
    private final int endLine;
    private final int endColumn;
    
    /**
     * 创建源代码位置信息
     * 
     * @param startLine 起始行
     * @param startColumn 起始列
     * @param endLine 结束行
     * @param endColumn 结束列
     */
    public SourceLocation(int startLine, int startColumn, int endLine, int endColumn) {
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }
    
    /**
     * 获取起始行
     * 
     * @return 起始行
     */
    public int getStartLine() {
        return startLine;
    }
    
    /**
     * 获取起始列
     * 
     * @return 起始列
     */
    public int getStartColumn() {
        return startColumn;
    }
    
    /**
     * 获取结束行
     * 
     * @return 结束行
     */
    public int getEndLine() {
        return endLine;
    }
    
    /**
     * 获取结束列
     * 
     * @return 结束列
     */
    public int getEndColumn() {
        return endColumn;
    }
    
    @Override
    public String toString() {
        return String.format("%d:%d-%d:%d", startLine, startColumn, endLine, endColumn);
    }
}