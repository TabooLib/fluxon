package org.tabooproject.fluxon.ir.instruction;

import org.tabooproject.fluxon.ir.IRBasicBlock;
import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRValue;
import org.tabooproject.fluxon.ir.IRVisitor;
import org.tabooproject.fluxon.ir.type.IRTypeFactory;

/**
 * 条件分支指令
 * 根据条件跳转到不同的基本块
 */
public class CondBranchInst extends AbstractInstruction {
    private final IRValue condition; // 条件
    private final IRBasicBlock thenBlock; // 条件为真时跳转的基本块
    private final IRBasicBlock elseBlock; // 条件为假时跳转的基本块
    
    /**
     * 创建条件分支指令
     * 
     * @param condition 条件
     * @param thenBlock 条件为真时跳转的基本块
     * @param elseBlock 条件为假时跳转的基本块
     */
    public CondBranchInst(IRValue condition, IRBasicBlock thenBlock, IRBasicBlock elseBlock) {
        super(IRTypeFactory.getInstance().getVoidType(), "br");
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
        
        // 检查条件类型
        if (!condition.getType().isBooleanType()) {
            throw new IllegalArgumentException("Condition must be of boolean type, got " + condition.getType());
        }
        
        // 添加控制流边
        if (parent != null) {
            parent.addSuccessor(thenBlock);
            parent.addSuccessor(elseBlock);
        }
    }
    
    /**
     * 获取条件
     * 
     * @return 条件
     */
    public IRValue getCondition() {
        return condition;
    }
    
    /**
     * 获取条件为真时跳转的基本块
     * 
     * @return 条件为真时跳转的基本块
     */
    public IRBasicBlock getThenBlock() {
        return thenBlock;
    }
    
    /**
     * 获取条件为假时跳转的基本块
     * 
     * @return 条件为假时跳转的基本块
     */
    public IRBasicBlock getElseBlock() {
        return elseBlock;
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
        return condition + ", label %" + thenBlock.getName() + ", label %" + elseBlock.getName();
    }
    
    @Override
    public String toString() {
        return getInstructionName() + " " + getOperandsString();
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitCondBranchInst(this);
    }
}