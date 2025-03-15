package org.tabooproject.fluxon.ast.statement;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;
import org.tabooproject.fluxon.ast.expression.Expr;

import java.util.List;

/**
 * 函数声明语句节点
 * 表示函数声明
 */
public class FunctionDeclStmt extends Stmt {
    private final String name;
    private final List<Parameter> parameters;
    private final Expr body;
    private final String returnType; // 返回类型注解，可能为 null
    private final boolean isAsync; // 是否为异步函数
    
    /**
     * 创建函数声明语句节点
     * 
     * @param name 函数名
     * @param parameters 参数列表
     * @param body 函数体
     * @param returnType 返回类型注解
     * @param isAsync 是否为异步函数
     * @param location 源代码位置
     */
    public FunctionDeclStmt(String name, List<Parameter> parameters, Expr body, String returnType, boolean isAsync, SourceLocation location) {
        super(location);
        this.name = name;
        this.parameters = parameters;
        this.body = body;
        this.returnType = returnType;
        this.isAsync = isAsync;
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
     * 获取函数体
     * 
     * @return 函数体
     */
    public Expr getBody() {
        return body;
    }
    
    /**
     * 获取返回类型注解
     * 
     * @return 返回类型注解，可能为 null
     */
    public String getReturnType() {
        return returnType;
    }
    
    /**
     * 检查是否为异步函数
     * 
     * @return 是否为异步函数
     */
    public boolean isAsync() {
        return isAsync;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitFunctionDeclStmt(this);
    }
    
    /**
     * 函数参数
     */
    public static class Parameter {
        private final String name;
        private final String type; // 类型注解，可能为 null
        
        /**
         * 创建函数参数
         * 
         * @param name 参数名
         * @param type 类型注解
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
         * 获取类型注解
         * 
         * @return 类型注解，可能为 null
         */
        public String getType() {
            return type;
        }
    }
}