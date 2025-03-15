package org.tabooproject.fluxon.ast.statement;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;
import org.tabooproject.fluxon.ast.expression.Expr;

/**
 * 返回语句节点
 * 表示返回语句
 */
public class ReturnStmt extends Stmt {
    private final Expr value; // 返回值表达式，可能为 null
    
    /**
     * 创建返回语句节点
     * 
     * @param value 返回值表达式
     * @param location 源代码位置
     */
    public ReturnStmt(Expr value, SourceLocation location) {
        super(location);
        this.value = value;
    }
    
    /**
     * 获取返回值表达式
     * 
     * @return 返回值表达式，可能为 null
     */
    public Expr getValue() {
        return value;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitReturnStmt(this);
    }
}