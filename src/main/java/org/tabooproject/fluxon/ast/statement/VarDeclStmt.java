package org.tabooproject.fluxon.ast.statement;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;
import org.tabooproject.fluxon.ast.expression.Expr;

/**
 * 变量声明语句节点
 * 表示变量声明
 */
public class VarDeclStmt extends Stmt {
    private final String name;
    private final Expr initializer;
    private final boolean isVal; // true 表示 val（不可变），false 表示 var（可变）
    private final String typeAnnotation; // 类型注解，可能为 null
    
    /**
     * 创建变量声明语句节点
     * 
     * @param name 变量名
     * @param initializer 初始化表达式
     * @param isVal 是否为不可变变量
     * @param typeAnnotation 类型注解
     * @param location 源代码位置
     */
    public VarDeclStmt(String name, Expr initializer, boolean isVal, String typeAnnotation, SourceLocation location) {
        super(location);
        this.name = name;
        this.initializer = initializer;
        this.isVal = isVal;
        this.typeAnnotation = typeAnnotation;
    }
    
    /**
     * 获取变量名
     * 
     * @return 变量名
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取初始化表达式
     * 
     * @return 初始化表达式
     */
    public Expr getInitializer() {
        return initializer;
    }
    
    /**
     * 检查是否为不可变变量
     * 
     * @return 是否为不可变变量
     */
    public boolean isVal() {
        return isVal;
    }
    
    /**
     * 获取类型注解
     * 
     * @return 类型注解，可能为 null
     */
    public String getTypeAnnotation() {
        return typeAnnotation;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitVarDeclStmt(this);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (isVal) {
            builder.append("val ");
        } else {
            builder.append("var ");
        }
        builder.append(name);
        if (typeAnnotation != null) {
            builder.append(": ").append(typeAnnotation);
        }
        if (initializer != null) {
            builder.append(" = ").append(initializer);
        }
        return builder.toString();
    }
}