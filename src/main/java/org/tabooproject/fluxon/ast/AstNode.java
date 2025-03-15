package org.tabooproject.fluxon.ast;

/**
 * AST 节点基类
 * 所有 AST 节点类型的公共接口
 */
public interface AstNode {
    /**
     * 接受访问者
     * 
     * @param visitor 访问者
     * @param <T> 访问结果类型
     * @return 访问结果
     */
    <T> T accept(AstVisitor<T> visitor);
    
    /**
     * 获取节点的位置信息
     * 
     * @return 位置信息
     */
    SourceLocation getLocation();
}