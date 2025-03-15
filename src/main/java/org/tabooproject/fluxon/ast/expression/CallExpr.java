package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

import java.util.List;

/**
 * 函数调用表达式节点
 * 表示函数调用
 */
public class CallExpr extends Expr {
    private final String name;
    private final List<Expr> arguments;
    
    /**
     * 创建函数调用表达式节点
     * 
     * @param name 函数名
     * @param arguments 参数列表
     * @param location 源代码位置
     */
    public CallExpr(String name, List<Expr> arguments, SourceLocation location) {
        super(location);
        this.name = name;
        this.arguments = arguments;
    }
    
    /**
     * 获取函数名
     * 
     * @return 函数名
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取参数列表
     * 
     * @return 参数列表
     */
    public List<Expr> getArguments() {
        return arguments;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitCallExpr(this);
    }
}