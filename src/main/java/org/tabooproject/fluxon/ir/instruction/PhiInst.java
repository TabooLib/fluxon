package org.tabooproject.fluxon.ir.instruction;

import org.tabooproject.fluxon.ir.IRBasicBlock;
import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRValue;
import org.tabooproject.fluxon.ir.IRVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phi 指令
 * 用于 SSA 形式中的变量合并
 */
public class PhiInst extends AbstractInstruction {
    private final Map<IRBasicBlock, IRValue> incomingValues = new HashMap<>();
    
    /**
     * 创建 Phi 指令
     * 
     * @param type 类型
     * @param name 指令名称
     */
    public PhiInst(IRType type, String name) {
        super(type, name);
    }
    
    /**
     * 添加传入值
     * 
     * @param block 基本块
     * @param value 值
     */
    public void addIncoming(IRBasicBlock block, IRValue value) {
        // 检查类型兼容性
        if (!value.getType().equals(type)) {
            throw new IllegalArgumentException("Type mismatch: expected " + type + ", but got " + value.getType());
        }
        
        incomingValues.put(block, value);
    }
    
    /**
     * 获取传入值
     * 
     * @return 传入值
     */
    public Map<IRBasicBlock, IRValue> getIncomingValues() {
        return incomingValues;
    }
    
    /**
     * 获取传入基本块
     * 
     * @return 传入基本块
     */
    public List<IRBasicBlock> getIncomingBlocks() {
        return new ArrayList<>(incomingValues.keySet());
    }
    
    /**
     * 获取传入值
     * 
     * @param block 基本块
     * @return 传入值
     */
    public IRValue getIncomingValue(IRBasicBlock block) {
        return incomingValues.get(block);
    }
    
    @Override
    protected String getInstructionName() {
        return "phi";
    }
    
    @Override
    protected String getOperandsString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(" ");
        
        boolean first = true;
        for (Map.Entry<IRBasicBlock, IRValue> entry : incomingValues.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            
            sb.append("[ ").append(entry.getValue()).append(", %").append(entry.getKey().getName()).append(" ]");
        }
        
        return sb.toString();
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitPhiInst(this);
    }
}