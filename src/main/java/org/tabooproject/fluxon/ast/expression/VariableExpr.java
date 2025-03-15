package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

/**
 * 变量引用表达式节点
 * 表示对变量的引用（使用 & 前缀）
 */
public class VariableExpr extends Expr {
    private final String name;
    private final boolean reference;
    
    /**
     * 创建变量引用表达式节点
     * 
     * @param name 变量名
     * @param location 源代码位置
     */
    public VariableExpr(String name, SourceLocation location) {
        this(name, false, location);
    }
    
    /**
     * 创建变量引用表达式节点
     * 
     * @param name 变量名
     * @param reference 是否为引用
     * @param location 源代码位置
     */
    public VariableExpr(String name, boolean reference, SourceLocation location) {
        super(location);
        this.name = name;
        this.reference = reference;
    }
    
    /**
     * 获取变量名
     * 
     * @return 变量名
     */
    public String getName() {
        return name;
    }
    
    /**
     * 检查是否为引用
     * 
     * @return 是否为引用
     */
    public boolean isReference() {
        return reference;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitVariableExpr(this);
    }
}