package org.tabooproject.fluxon.ir.instruction;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRValue;
import org.tabooproject.fluxon.ir.IRVisitor;
import org.tabooproject.fluxon.ir.type.IRTypeFactory;

/**
 * 返回指令
 * 从函数返回值
 */
public class ReturnInst extends AbstractInstruction {
    private final IRValue value; // 返回值，可能为 null
    
    /**
     * 创建返回指令
     * 
     * @param value 返回值，可能为 null
     */
    public ReturnInst(IRValue value) {
        super(IRTypeFactory.getInstance().getVoidType(), "ret");
        this.value = value;
    }
    
    /**
     * 创建无返回值的返回指令
     * 
     * @return 返回指令
     */
    public static ReturnInst createVoid() {
        return new ReturnInst(null);
    }
    
    /**
     * 获取返回值
     * 
     * @return 返回值，可能为 null
     */
    public IRValue getValue() {
        return value;
    }
    
    /**
     * 检查是否有返回值
     * 
     * @return 是否有返回值
     */
    public boolean hasValue() {
        return value != null;
    }
    
    @Override
    public boolean isTerminator() {
        return true;
    }
    
    @Override
    protected String getInstructionName() {
        return "ret";
    }
    
    @Override
    protected String getOperandsString() {
        return value != null ? value.toString() : "void";
    }
    
    @Override
    public String toString() {
        return getInstructionName() + " " + getOperandsString();
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitReturnInst(this);
    }
}