package org.tabooproject.fluxon.ast.statement;

import org.tabooproject.fluxon.ast.SourceLocation;
import org.tabooproject.fluxon.ast.expression.Expr;

import java.util.List;

/**
 * 异步函数声明语句
 * 表示异步函数的声明
 */
public class AsyncFunctionDeclStmt extends FunctionDeclStmt {

    /**
     * 创建异步函数声明语句
     * 
     * @param name 函数名
     * @param parameters 参数列表
     * @param returnType 返回类型
     * @param body 函数体
     * @param location 源代码位置
     */
    public AsyncFunctionDeclStmt(String name, List<Parameter> parameters, Expr body, String returnType, SourceLocation location) {
        super(name, parameters, body, returnType, true, location);
    }
}