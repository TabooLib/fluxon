package org.tabooproject.fluxon.ir.instruction;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRVisitor;
import org.tabooproject.fluxon.ir.type.IRTypeFactory;

/**
 * 分配指令
 * 在栈上分配内存
 */
public class AllocaInst extends AbstractInstruction {
    private final IRType allocatedType; // 分配的类型
    
    /**
     * 创建分配指令
     * 
     * @param allocatedType 分配的类型
     * @param name 指令名称
     */
    public AllocaInst(IRType allocatedType, String name) {
        super(IRTypeFactory.getInstance().getPointerType(allocatedType), name);
        this.allocatedType = allocatedType;
    }
    
    /**
     * 获取分配的类型
     * 
     * @return 分配的类型
     */
    public IRType getAllocatedType() {
        return allocatedType;
    }
    
    @Override
    protected String getInstructionName() {
        return "alloca";
    }
    
    @Override
    protected String getOperandsString() {
        return allocatedType.toString();
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitAllocaInst(this);
    }
}