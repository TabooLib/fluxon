package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

/**
 * 字面量表达式节点
 * 表示字符串、数字、布尔值等字面量
 */
public class LiteralExpr extends Expr {
    private final Object value;
    private final LiteralType type;
    
    /**
     * 创建字面量表达式节点
     * 
     * @param value 字面量值
     * @param type 字面量类型
     * @param location 源代码位置
     */
    public LiteralExpr(Object value, LiteralType type, SourceLocation location) {
        super(location);
        this.value = value;
        this.type = type;
    }
    
    /**
     * 获取字面量值
     * 
     * @return 字面量值
     */
    public Object getValue() {
        return value;
    }
    
    /**
     * 获取字面量类型
     * 
     * @return 字面量类型
     */
    public LiteralType getType() {
        return type;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitLiteralExpr(this);
    }
    
    /**
     * 字面量类型
     */
    public enum LiteralType {
        /**
         * 字符串字面量
         */
        STRING,
        
        /**
         * 整数字面量
         */
        INTEGER,
        
        /**
         * 浮点数字面量
         */
        FLOAT,
        
        /**
         * 布尔字面量
         */
        BOOLEAN,
        
        /**
         * 空值字面量
         */
        NULL,
        
        /**
         * 标识符字面量
         */
        IDENTIFIER
    }
}