package org.tabooproject.fluxon.ast.statement;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;
import org.tabooproject.fluxon.ast.expression.Expr;

/**
 * Await 语句
 * 表示异步等待表达式的结果
 */
public class AwaitStmt extends Stmt {
    private final Expr expression;
    
    /**
     * 创建 Await 语句
     * 
     * @param expression 等待的表达式
     * @param location 源代码位置
     */
    public AwaitStmt(Expr expression, SourceLocation location) {
        super(location);
        this.expression = expression;
    }
    
    /**
     * 获取等待的表达式
     * 
     * @return 等待的表达式
     */
    public Expr getExpression() {
        return expression;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitAwaitStmt(this);
    }
}