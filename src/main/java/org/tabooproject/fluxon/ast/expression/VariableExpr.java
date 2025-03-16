package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

/**
 * 变量引用表达式节点
 * 表示对变量的引用（使用 & 前缀）
 */
public class VariableExpr extends Expr {
    private final String name;

    /**
     * 创建变量引用表达式节点
     * 
     * @param name 变量名
     * @param location 源代码位置
     */
    public VariableExpr(String name, SourceLocation location) {
        super(location);
        this.name = name;
    }
    
    /**
     * 获取变量名
     * 
     * @return 变量名
     */
    public String getName() {
        return name;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitVariableExpr(this);
    }

    @Override
    public String toString() {
        return "&" + name;
    }
}