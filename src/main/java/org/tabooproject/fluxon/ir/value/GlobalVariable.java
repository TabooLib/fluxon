package org.tabooproject.fluxon.ir.value;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRVisitor;

/**
 * 全局变量
 * 表示全局变量
 */
public class GlobalVariable extends AbstractValue {
    private final Constant initializer; // 初始值，可能为 null
    private final boolean isConstant; // 是否为常量（不可变）
    
    /**
     * 创建全局变量
     * 
     * @param name 变量名
     * @param type 变量类型
     * @param initializer 初始值
     * @param isConstant 是否为常量
     */
    public GlobalVariable(String name, IRType type, Constant initializer, boolean isConstant) {
        super(type, name);
        this.initializer = initializer;
        this.isConstant = isConstant;
    }
    
    /**
     * 获取初始值
     * 
     * @return 初始值
     */
    public Constant getInitializer() {
        return initializer;
    }
    
    /**
     * 检查是否为常量
     * 
     * @return 是否为常量
     */
    public boolean isConstant() {
        return isConstant;
    }
    
    @Override
    public boolean isGlobalVariable() {
        return true;
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitGlobalVariable(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(name).append(": ").append(type);
        
        if (initializer != null) {
            sb.append(" = ").append(initializer);
        }
        
        return sb.toString();
    }
}