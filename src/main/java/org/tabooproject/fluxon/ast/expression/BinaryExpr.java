package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

/**
 * 二元表达式节点
 * 表示二元运算，如加减乘除等
 */
public class BinaryExpr extends Expr {
    private final Expr left;
    private final Expr right;
    private final Operator operator;
    
    /**
     * 创建二元表达式节点
     * 
     * @param left 左操作数
     * @param right 右操作数
     * @param operator 运算符
     * @param location 源代码位置
     */
    public BinaryExpr(Expr left, Expr right, Operator operator, SourceLocation location) {
        super(location);
        this.left = left;
        this.right = right;
        this.operator = operator;
    }
    
    /**
     * 获取左操作数
     * 
     * @return 左操作数
     */
    public Expr getLeft() {
        return left;
    }
    
    /**
     * 获取右操作数
     * 
     * @return 右操作数
     */
    public Expr getRight() {
        return right;
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
        return visitor.visitBinaryExpr(this);
    }
    
    /**
     * 二元运算符
     */
    public enum Operator {
        /**
         * 加法
         */
        ADD("+"),
        
        /**
         * 减法
         */
        SUBTRACT("-"),
        
        /**
         * 乘法
         */
        MULTIPLY("*"),
        
        /**
         * 除法
         */
        DIVIDE("/"),
        
        /**
         * 取模
         */
        MODULO("%"),
        
        /**
         * 等于
         */
        EQUAL("=="),
        
        /**
         * 不等于
         */
        NOT_EQUAL("!="),
        
        /**
         * 大于
         */
        GREATER(">"),
        
        /**
         * 小于
         */
        LESS("<"),
        
        /**
         * 大于等于
         */
        GREATER_EQUAL(">="),
        
        /**
         * 小于等于
         */
        LESS_EQUAL("<="),
        
        /**
         * 逻辑与
         */
        AND("&&"),
        
        /**
         * 逻辑或
         */
        OR("||");
        
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