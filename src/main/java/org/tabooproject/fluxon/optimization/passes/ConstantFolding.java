package org.tabooproject.fluxon.optimization.passes;

import org.tabooproject.fluxon.ir.IRBasicBlock;
import org.tabooproject.fluxon.ir.IRInstruction;
import org.tabooproject.fluxon.ir.IRValue;
import org.tabooproject.fluxon.ir.instruction.BinaryInst;
import org.tabooproject.fluxon.ir.instruction.UnaryInst;
import org.tabooproject.fluxon.ir.value.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 常量折叠优化 Pass
 * 在编译时计算常量表达式的值
 */
public class ConstantFolding extends AbstractOptimizationPass {
    
    /**
     * 创建常量折叠优化 Pass
     */
    public ConstantFolding() {
        super("ConstantFolding");
    }
    
    @Override
    protected void optimizeBasicBlock(IRBasicBlock block) {
        List<IRInstruction> instructions = block.getInstructions();
        List<IRInstruction> newInstructions = new ArrayList<>();
        
        for (IRInstruction instruction : instructions) {
            // 尝试折叠常量
            IRValue folded = foldConstant(instruction);
            
            if (folded != null && folded != instruction) {
                // 如果成功折叠，替换指令
                replaceInstruction(instruction, folded, newInstructions);
            } else {
                // 否则，保留原指令
                newInstructions.add(instruction);
            }
        }
        
        // 更新基本块的指令
        instructions.clear();
        instructions.addAll(newInstructions);
    }
    
    /**
     * 折叠常量
     * 
     * @param instruction 指令
     * @return 折叠后的常量值，如果无法折叠则返回 null
     */
    private IRValue foldConstant(IRInstruction instruction) {
        if (instruction instanceof BinaryInst) {
            return foldBinaryInst((BinaryInst) instruction);
        } else if (instruction instanceof UnaryInst) {
            return foldUnaryInst((UnaryInst) instruction);
        }
        
        return null;
    }
    
    /**
     * 折叠二元运算指令
     * 
     * @param instruction 二元运算指令
     * @return 折叠后的常量值，如果无法折叠则返回 null
     */
    private IRValue foldBinaryInst(BinaryInst instruction) {
        IRValue left = instruction.getLeft();
        IRValue right = instruction.getRight();
        
        // 检查操作数是否为常量
        if (!(left instanceof Constant) || !(right instanceof Constant)) {
            return null;
        }
        
        // 根据操作符和操作数类型进行折叠
        BinaryInst.Operator operator = instruction.getOperator();
        
        // 整数常量
        if (left instanceof ConstantInt && right instanceof ConstantInt) {
            return foldIntBinaryInst(operator, (ConstantInt) left, (ConstantInt) right);
        }
        
        // 浮点数常量
        if (left instanceof ConstantFloat && right instanceof ConstantFloat) {
            return foldFloatBinaryInst(operator, (ConstantFloat) left, (ConstantFloat) right);
        }
        
        // 布尔常量
        if (left instanceof ConstantBool && right instanceof ConstantBool) {
            return foldBoolBinaryInst(operator, (ConstantBool) left, (ConstantBool) right);
        }
        
        return null;
    }
    
    /**
     * 折叠整数二元运算
     * 
     * @param operator 运算符
     * @param left 左操作数
     * @param right 右操作数
     * @return 折叠后的常量值
     */
    private IRValue foldIntBinaryInst(BinaryInst.Operator operator, ConstantInt left, ConstantInt right) {
        long leftValue = left.getValue();
        long rightValue = right.getValue();
        
        switch (operator) {
            case ADD:
                return ConstantInt.getI32((int) (leftValue + rightValue));
            case SUB:
                return ConstantInt.getI32((int) (leftValue - rightValue));
            case MUL:
                return ConstantInt.getI32((int) (leftValue * rightValue));
            case DIV:
                if (rightValue == 0) {
                    // 除以零，不折叠
                    return null;
                }
                return ConstantInt.getI32((int) (leftValue / rightValue));
            case REM:
                if (rightValue == 0) {
                    // 除以零，不折叠
                    return null;
                }
                return ConstantInt.getI32((int) (leftValue % rightValue));
            case EQ:
                return ConstantBool.get(leftValue == rightValue);
            case NE:
                return ConstantBool.get(leftValue != rightValue);
            case LT:
                return ConstantBool.get(leftValue < rightValue);
            case LE:
                return ConstantBool.get(leftValue <= rightValue);
            case GT:
                return ConstantBool.get(leftValue > rightValue);
            case GE:
                return ConstantBool.get(leftValue >= rightValue);
            case AND:
                return ConstantInt.getI32((int) (leftValue & rightValue));
            case OR:
                return ConstantInt.getI32((int) (leftValue | rightValue));
            case XOR:
                return ConstantInt.getI32((int) (leftValue ^ rightValue));
            case SHL:
                return ConstantInt.getI32((int) (leftValue << rightValue));
            case SHR:
                return ConstantInt.getI32((int) (leftValue >> rightValue));
            default:
                return null;
        }
    }
    
    /**
     * 折叠浮点数二元运算
     * 
     * @param operator 运算符
     * @param left 左操作数
     * @param right 右操作数
     * @return 折叠后的常量值
     */
    private IRValue foldFloatBinaryInst(BinaryInst.Operator operator, ConstantFloat left, ConstantFloat right) {
        double leftValue = left.getDoubleValue();
        double rightValue = right.getDoubleValue();
        
        switch (operator) {
            case ADD:
                return ConstantFloat.getDouble(leftValue + rightValue);
            case SUB:
                return ConstantFloat.getDouble(leftValue - rightValue);
            case MUL:
                return ConstantFloat.getDouble(leftValue * rightValue);
            case DIV:
                if (rightValue == 0) {
                    // 除以零，不折叠
                    return null;
                }
                return ConstantFloat.getDouble(leftValue / rightValue);
            case EQ:
                return ConstantBool.get(leftValue == rightValue);
            case NE:
                return ConstantBool.get(leftValue != rightValue);
            case LT:
                return ConstantBool.get(leftValue < rightValue);
            case LE:
                return ConstantBool.get(leftValue <= rightValue);
            case GT:
                return ConstantBool.get(leftValue > rightValue);
            case GE:
                return ConstantBool.get(leftValue >= rightValue);
            default:
                return null;
        }
    }
    
    /**
     * 折叠布尔二元运算
     * 
     * @param operator 运算符
     * @param left 左操作数
     * @param right 右操作数
     * @return 折叠后的常量值
     */
    private IRValue foldBoolBinaryInst(BinaryInst.Operator operator, ConstantBool left, ConstantBool right) {
        boolean leftValue = left.getValue();
        boolean rightValue = right.getValue();
        
        switch (operator) {
            case EQ:
                return ConstantBool.get(leftValue == rightValue);
            case NE:
                return ConstantBool.get(leftValue != rightValue);
            case AND:
                return ConstantBool.get(leftValue && rightValue);
            case OR:
                return ConstantBool.get(leftValue || rightValue);
            case XOR:
                return ConstantBool.get(leftValue ^ rightValue);
            default:
                return null;
        }
    }
    
    /**
     * 折叠一元运算指令
     * 
     * @param instruction 一元运算指令
     * @return 折叠后的常量值，如果无法折叠则返回 null
     */
    private IRValue foldUnaryInst(UnaryInst instruction) {
        IRValue operand = instruction.getOperand();
        
        // 检查操作数是否为常量
        if (!(operand instanceof Constant)) {
            return null;
        }
        
        // 根据操作符和操作数类型进行折叠
        UnaryInst.Operator operator = instruction.getOperator();
        
        // 整数常量
        if (operand instanceof ConstantInt) {
            return foldIntUnaryInst(operator, (ConstantInt) operand);
        }
        
        // 浮点数常量
        if (operand instanceof ConstantFloat) {
            return foldFloatUnaryInst(operator, (ConstantFloat) operand);
        }
        
        // 布尔常量
        if (operand instanceof ConstantBool) {
            return foldBoolUnaryInst(operator, (ConstantBool) operand);
        }
        
        return null;
    }
    
    /**
     * 折叠整数一元运算
     * 
     * @param operator 运算符
     * @param operand 操作数
     * @return 折叠后的常量值
     */
    private IRValue foldIntUnaryInst(UnaryInst.Operator operator, ConstantInt operand) {
        long value = operand.getValue();
        
        switch (operator) {
            case NEG:
                return ConstantInt.getI32((int) -value);
            case NOT:
                return ConstantInt.getI32((int) ~value);
            default:
                return null;
        }
    }
    
    /**
     * 折叠浮点数一元运算
     * 
     * @param operator 运算符
     * @param operand 操作数
     * @return 折叠后的常量值
     */
    private IRValue foldFloatUnaryInst(UnaryInst.Operator operator, ConstantFloat operand) {
        double value = operand.getDoubleValue();
        
        switch (operator) {
            case NEG:
                return ConstantFloat.getDouble(-value);
            default:
                return null;
        }
    }
    
    /**
     * 折叠布尔一元运算
     * 
     * @param operator 运算符
     * @param operand 操作数
     * @return 折叠后的常量值
     */
    private IRValue foldBoolUnaryInst(UnaryInst.Operator operator, ConstantBool operand) {
        boolean value = operand.getValue();
        
        switch (operator) {
            case NOT:
                return ConstantBool.get(!value);
            default:
                return null;
        }
    }
    
    /**
     * 替换指令
     * 
     * @param oldInstruction 旧指令
     * @param newValue 新值
     * @param instructions 指令列表
     */
    private void replaceInstruction(IRInstruction oldInstruction, IRValue newValue, List<IRInstruction> instructions) {
        // 在后续指令中，将对旧指令的引用替换为新值
        // 这里简化处理，只是不添加旧指令
        // 实际实现中，需要更新所有引用
    }
}