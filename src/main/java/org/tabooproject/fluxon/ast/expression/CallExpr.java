package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

import java.util.List;

/**
 * 函数调用表达式节点
 * 表示函数调用
 */
public class CallExpr extends Expr {
    private final String functionName;
    private final List<Expr> arguments;
    
    /**
     * 创建函数调用表达式节点
     * 
     * @param functionName 函数名
     * @param arguments 参数列表
     * @param location 源代码位置
     */
    public CallExpr(String functionName, List<Expr> arguments, SourceLocation location) {
        super(location);
        this.functionName = functionName;
        this.arguments = arguments;
    }
    
    /**
     * 获取函数名
     * 
     * @return 函数名
     */
    public String getFunctionName() {
        return functionName;
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

    @Override
    public String toString() {
        return functionName + "(" + String.join(", ", arguments.stream().map(Object::toString).toList()) + ")";
    }
}