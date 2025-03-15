package org.tabooproject.fluxon.optimization.passes;

import org.tabooproject.fluxon.ir.IRBasicBlock;
import org.tabooproject.fluxon.ir.IRFunction;
import org.tabooproject.fluxon.ir.IRInstruction;
import org.tabooproject.fluxon.ir.IRValue;
import org.tabooproject.fluxon.ir.instruction.BranchInst;
import org.tabooproject.fluxon.ir.instruction.CondBranchInst;
import org.tabooproject.fluxon.ir.value.ConstantBool;

import java.util.*;

/**
 * 死代码消除优化 Pass
 * 删除程序中永远不会执行的代码
 */
public class DeadCodeElimination extends AbstractOptimizationPass {
    
    /**
     * 创建死代码消除优化 Pass
     */
    public DeadCodeElimination() {
        super("DeadCodeElimination");
    }
    
    @Override
    protected void optimizeFunction(IRFunction function) {
        // 标记可达基本块
        Set<IRBasicBlock> reachableBlocks = markReachableBlocks(function);
        
        // 删除不可达基本块
        List<IRBasicBlock> blocks = function.getBlocks();
        blocks.removeIf(block -> !reachableBlocks.contains(block));
        
        // 优化条件分支
        for (IRBasicBlock block : blocks) {
            optimizeConditionalBranches(block);
        }
    }
    
    @Override
    protected void optimizeBasicBlock(IRBasicBlock block) {
        // 删除无用指令
        removeUnusedInstructions(block);
    }
    
    /**
     * 标记可达基本块
     * 
     * @param function IR 函数
     * @return 可达基本块集合
     */
    private Set<IRBasicBlock> markReachableBlocks(IRFunction function) {
        Set<IRBasicBlock> reachableBlocks = new HashSet<>();
        Queue<IRBasicBlock> workList = new LinkedList<>();
        
        // 从入口基本块开始
        IRBasicBlock entryBlock = function.getEntryBlock();
        if (entryBlock != null) {
            workList.add(entryBlock);
            reachableBlocks.add(entryBlock);
        }
        
        // 广度优先遍历
        while (!workList.isEmpty()) {
            IRBasicBlock block = workList.poll();
            
            // 添加后继基本块
            for (IRBasicBlock successor : block.getSuccessors()) {
                if (!reachableBlocks.contains(successor)) {
                    reachableBlocks.add(successor);
                    workList.add(successor);
                }
            }
        }
        
        return reachableBlocks;
    }
    
    /**
     * 优化条件分支
     * 
     * @param block IR 基本块
     */
    private void optimizeConditionalBranches(IRBasicBlock block) {
        IRInstruction terminator = block.getTerminator();
        
        if (terminator instanceof CondBranchInst) {
            CondBranchInst condBranch = (CondBranchInst) terminator;
            IRValue condition = condBranch.getCondition();
            
            // 如果条件是常量布尔值，替换为无条件分支
            if (condition instanceof ConstantBool) {
                boolean value = ((ConstantBool) condition).getValue();
                IRBasicBlock target = value ? condBranch.getThenBlock() : condBranch.getElseBlock();
                
                // 创建无条件分支
                BranchInst branch = new BranchInst(target);
                
                // 替换终止指令
                List<IRInstruction> instructions = block.getInstructions();
                instructions.set(instructions.size() - 1, branch);
                
                // 更新控制流图
                block.getSuccessors().clear();
                block.addSuccessor(target);
            }
        }
    }
    
    /**
     * 删除无用指令
     * 
     * @param block IR 基本块
     */
    private void removeUnusedInstructions(IRBasicBlock block) {
        List<IRInstruction> instructions = block.getInstructions();
        
        // 标记有用指令
        Set<IRInstruction> usefulInstructions = markUsefulInstructions(block);
        
        // 删除无用指令
        instructions.removeIf(instruction -> !usefulInstructions.contains(instruction));
    }
    
    /**
     * 标记有用指令
     * 
     * @param block IR 基本块
     * @return 有用指令集合
     */
    private Set<IRInstruction> markUsefulInstructions(IRBasicBlock block) {
        Set<IRInstruction> usefulInstructions = new HashSet<>();
        Set<IRValue> usefulValues = new HashSet<>();
        List<IRInstruction> instructions = block.getInstructions();
        
        // 从后向前遍历
        for (int i = instructions.size() - 1; i >= 0; i--) {
            IRInstruction instruction = instructions.get(i);
            
            // 终止指令和有副作用的指令总是有用的
            if (instruction.isTerminator() || instruction.hasSideEffects()) {
                usefulInstructions.add(instruction);
                
                // 标记操作数为有用值
                markOperandsAsUseful(instruction, usefulValues);
            }
            // 如果指令的结果被使用，则指令有用
            else if (usefulValues.contains(instruction)) {
                usefulInstructions.add(instruction);
                
                // 标记操作数为有用值
                markOperandsAsUseful(instruction, usefulValues);
            }
        }
        
        return usefulInstructions;
    }
    
    /**
     * 标记操作数为有用值
     * 
     * @param instruction 指令
     * @param usefulValues 有用值集合
     */
    private void markOperandsAsUseful(IRInstruction instruction, Set<IRValue> usefulValues) {
        // 这里简化处理，实际实现中需要根据指令类型获取操作数
        // 例如，对于二元运算指令，需要标记左右操作数为有用值
        // 对于函数调用指令，需要标记函数和参数为有用值
        // 对于加载指令，需要标记指针为有用值
        // 对于存储指令，需要标记值和指针为有用值
        // 等等
    }
}