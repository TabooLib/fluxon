package org.tabooproject.fluxon.ir.instruction;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRValue;
import org.tabooproject.fluxon.ir.IRVisitor;
import org.tabooproject.fluxon.ir.type.IRTypeFactory;

/**
 * 二元运算指令
 * 执行二元运算
 */
public class BinaryInst extends AbstractInstruction {
    /**
     * 二元运算符
     */
    public enum Operator {
        /**
         * 加法
         */
        ADD("add"),
        
        /**
         * 减法
         */
        SUB("sub"),
        
        /**
         * 乘法
         */
        MUL("mul"),
        
        /**
         * 除法
         */
        DIV("div"),
        
        /**
         * 取模
         */
        REM("rem"),
        
        /**
         * 按位与
         */
        AND("and"),
        
        /**
         * 按位或
         */
        OR("or"),
        
        /**
         * 按位异或
         */
        XOR("xor"),
        
        /**
         * 左移
         */
        SHL("shl"),
        
        /**
         * 右移
         */
        SHR("shr"),
        
        /**
         * 等于
         */
        EQ("eq"),
        
        /**
         * 不等于
         */
        NE("ne"),
        
        /**
         * 小于
         */
        LT("lt"),
        
        /**
         * 小于等于
         */
        LE("le"),
        
        /**
         * 大于
         */
        GT("gt"),
        
        /**
         * 大于等于
         */
        GE("ge");
        
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
    private final IRValue left; // 左操作数
    private final IRValue right; // 右操作数
    
    /**
     * 创建二元运算指令
     * 
     * @param operator 运算符
     * @param left 左操作数
     * @param right 右操作数
     * @param name 指令名称
     */
    public BinaryInst(Operator operator, IRValue left, IRValue right, String name) {
        super(getResultType(operator, left, right), name);
        this.operator = operator;
        this.left = left;
        this.right = right;
        
        // 检查类型兼容性
        if (!left.getType().equals(right.getType())) {
            throw new IllegalArgumentException("Type mismatch: " + left.getType() + " and " + right.getType());
        }
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
     * 获取左操作数
     * 
     * @return 左操作数
     */
    public IRValue getLeft() {
        return left;
    }
    
    /**
     * 获取右操作数
     * 
     * @return 右操作数
     */
    public IRValue getRight() {
        return right;
    }
    
    /**
     * 获取结果类型
     * 
     * @param operator 运算符
     * @param left 左操作数
     * @param right 右操作数
     * @return 结果类型
     */
    private static IRType getResultType(Operator operator, IRValue left, IRValue right) {
        // 比较运算符返回布尔类型
        if (operator == Operator.EQ || operator == Operator.NE ||
                operator == Operator.LT || operator == Operator.LE ||
                operator == Operator.GT || operator == Operator.GE) {
            return IRTypeFactory.getInstance().getBoolType();
        }
        
        // 其他运算符返回操作数类型
        return left.getType();
    }
    
    @Override
    protected String getInstructionName() {
        return operator.getSymbol();
    }
    
    @Override
    protected String getOperandsString() {
        return left + ", " + right;
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitBinaryInst(this);
    }
}