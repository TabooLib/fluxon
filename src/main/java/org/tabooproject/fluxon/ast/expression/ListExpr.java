package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

import java.util.List;

/**
 * 列表表达式节点
 * 表示列表字面量，如 [1, 2, 3]
 */
public class ListExpr extends Expr {
    private final List<Expr> elements;
    
    /**
     * 创建列表表达式节点
     * 
     * @param elements 元素列表
     * @param location 源代码位置
     */
    public ListExpr(List<Expr> elements, SourceLocation location) {
        super(location);
        this.elements = elements;
    }
    
    /**
     * 获取元素列表
     * 
     * @return 元素列表
     */
    public List<Expr> getElements() {
        return elements;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitListExpr(this);
    }
}