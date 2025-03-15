package org.tabooproject.fluxon.ir.instruction;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRValue;
import org.tabooproject.fluxon.ir.IRVisitor;
import org.tabooproject.fluxon.ir.type.IRTypeFactory;

/**
 * Await 指令
 * 等待异步操作完成
 */
public class AwaitInst extends AbstractInstruction {
    private final IRValue expression; // 等待的表达式
    
    /**
     * 创建 Await 指令
     * 
     * @param expression 等待的表达式
     * @param resultType 结果类型
     * @param name 指令名称
     */
    public AwaitInst(IRValue expression, IRType resultType, String name) {
        super(resultType, name);
        this.expression = expression;
    }
    
    /**
     * 获取等待的表达式
     * 
     * @return 等待的表达式
     */
    public IRValue getExpression() {
        return expression;
    }
    
    @Override
    public boolean hasSideEffects() {
        return true;
    }
    
    @Override
    protected String getInstructionName() {
        return "await";
    }
    
    @Override
    protected String getOperandsString() {
        return expression.toString();
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitAwaitInst(this);
    }
}