package org.tabooproject.fluxon.ir.instruction;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRValue;
import org.tabooproject.fluxon.ir.IRVisitor;
import org.tabooproject.fluxon.ir.type.IRTypeFactory;
import org.tabooproject.fluxon.ir.type.PointerType;

/**
 * 存储指令
 * 将值存储到内存
 */
public class StoreInst extends AbstractInstruction {
    private final IRValue value; // 要存储的值
    private final IRValue pointer; // 指针
    
    /**
     * 创建存储指令
     * 
     * @param value 要存储的值
     * @param pointer 指针
     */
    public StoreInst(IRValue value, IRValue pointer) {
        super(IRTypeFactory.getInstance().getVoidType(), "store");
        this.value = value;
        this.pointer = pointer;
        
        // 检查类型兼容性
        IRType pointerType = pointer.getType();
        if (!(pointerType instanceof PointerType)) {
            throw new IllegalArgumentException("Expected pointer type, got " + pointerType);
        }
        
        IRType pointedType = ((PointerType) pointerType).getElementType();
        if (!value.getType().equals(pointedType)) {
            throw new IllegalArgumentException("Type mismatch: cannot store " + value.getType() + " to " + pointerType);
        }
    }
    
    /**
     * 获取要存储的值
     * 
     * @return 要存储的值
     */
    public IRValue getValue() {
        return value;
    }
    
    /**
     * 获取指针
     * 
     * @return 指针
     */
    public IRValue getPointer() {
        return pointer;
    }
    
    @Override
    public boolean hasSideEffects() {
        return true;
    }
    
    @Override
    protected String getInstructionName() {
        return "store";
    }
    
    @Override
    protected String getOperandsString() {
        return value + ", " + pointer;
    }
    
    @Override
    public String toString() {
        return getInstructionName() + " " + getOperandsString();
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitStoreInst(this);
    }
}