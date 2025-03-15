package org.tabooproject.fluxon.ir;

import java.util.ArrayList;
import java.util.List;

/**
 * IR 基本块
 * 表示一个基本块，包含指令序列
 */
public class IRBasicBlock implements IRNode {
    private final String name;
    private final List<IRInstruction> instructions = new ArrayList<>();
    private IRFunction parent;
    private final List<IRBasicBlock> predecessors = new ArrayList<>();
    private final List<IRBasicBlock> successors = new ArrayList<>();
    
    /**
     * 创建 IR 基本块
     * 
     * @param name 基本块名称
     * @param parent 父函数
     */
    public IRBasicBlock(String name, IRFunction parent) {
        this.name = name;
        this.parent = parent;
    }
    
    /**
     * 获取基本块名称
     * 
     * @return 基本块名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取父函数
     * 
     * @return 父函数
     */
    public IRFunction getParent() {
        return parent;
    }
    
    /**
     * 设置父函数
     * 
     * @param parent 父函数
     */
    public void setParent(IRFunction parent) {
        this.parent = parent;
    }
    
    /**
     * 获取指令列表
     * 
     * @return 指令列表
     */
    public List<IRInstruction> getInstructions() {
        return instructions;
    }
    
    /**
     * 添加指令
     * 
     * @param instruction 指令
     */
    public void addInstruction(IRInstruction instruction) {
        instructions.add(instruction);
        instruction.setParent(this);
    }
    
    /**
     * 获取前驱基本块
     * 
     * @return 前驱基本块
     */
    public List<IRBasicBlock> getPredecessors() {
        return predecessors;
    }
    
    /**
     * 添加前驱基本块
     * 
     * @param predecessor 前驱基本块
     */
    public void addPredecessor(IRBasicBlock predecessor) {
        if (!predecessors.contains(predecessor)) {
            predecessors.add(predecessor);
            predecessor.addSuccessor(this);
        }
    }
    
    /**
     * 获取后继基本块
     * 
     * @return 后继基本块
     */
    public List<IRBasicBlock> getSuccessors() {
        return successors;
    }
    
    /**
     * 添加后继基本块
     * 
     * @param successor 后继基本块
     */
    public void addSuccessor(IRBasicBlock successor) {
        if (!successors.contains(successor)) {
            successors.add(successor);
            successor.addPredecessor(this);
        }
    }
    
    /**
     * 获取终止指令
     * 
     * @return 终止指令，如果没有则返回 null
     */
    public IRInstruction getTerminator() {
        if (instructions.isEmpty()) {
            return null;
        }
        
        IRInstruction last = instructions.get(instructions.size() - 1);
        if (last.isTerminator()) {
            return last;
        }
        
        return null;
    }
    
    /**
     * 检查基本块是否有终止指令
     * 
     * @return 是否有终止指令
     */
    public boolean hasTerminator() {
        return getTerminator() != null;
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitBasicBlock(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  block ").append(name).append(" {\n");
        
        // 指令
        for (IRInstruction instruction : instructions) {
            sb.append("    ").append(instruction).append("\n");
        }
        
        sb.append("  }");
        return sb.toString();
    }
}