package org.tabooproject.fluxon.ir.instruction;

import org.tabooproject.fluxon.ir.IRBasicBlock;
import org.tabooproject.fluxon.ir.IRInstruction;
import org.tabooproject.fluxon.ir.IRType;

/**
 * 抽象指令
 * 提供 IR 指令的基本实现
 */
public abstract class AbstractInstruction implements IRInstruction {
    protected final IRType type;
    protected final String name;
    protected IRBasicBlock parent;
    
    /**
     * 创建抽象指令
     * 
     * @param type 指令类型
     * @param name 指令名称
     */
    protected AbstractInstruction(IRType type, String name) {
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
    public IRBasicBlock getParent() {
        return parent;
    }
    
    @Override
    public void setParent(IRBasicBlock parent) {
        this.parent = parent;
    }
    
    @Override
    public boolean isConstant() {
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
    public boolean isTerminator() {
        return false;
    }
    
    @Override
    public boolean hasSideEffects() {
        return false;
    }
    
    @Override
    public String toString() {
        return name + " = " + getInstructionName() + " " + getOperandsString();
    }
    
    /**
     * 获取指令名称
     * 
     * @return 指令名称
     */
    protected abstract String getInstructionName();
    
    /**
     * 获取操作数字符串
     * 
     * @return 操作数字符串
     */
    protected abstract String getOperandsString();
}