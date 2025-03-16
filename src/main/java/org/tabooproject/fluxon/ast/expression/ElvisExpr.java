package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

/**
 * Elvis 操作符表达式节点
 * 表示 Elvis 操作符，如 name ?: "Guest"
 */
public class ElvisExpr extends Expr {
    private final Expr condition;
    private final Expr fallback;
    
    /**
     * 创建 Elvis 操作符表达式节点
     * 
     * @param condition 条件表达式
     * @param fallback 备选表达式
     * @param location 源代码位置
     */
    public ElvisExpr(Expr condition, Expr fallback, SourceLocation location) {
        super(location);
        this.condition = condition;
        this.fallback = fallback;
    }
    
    /**
     * 获取条件表达式
     * 
     * @return 条件表达式
     */
    public Expr getCondition() {
        return condition;
    }
    
    /**
     * 获取备选表达式
     * 
     * @return 备选表达式
     */
    public Expr getFallback() {
        return fallback;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitElvisExpr(this);
    }

    @Override
    public String toString() {
        return condition + " ?: " + fallback;
    }
}