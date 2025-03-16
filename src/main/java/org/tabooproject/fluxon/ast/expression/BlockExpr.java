package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;
import org.tabooproject.fluxon.ast.statement.BlockStmt;

/**
 * 块表达式节点
 * 表示一个块作为表达式使用的情况
 */
public class BlockExpr extends Expr {
    private final BlockStmt block;
    
    /**
     * 创建块表达式节点
     * 
     * @param block 块语句
     * @param location 源代码位置
     */
    public BlockExpr(BlockStmt block, SourceLocation location) {
        super(location);
        this.block = block;
    }
    
    /**
     * 获取块语句
     * 
     * @return 块语句
     */
    public BlockStmt getBlock() {
        return block;
    }

    @Override
    public String toString() {
        return block.toString();
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitBlockExpr(this);
    }
}