package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstNode;
import org.tabooproject.fluxon.ast.SourceLocation;

/**
 * 表达式节点基类
 * 所有表达式节点的公共父类
 */
public abstract class Expr implements AstNode {
    private final SourceLocation location;
    
    /**
     * 创建表达式节点
     * 
     * @param location 源代码位置
     */
    protected Expr(SourceLocation location) {
        this.location = location;
    }
    
    @Override
    public SourceLocation getLocation() {
        return location;
    }
}