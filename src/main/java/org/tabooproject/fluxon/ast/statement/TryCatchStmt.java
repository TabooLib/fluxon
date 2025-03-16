package org.tabooproject.fluxon.ast.statement;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

/**
 * try-catch 语句节点
 * 表示 try-catch 语句
 */
public class TryCatchStmt extends Stmt {
    private final Stmt tryBlock;
    private final Stmt catchBlock;
    private final String exceptionVariable; // 异常变量名，可能为 null
    private final String exceptionType; // 异常类型，可能为 null
    
    /**
     * 创建 try-catch 语句节点
     * 
     * @param tryBlock try 块
     * @param catchBlock catch 块
     * @param exceptionVariable 异常变量名
     * @param exceptionType 异常类型
     * @param location 源代码位置
     */
    public TryCatchStmt(Stmt tryBlock, Stmt catchBlock, String exceptionVariable, String exceptionType, SourceLocation location) {
        super(location);
        this.tryBlock = tryBlock;
        this.catchBlock = catchBlock;
        this.exceptionVariable = exceptionVariable;
        this.exceptionType = exceptionType;
    }
    
    /**
     * 获取 try 块
     * 
     * @return try 块
     */
    public Stmt getTryBlock() {
        return tryBlock;
    }
    
    /**
     * 获取 catch 块
     * 
     * @return catch 块
     */
    public Stmt getCatchBlock() {
        return catchBlock;
    }
    
    /**
     * 获取异常变量名
     * 
     * @return 异常变量名，可能为 null
     */
    public String getExceptionVariable() {
        return exceptionVariable;
    }
    
    /**
     * 获取异常类型
     * 
     * @return 异常类型，可能为 null
     */
    public String getExceptionType() {
        return exceptionType;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitTryCatchStmt(this);
    }
}