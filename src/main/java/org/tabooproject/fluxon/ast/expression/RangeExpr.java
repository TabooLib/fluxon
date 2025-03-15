package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

/**
 * 范围表达式节点
 * 表示范围表达式，如 1..10 或 start..<end
 */
public class RangeExpr extends Expr {
    private final Expr start;
    private final Expr end;
    private final boolean inclusive; // true 表示包含上界（..），false 表示排除上界（..<）
    
    /**
     * 创建范围表达式节点
     * 
     * @param start 起始表达式
     * @param end 结束表达式
     * @param inclusive 是否包含上界
     * @param location 源代码位置
     */
    public RangeExpr(Expr start, Expr end, boolean inclusive, SourceLocation location) {
        super(location);
        this.start = start;
        this.end = end;
        this.inclusive = inclusive;
    }
    
    /**
     * 获取起始表达式
     * 
     * @return 起始表达式
     */
    public Expr getStart() {
        return start;
    }
    
    /**
     * 获取结束表达式
     * 
     * @return 结束表达式
     */
    public Expr getEnd() {
        return end;
    }
    
    /**
     * 检查是否包含上界
     * 
     * @return 是否包含上界
     */
    public boolean isInclusive() {
        return inclusive;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitRangeExpr(this);
    }
}