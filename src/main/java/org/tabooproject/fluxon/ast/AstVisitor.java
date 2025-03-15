package org.tabooproject.fluxon.ast;

import org.tabooproject.fluxon.ast.expression.*;
import org.tabooproject.fluxon.ast.statement.*;

/**
 * AST 访问者接口
 * 用于遍历和处理 AST
 * 
 * @param <T> 访问结果类型
 */
public interface AstVisitor<T> {
    // 表达式节点
    
    /**
     * 访问二元表达式节点
     * 
     * @param node 二元表达式节点
     * @return 访问结果
     */
    T visitBinaryExpr(BinaryExpr node);
    
    /**
     * 访问一元表达式节点
     * 
     * @param node 一元表达式节点
     * @return 访问结果
     */
    T visitUnaryExpr(UnaryExpr node);
    
    /**
     * 访问字面量表达式节点
     * 
     * @param node 字面量表达式节点
     * @return 访问结果
     */
    T visitLiteralExpr(LiteralExpr node);
    
    /**
     * 访问变量引用表达式节点
     * 
     * @param node 变量引用表达式节点
     * @return 访问结果
     */
    T visitVariableExpr(VariableExpr node);
    
    /**
     * 访问函数调用表达式节点
     * 
     * @param node 函数调用表达式节点
     * @return 访问结果
     */
    T visitCallExpr(CallExpr node);
    
    /**
     * 访问条件表达式节点
     * 
     * @param node 条件表达式节点
     * @return 访问结果
     */
    T visitIfExpr(IfExpr node);
    
    /**
     * 访问 when 表达式节点
     * 
     * @param node when 表达式节点
     * @return 访问结果
     */
    T visitWhenExpr(WhenExpr node);
    
    /**
     * 访问列表表达式节点
     * 
     * @param node 列表表达式节点
     * @return 访问结果
     */
    T visitListExpr(ListExpr node);
    
    /**
     * 访问字典表达式节点
     * 
     * @param node 字典表达式节点
     * @return 访问结果
     */
    T visitMapExpr(MapExpr node);
    
    /**
     * 访问范围表达式节点
     * 
     * @param node 范围表达式节点
     * @return 访问结果
     */
    T visitRangeExpr(RangeExpr node);
    
    /**
     * 访问空安全访问表达式节点
     * 
     * @param node 空安全访问表达式节点
     * @return 访问结果
     */
    T visitSafeAccessExpr(SafeAccessExpr node);
    
    /**
     * 访问 Elvis 操作符表达式节点
     * 
     * @param node Elvis 操作符表达式节点
     * @return 访问结果
     */
    T visitElvisExpr(ElvisExpr node);
    
    /**
     * 访问 Lambda 表达式节点
     * 
     * @param node Lambda 表达式节点
     * @return 访问结果
     */
    T visitLambdaExpr(LambdaExpr node);
    
    /**
     * 访问块表达式节点
     * 
     * @param node 块表达式节点
     * @return 访问结果
     */
    T visitBlockExpr(BlockExpr node);
    
    // 语句节点
    
    /**
     * 访问块语句节点
     * 
     * @param node 块语句节点
     * @return 访问结果
     */
    T visitBlockStmt(BlockStmt node);
    
    /**
     * 访问表达式语句节点
     * 
     * @param node 表达式语句节点
     * @return 访问结果
     */
    T visitExpressionStmt(ExpressionStmt node);
    
    /**
     * 访问变量声明语句节点
     * 
     * @param node 变量声明语句节点
     * @return 访问结果
     */
    T visitVarDeclStmt(VarDeclStmt node);
    
    /**
     * 访问函数声明语句节点
     * 
     * @param node 函数声明语句节点
     * @return 访问结果
     */
    T visitFunctionDeclStmt(FunctionDeclStmt node);
    
    /**
     * 访问异步函数声明语句节点
     * 
     * @param node 异步函数声明语句节点
     * @return 访问结果
     */
    T visitAsyncFunctionDeclStmt(AsyncFunctionDeclStmt node);
    
    /**
     * 访问返回语句节点
     * 
     * @param node 返回语句节点
     * @return 访问结果
     */
    T visitReturnStmt(ReturnStmt node);
    
    /**
     * 访问 try-catch 语句节点
     * 
     * @param node try-catch 语句节点
     * @return 访问结果
     */
    T visitTryCatchStmt(TryCatchStmt node);
    
    /**
     * 访问 await 语句节点
     * 
     * @param node await 语句节点
     * @return 访问结果
     */
    T visitAwaitStmt(AwaitStmt node);
    
    /**
     * 访问程序节点
     * 
     * @param node 程序节点
     * @return 访问结果
     */
    T visitProgram(Program node);
}