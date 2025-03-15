package org.tabooproject.fluxon.ir.value;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRValue;

/**
 * 抽象值
 * 提供 IR 值的基本实现
 */
public abstract class AbstractValue implements IRValue {
    protected final IRType type;
    protected final String name;
    
    /**
     * 创建抽象值
     * 
     * @param type 值类型
     * @param name 值名称
     */
    protected AbstractValue(IRType type, String name) {
        this.type = type;
        this.name = name;
    }
    
    @Override
    public IRType getType() {
        return type;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean isConstant() {
        return false;
    }
    
    @Override
    public boolean isInstruction() {
        return false;
    }
    
    @Override
    public boolean isGlobalVariable() {
        return false;
    }
    
    @Override
    public boolean isParameter() {
        return false;
    }
    
    @Override
    public String toString() {
        return name + ": " + type;
    }
}