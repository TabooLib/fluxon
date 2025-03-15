package org.tabooproject.fluxon.ast.statement;

import org.tabooproject.fluxon.ast.AstNode;
import org.tabooproject.fluxon.ast.SourceLocation;

/**
 * 语句节点基类
 * 所有语句节点的公共父类
 */
public abstract class Stmt implements AstNode {
    private final SourceLocation location;
    
    /**
     * 创建语句节点
     * 
     * @param location 源代码位置
     */
    protected Stmt(SourceLocation location) {
        this.location = location;
    }
    
    @Override
    public SourceLocation getLocation() {
        return location;
    }
}