package org.tabooproject.fluxon.ast.statement;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;
import org.tabooproject.fluxon.ast.expression.Expr;

/**
 * 表达式语句节点
 * 表示表达式作为语句使用的情况
 */
public class ExpressionStmt extends Stmt {
    private final Expr expression;
    
    /**
     * 创建表达式语句节点
     * 
     * @param expression 表达式
     * @param location 源代码位置
     */
    public ExpressionStmt(Expr expression, SourceLocation location) {
        super(location);
        this.expression = expression;
    }
    
    /**
     * 获取表达式
     * 
     * @return 表达式
     */
    public Expr getExpression() {
        return expression;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitExpressionStmt(this);
    }
}