package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

/**
 * 一元表达式节点
 * 表示一元运算，如负号、逻辑非等
 */
public class UnaryExpr extends Expr {
    private final Expr operand;
    private final Operator operator;
    
    /**
     * 创建一元表达式节点
     * 
     * @param operand 操作数
     * @param operator 运算符
     * @param location 源代码位置
     */
    public UnaryExpr(Expr operand, Operator operator, SourceLocation location) {
        super(location);
        this.operand = operand;
        this.operator = operator;
    }
    
    /**
     * 获取操作数
     * 
     * @return 操作数
     */
    public Expr getOperand() {
        return operand;
    }
    
    /**
     * 获取运算符
     * 
     * @return 运算符
     */
    public Operator getOperator() {
        return operator;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitUnaryExpr(this);
    }

    @Override
    public String toString() {
        return operator + "" + operand;
    }

    /**
     * 一元运算符
     */
    public enum Operator {
        /**
         * 负号
         */
        NEGATE("-"),
        
        /**
         * 逻辑非
         */
        NOT("!");
        
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
}