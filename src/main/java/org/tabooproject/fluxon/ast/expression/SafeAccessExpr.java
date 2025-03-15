package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

/**
 * 空安全访问表达式节点
 * 表示空安全访问，如 user?.name
 */
public class SafeAccessExpr extends Expr {
    private final Expr object;
    private final String property;
    
    /**
     * 创建空安全访问表达式节点
     * 
     * @param object 对象表达式
     * @param property 属性名
     * @param location 源代码位置
     */
    public SafeAccessExpr(Expr object, String property, SourceLocation location) {
        super(location);
        this.object = object;
        this.property = property;
    }
    
    /**
     * 获取对象表达式
     * 
     * @return 对象表达式
     */
    public Expr getObject() {
        return object;
    }
    
    /**
     * 获取属性名
     * 
     * @return 属性名
     */
    public String getProperty() {
        return property;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitSafeAccessExpr(this);
    }
}