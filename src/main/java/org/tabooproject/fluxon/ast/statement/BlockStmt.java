package org.tabooproject.fluxon.ast.statement;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

import java.util.List;

/**
 * 块语句节点
 * 表示一组语句
 */
public class BlockStmt extends Stmt {
    private final List<Stmt> statements;
    
    /**
     * 创建块语句节点
     * 
     * @param statements 语句列表
     * @param location 源代码位置
     */
    public BlockStmt(List<Stmt> statements, SourceLocation location) {
        super(location);
        this.statements = statements;
    }
    
    /**
     * 获取语句列表
     * 
     * @return 语句列表
     */
    public List<Stmt> getStatements() {
        return statements;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitBlockStmt(this);
    }
}