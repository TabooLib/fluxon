package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

/**
 * 条件表达式节点
 * 表示 if-then-else 表达式
 */
public class IfExpr extends Expr {
    private final Expr condition;
    private final Expr thenBranch;
    private final Expr elseBranch;
    
    /**
     * 创建条件表达式节点
     * 
     * @param condition 条件表达式
     * @param thenBranch then 分支
     * @param elseBranch else 分支
     * @param location 源代码位置
     */
    public IfExpr(Expr condition, Expr thenBranch, Expr elseBranch, SourceLocation location) {
        super(location);
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
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
     * 获取 then 分支
     * 
     * @return then 分支
     */
    public Expr getThenBranch() {
        return thenBranch;
    }
    
    /**
     * 获取 else 分支
     * 
     * @return else 分支
     */
    public Expr getElseBranch() {
        return elseBranch;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitIfExpr(this);
    }

    @Override
    public String toString() {
        return "if (" + condition + ") then { " + thenBranch + " } else { " + elseBranch + " }";
    }
}