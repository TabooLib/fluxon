package org.tabooproject.fluxon.ir.instruction;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRValue;
import org.tabooproject.fluxon.ir.IRVisitor;
import org.tabooproject.fluxon.ir.type.IRTypeFactory;

/**
 * 一元运算指令
 * 执行一元运算
 */
public class UnaryInst extends AbstractInstruction {
    /**
     * 一元运算符
     */
    public enum Operator {
        /**
         * 取负
         */
        NEG("neg"),
        
        /**
         * 按位取反
         */
        NOT("not");
        
        private final String symbol;
        
        Operator(String symbol) {
            this.symbol = symbol;
        }
        
        /**
         * 获取运算符符号
         * 
         * @return 运算符符号
         */
        public String getSymbol() {
            return symbol;
        }
    }
    
    private final Operator operator; // 运算符
    private final IRValue operand; // 操作数
    
    /**
     * 创建一元运算指令
     * 
     * @param operator 运算符
     * @param operand 操作数
     * @param name 指令名称
     */
    public UnaryInst(Operator operator, IRValue operand, String name) {
        super(getResultType(operator, operand), name);
        this.operator = operator;
        this.operand = operand;
    }
    
    /**
     * 获取运算符
     * 
     * @return 运算符
     */
    public Operator getOperator() {
        return operator;
    }
    
    /**
     * 获取操作数
     * 
     * @return 操作数
     */
    public IRValue getOperand() {
        return operand;
    }
    
    /**
     * 获取结果类型
     * 
     * @param operator 运算符
     * @param operand 操作数
     * @return 结果类型
     */
    private static IRType getResultType(Operator operator, IRValue operand) {
        if (operator == Operator.NOT && operand.getType().isBooleanType()) {
            return IRTypeFactory.getInstance().getBoolType();
        }
        
        return operand.getType();
    }
    
    @Override
    protected String getInstructionName() {
        return operator.getSymbol();
    }
    
    @Override
    protected String getOperandsString() {
        return operand.toString();
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitUnaryInst(this);
    }
}