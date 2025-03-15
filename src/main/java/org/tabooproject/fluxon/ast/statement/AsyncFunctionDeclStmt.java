package org.tabooproject.fluxon.ast.statement;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;
import org.tabooproject.fluxon.ast.expression.Expr;

import java.util.List;

/**
 * 异步函数声明语句
 * 表示异步函数的声明
 */
public class AsyncFunctionDeclStmt extends Stmt {
    private final String name;
    private final List<Parameter> parameters;
    private final String returnType;
    private final Expr body;
    
    /**
     * 创建异步函数声明语句
     * 
     * @param name 函数名
     * @param parameters 参数列表
     * @param returnType 返回类型
     * @param body 函数体
     * @param location 源代码位置
     */
    public AsyncFunctionDeclStmt(String name, List<Parameter> parameters, String returnType, Expr body, SourceLocation location) {
        super(location);
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
    }
    
    /**
     * 获取函数名
     * 
     * @return 函数名
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取参数列表
     * 
     * @return 参数列表
     */
    public List<Parameter> getParameters() {
        return parameters;
    }
    
    /**
     * 获取返回类型
     * 
     * @return 返回类型
     */
    public String getReturnType() {
        return returnType;
    }
    
    /**
     * 获取函数体
     * 
     * @return 函数体
     */
    public Expr getBody() {
        return body;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitAsyncFunctionDeclStmt(this);
    }
    
    /**
     * 函数参数
     */
    public static class Parameter {
        private final String name;
        private final String type;
        
        /**
         * 创建函数参数
         * 
         * @param name 参数名
         * @param type 参数类型
         */
        public Parameter(String name, String type) {
            this.name = name;
            this.type = type;
        }
        
        /**
         * 获取参数名
         * 
         * @return 参数名
         */
        public String getName() {
            return name;
        }
        
        /**
         * 获取参数类型
         * 
         * @return 参数类型
         */
        public String getType() {
            return type;
        }
    }
}