package org.tabooproject.fluxon.ir.instruction;

import org.tabooproject.fluxon.ir.IRBasicBlock;
import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRVisitor;
import org.tabooproject.fluxon.ir.type.IRTypeFactory;

/**
 * 无条件分支指令
 * 无条件跳转到目标基本块
 */
public class BranchInst extends AbstractInstruction {
    private final IRBasicBlock target; // 目标基本块
    
    /**
     * 创建无条件分支指令
     * 
     * @param target 目标基本块
     */
    public BranchInst(IRBasicBlock target) {
        super(IRTypeFactory.getInstance().getVoidType(), "br");
        this.target = target;
        
        // 添加控制流边
        if (parent != null) {
            parent.addSuccessor(target);
        }
    }
    
    /**
     * 获取目标基本块
     * 
     * @return 目标基本块
     */
    public IRBasicBlock getTarget() {
        return target;
    }
    
    @Override
    public boolean isTerminator() {
        return true;
    }
    
    @Override
    protected String getInstructionName() {
        return "br";
    }
    
    @Override
    protected String getOperandsString() {
        return "label %" + target.getName();
    }
    
    @Override
    public String toString() {
        return getInstructionName() + " " + getOperandsString();
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitBranchInst(this);
    }
}