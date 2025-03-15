package org.tabooproject.fluxon.ir.instruction;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRValue;
import org.tabooproject.fluxon.ir.IRVisitor;
import org.tabooproject.fluxon.ir.type.PointerType;

/**
 * 加载指令
 * 从内存加载值
 */
public class LoadInst extends AbstractInstruction {
    private final IRValue pointer; // 指针
    
    /**
     * 创建加载指令
     * 
     * @param pointer 指针
     * @param name 指令名称
     */
    public LoadInst(IRValue pointer, String name) {
        super(getPointedType(pointer), name);
        this.pointer = pointer;
    }
    
    /**
     * 获取指针
     * 
     * @return 指针
     */
    public IRValue getPointer() {
        return pointer;
    }
    
    /**
     * 获取指针指向的类型
     * 
     * @param pointer 指针
     * @return 指针指向的类型
     */
    private static IRType getPointedType(IRValue pointer) {
        IRType pointerType = pointer.getType();
        if (!(pointerType instanceof PointerType)) {
            throw new IllegalArgumentException("Expected pointer type, got " + pointerType);
        }
        
        return ((PointerType) pointerType).getElementType();
    }
    
    @Override
    protected String getInstructionName() {
        return "load";
    }
    
    @Override
    protected String getOperandsString() {
        return type + ", " + pointer;
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitLoadInst(this);
    }
}