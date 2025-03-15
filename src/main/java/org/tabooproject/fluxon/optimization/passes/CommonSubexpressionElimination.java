package org.tabooproject.fluxon.optimization.passes;

import org.tabooproject.fluxon.ir.IRBasicBlock;
import org.tabooproject.fluxon.ir.IRInstruction;
import org.tabooproject.fluxon.ir.IRValue;
import org.tabooproject.fluxon.ir.instruction.BinaryInst;
import org.tabooproject.fluxon.ir.instruction.LoadInst;
import org.tabooproject.fluxon.ir.instruction.StoreInst;
import org.tabooproject.fluxon.ir.instruction.UnaryInst;

import java.util.*;

/**
 * 公共子表达式消除优化 Pass
 * 识别并消除重复计算的表达式
 */
public class CommonSubexpressionElimination extends AbstractOptimizationPass {
    
    /**
     * 创建公共子表达式消除优化 Pass
     */
    public CommonSubexpressionElimination() {
        super("CommonSubexpressionElimination");
    }
    
    @Override
    protected void optimizeBasicBlock(IRBasicBlock block) {
        // 表达式到指令的映射
        Map<ExpressionKey, IRInstruction> expressionMap = new HashMap<>();
        
        // 指令替换映射
        Map<IRInstruction, IRInstruction> replacementMap = new HashMap<>();
        
        // 可能被修改的内存位置
        Set<IRValue> modifiedMemory = new HashSet<>();
        
        List<IRInstruction> instructions = block.getInstructions();
        List<IRInstruction> newInstructions = new ArrayList<>();
        
        for (IRInstruction instruction : instructions) {
            // 如果是存储指令，记录被修改的内存位置
            if (instruction instanceof StoreInst) {
                StoreInst store = (StoreInst) instruction;
                modifiedMemory.add(store.getPointer());
            }
            
            // 如果是加载指令，检查内存位置是否被修改
            if (instruction instanceof LoadInst) {
                LoadInst load = (LoadInst) instruction;
                IRValue pointer = load.getPointer();
                
                // 如果内存位置没有被修改，检查是否有相同的加载指令
                if (!modifiedMemory.contains(pointer)) {
                    ExpressionKey key = new LoadExpressionKey(pointer);
                    IRInstruction existing = expressionMap.get(key);
                    
                    if (existing != null) {
                        // 找到公共子表达式，替换当前指令
                        replacementMap.put(instruction, existing);
                        continue;
                    } else {
                        // 记录当前加载指令
                        expressionMap.put(key, instruction);
                    }
                }
            }
            
            // 如果是二元运算指令，检查是否有相同的运算
            if (instruction instanceof BinaryInst) {
                BinaryInst binary = (BinaryInst) instruction;
                
                // 获取操作数，考虑替换
                IRValue left = getReplacedValue(binary.getLeft(), replacementMap);
                IRValue right = getReplacedValue(binary.getRight(), replacementMap);
                
                ExpressionKey key = new BinaryExpressionKey(binary.getOperator(), left, right);
                IRInstruction existing = expressionMap.get(key);
                
                if (existing != null) {
                    // 找到公共子表达式，替换当前指令
                    replacementMap.put(instruction, existing);
                    continue;
                } else {
                    // 记录当前二元运算指令
                    expressionMap.put(key, instruction);
                }
            }
            
            // 如果是一元运算指令，检查是否有相同的运算
            if (instruction instanceof UnaryInst) {
                UnaryInst unary = (UnaryInst) instruction;
                
                // 获取操作数，考虑替换
                IRValue operand = getReplacedValue(unary.getOperand(), replacementMap);
                
                ExpressionKey key = new UnaryExpressionKey(unary.getOperator(), operand);
                IRInstruction existing = expressionMap.get(key);
                
                if (existing != null) {
                    // 找到公共子表达式，替换当前指令
                    replacementMap.put(instruction, existing);
                    continue;
                } else {
                    // 记录当前一元运算指令
                    expressionMap.put(key, instruction);
                }
            }
            
            // 添加当前指令
            newInstructions.add(instruction);
        }
        
        // 更新基本块的指令
        instructions.clear();
        instructions.addAll(newInstructions);
        
        // 更新指令的使用
        updateInstructionUses(block, replacementMap);
    }
    
    /**
     * 获取替换后的值
     * 
     * @param value 原始值
     * @param replacementMap 替换映射
     * @return 替换后的值
     */
    private IRValue getReplacedValue(IRValue value, Map<IRInstruction, IRInstruction> replacementMap) {
        if (value instanceof IRInstruction && replacementMap.containsKey(value)) {
            return replacementMap.get(value);
        }
        return value;
    }
    
    /**
     * 更新指令的使用
     * 
     * @param block 基本块
     * @param replacementMap 替换映射
     */
    private void updateInstructionUses(IRBasicBlock block, Map<IRInstruction, IRInstruction> replacementMap) {
        // 这里简化处理，实际实现中需要遍历所有指令，更新操作数
        // 例如，对于二元运算指令，需要更新左右操作数
        // 对于函数调用指令，需要更新参数
        // 对于加载指令，需要更新指针
        // 对于存储指令，需要更新值和指针
        // 等等
    }
    
    /**
     * 表达式键接口
     */
    private interface ExpressionKey {
    }
    
    /**
     * 加载表达式键
     */
    private static class LoadExpressionKey implements ExpressionKey {
        private final IRValue pointer;
        
        public LoadExpressionKey(IRValue pointer) {
            this.pointer = pointer;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LoadExpressionKey that = (LoadExpressionKey) o;
            return Objects.equals(pointer, that.pointer);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(pointer);
        }
    }
    
    /**
     * 二元表达式键
     */
    private static class BinaryExpressionKey implements ExpressionKey {
        private final BinaryInst.Operator operator;
        private final IRValue left;
        private final IRValue right;
        
        public BinaryExpressionKey(BinaryInst.Operator operator, IRValue left, IRValue right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BinaryExpressionKey that = (BinaryExpressionKey) o;
            return operator == that.operator &&
                    Objects.equals(left, that.left) &&
                    Objects.equals(right, that.right);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(operator, left, right);
        }
    }
    
    /**
     * 一元表达式键
     */
    private static class UnaryExpressionKey implements ExpressionKey {
        private final UnaryInst.Operator operator;
        private final IRValue operand;
        
        public UnaryExpressionKey(UnaryInst.Operator operator, IRValue operand) {
            this.operator = operator;
            this.operand = operand;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UnaryExpressionKey that = (UnaryExpressionKey) o;
            return operator == that.operator &&
                    Objects.equals(operand, that.operand);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(operator, operand);
        }
    }
}