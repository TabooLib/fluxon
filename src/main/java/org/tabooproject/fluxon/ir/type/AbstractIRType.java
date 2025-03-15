package org.tabooproject.fluxon.ir.type;

import org.tabooproject.fluxon.ir.IRType;

/**
 * 抽象 IR 类型
 * 提供 IR 类型的基本实现
 */
public abstract class AbstractIRType implements IRType {
    protected final String name;
    
    /**
     * 创建抽象 IR 类型
     * 
     * @param name 类型名称
     */
    protected AbstractIRType(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean isIntegerType() {
        return false;
    }
    
    @Override
    public boolean isFloatType() {
        return false;
    }
    
    @Override
    public boolean isBooleanType() {
        return false;
    }
    
    @Override
    public boolean isStringType() {
        return false;
    }
    
    @Override
    public boolean isPointerType() {
        return false;
    }
    
    @Override
    public boolean isFunctionType() {
        return false;
    }
    
    @Override
    public boolean isArrayType() {
        return false;
    }
    
    @Override
    public boolean isStructType() {
        return false;
    }
    
    @Override
    public boolean isVoidType() {
        return false;
    }
    
    @Override
    public String toString() {
        return name;
    }
}